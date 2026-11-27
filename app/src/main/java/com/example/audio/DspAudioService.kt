package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.EqPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DspAudioService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: EqPreferencesRepository
    private lateinit var dspEngine: AudioDspEngine
    private lateinit var audioManager: AudioManager
    private val sessionReceiver = AudioSessionBroadcastReceiver()
    private var receiverRegistered = false
    private var playbackCallback: AudioManager.AudioPlaybackCallback? = null

    private var lastConfig: DspConfigState = DspConfigState()

    override fun onCreate() {
        super.onCreate()
        repository = EqPreferencesRepository(applicationContext)
        dspEngine = AudioDspEngine(applicationContext)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        createNotificationChannel()
        startAsForeground(buildNotification(lastConfig, AudioDspSessionHolder.runtimeStatus.value))

        registerDynamicSessionReceiver()
        registerPlaybackMonitor()

        serviceScope.launch {
            combine(
                repository.configFlow,
                AudioDspSessionHolder.externalSessions,
                AudioDspSessionHolder.isMediaPlayingOnDevice
            ) { config, sessions, isMediaPlaying ->
                Triple(config, sessions, isMediaPlaying)
            }.collect { (config, sessions, isMediaPlaying) ->
                lastConfig = config
                val status = dspEngine.apply(config, sessions, isMediaPlaying)
                AudioDspSessionHolder.updateRuntimeStatus(status)
                updateNotification(config, status)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_DSP -> {
                serviceScope.launch {
                    repository.setDspEnabled(!lastConfig.isDspEnabled)
                }
            }
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(sessionReceiver) }
            receiverRegistered = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playbackCallback?.let { runCatching { audioManager.unregisterAudioPlaybackCallback(it) } }
        }
        dspEngine.releaseAll()
        AudioDspSessionHolder.updateRuntimeStatus(
            DspRuntimeStatus(
                serviceRunning = false,
                engineTier = DspEngineTier.UNAVAILABLE,
                activeAppSummary = "DSP Engine Stopped"
            )
        )
        serviceScope.cancel()
    }

    private fun registerDynamicSessionReceiver() {
        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        ContextCompat.registerReceiver(
            this,
            sessionReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        receiverRegistered = true
    }

    private fun registerPlaybackMonitor() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val checkConfigs: (List<AudioPlaybackConfiguration>) -> Unit = { configs ->
            val mediaActive = configs.any { cfg ->
                val usage = cfg.audioAttributes.usage
                usage == AudioAttributes.USAGE_MEDIA || usage == AudioAttributes.USAGE_GAME
            }
            AudioDspSessionHolder.setMediaPlayingOnDevice(mediaActive)
        }

        checkConfigs(audioManager.activePlaybackConfigurations)

        val callback = object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                checkConfigs(configs)
            }
        }
        playbackCallback = callback
        audioManager.registerAudioPlaybackCallback(callback, null)
    }

    private fun startAsForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(config: DspConfigState, status: DspRuntimeStatus) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(config, status))
    }

    private fun buildNotification(config: DspConfigState, status: DspRuntimeStatus): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DspAudioService::class.java).apply { action = ACTION_TOGGLE_DSP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, DspAudioService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (config.isDspEnabled) {
            getString(R.string.notification_title_active)
        } else {
            getString(R.string.notification_title_bypass)
        }
        val presetName = BuiltInPresets.all.find { it.id == config.activePresetId }?.name
            ?: if (config.activePresetId == "custom") "Custom Curve" else "Custom Preset"

        val contentText = "$presetName • ${status.activeAppSummary}"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.action_toggle_dsp), toggleIntent)
            .addAction(0, getString(R.string.action_stop_service), stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "eq_pro_dsp_engine"
        private const val NOTIFICATION_ID = 4401
        const val ACTION_START = "com.example.audio.ACTION_START_DSP"
        const val ACTION_TOGGLE_DSP = "com.example.audio.ACTION_TOGGLE_DSP"
        const val ACTION_STOP_SERVICE = "com.example.audio.ACTION_STOP_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, DspAudioService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DspAudioService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
