package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AudioDspSessionHolder {
    private val _externalSessions = MutableStateFlow<List<AudioSessionInfo>>(emptyList())
    val externalSessions: StateFlow<List<AudioSessionInfo>> = _externalSessions.asStateFlow()

    private val _isMediaPlayingOnDevice = MutableStateFlow(false)
    val isMediaPlayingOnDevice: StateFlow<Boolean> = _isMediaPlayingOnDevice.asStateFlow()

    private val _runtimeStatus = MutableStateFlow(DspRuntimeStatus())
    val runtimeStatus: StateFlow<DspRuntimeStatus> = _runtimeStatus.asStateFlow()

    private val _fftSpectrum = MutableStateFlow(List(20) { 0f })
    val fftSpectrum: StateFlow<List<Float>> = _fftSpectrum.asStateFlow()

    fun onSessionOpened(context: Context, sessionId: Int, packageName: String?) {
        if (sessionId <= 0) return
        val pkg = packageName?.takeIf { it.isNotBlank() } ?: "unknown.player"
        val isYt = pkg.contains("youtube", ignoreCase = true) ||
            pkg.contains("gotube", ignoreCase = true)
        val label = resolveAppLabel(context, pkg)
        val entry = AudioSessionInfo(
            sessionId = sessionId,
            packageName = pkg,
            appLabel = label,
            isYouTube = isYt,
            openedAtMs = System.currentTimeMillis()
        )
        _externalSessions.update { existing ->
            (existing.filterNot { it.sessionId == sessionId } + entry)
                .sortedByDescending { it.openedAtMs }
        }
    }

    fun handleControlPanelIntent(context: Context, intent: Intent?) {
        if (intent?.action != AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL) return
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)
        if (sessionId > 0) {
            onSessionOpened(context, sessionId, packageName)
        }
    }

    fun onSessionClosed(sessionId: Int) {
        if (sessionId <= 0) return
        _externalSessions.update { existing ->
            existing.filterNot { it.sessionId == sessionId }
        }
    }

    fun setMediaPlayingOnDevice(playing: Boolean) {
        _isMediaPlayingOnDevice.value = playing
    }

    fun updateRuntimeStatus(status: DspRuntimeStatus) {
        _runtimeStatus.value = status
    }

    fun updateFftSpectrum(magnitudes: List<Float>) {
        _fftSpectrum.value = magnitudes
    }

    fun resolveAppLabel(context: Context, packageName: String): String {
        return when {
            packageName == "com.google.android.youtube" -> "YouTube"
            packageName == "com.google.android.apps.youtube.music" -> "YouTube Music"
            packageName == "com.spotify.music" -> "Spotify"
            packageName == "com.spotify.lite" -> "Spotify Lite"
            packageName.contains("gotube", ignoreCase = true) -> "GoTube"
            packageName == "com.android.chrome" -> "Chrome Audio"
            packageName == "app.revanced.android.youtube" -> "YouTube ReVanced"
            else -> runCatching {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            }.getOrElse {
                packageName.substringAfterLast('.').replaceFirstChar { c -> c.uppercase() }
            }
        }
    }
}
