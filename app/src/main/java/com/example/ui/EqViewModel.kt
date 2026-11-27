package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AmpClassMode
import com.example.audio.AudioDspSessionHolder
import com.example.audio.AudioOutputMode
import com.example.audio.BuiltInPresets
import com.example.audio.CrossoverSlope
import com.example.audio.DspAudioService
import com.example.audio.DspConfigState
import com.example.audio.DspRuntimeStatus
import com.example.audio.EqChannelEditTarget
import com.example.audio.EqPreset
import com.example.audio.ReferenceTonePlayer
import com.example.audio.RoutingMode
import com.example.audio.SpeakerProfile
import com.example.audio.TestToneType
import com.example.data.EqPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EqUiState(
    val config: DspConfigState = DspConfigState(),
    val customPresets: List<EqPreset> = emptyList(),
    val runtime: DspRuntimeStatus = DspRuntimeStatus(),
    val fftSpectrum: List<Float> = List(20) { 0f },
    val activeTestTone: TestToneType? = null,
    val selectedEqChannel: EqChannelEditTarget = EqChannelEditTarget.LINKED
) {
    val allPresets: List<EqPreset>
        get() = customPresets + BuiltInPresets.all

    val activeDisplayedBandGains: List<Float>
        get() = when {
            config.linkEqChannels || selectedEqChannel == EqChannelEditTarget.LINKED -> config.leftBandGainsDb
            selectedEqChannel == EqChannelEditTarget.RIGHT -> config.rightBandGainsDb
            else -> config.leftBandGainsDb
        }
}

class EqViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EqPreferencesRepository(application.applicationContext)
    private val tonePlayer = ReferenceTonePlayer()
    private val _activeTestTone = MutableStateFlow<TestToneType?>(null)
    private val _selectedEqChannel = MutableStateFlow(EqChannelEditTarget.LINKED)

    val uiState: StateFlow<EqUiState> = combine(
        repository.configFlow,
        repository.customPresetsFlow,
        AudioDspSessionHolder.runtimeStatus,
        AudioDspSessionHolder.fftSpectrum,
        combine(_activeTestTone, _selectedEqChannel) { tone, ch -> tone to ch }
    ) { config, customPresets, runtime, fft, (testTone, channel) ->
        val resolvedChannel = if (config.linkEqChannels) {
            EqChannelEditTarget.LINKED
        } else if (channel == EqChannelEditTarget.LINKED) {
            EqChannelEditTarget.LEFT
        } else {
            channel
        }
        EqUiState(
            config = config,
            customPresets = customPresets,
            runtime = runtime,
            fftSpectrum = fft,
            activeTestTone = testTone,
            selectedEqChannel = resolvedChannel
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EqUiState()
    )

    init {
        ensureServiceRunning()
    }

    fun ensureServiceRunning() {
        runCatching {
            DspAudioService.start(getApplication())
        }
    }

    fun toggleDsp() {
        val next = !uiState.value.config.isDspEnabled
        viewModelScope.launch {
            ensureServiceRunning()
            repository.setDspEnabled(next)
        }
    }

    fun setLinkEqChannels(linked: Boolean) {
        _selectedEqChannel.value = if (linked) EqChannelEditTarget.LINKED else EqChannelEditTarget.LEFT
        viewModelScope.launch {
            repository.setLinkEqChannels(linked)
        }
    }

    fun selectEqChannelTab(target: EqChannelEditTarget) {
        if (target == EqChannelEditTarget.LINKED) {
            setLinkEqChannels(true)
        } else {
            _selectedEqChannel.value = target
            if (uiState.value.config.linkEqChannels) {
                viewModelScope.launch {
                    repository.setLinkEqChannels(false)
                }
            }
        }
    }

    fun updateBandGain(bandIndex: Int, gainDb: Float) {
        val target = when {
            uiState.value.config.linkEqChannels -> "LINKED"
            uiState.value.selectedEqChannel == EqChannelEditTarget.RIGHT -> "RIGHT"
            else -> "LEFT"
        }
        viewModelScope.launch {
            repository.updateChannelBandGain(target, bandIndex, gainDb)
        }
    }

    fun applyPreset(preset: EqPreset) {
        viewModelScope.launch {
            ensureServiceRunning()
            repository.applyPreset(preset)
        }
    }

    fun resetFlat() {
        viewModelScope.launch {
            repository.resetFlat()
        }
    }

    fun setPreAmp(db: Float) {
        viewModelScope.launch { repository.setPreAmp(db) }
    }

    // Stereo / Mono & L/R Volume
    fun setOutputMode(mode: AudioOutputMode) {
        viewModelScope.launch { repository.setOutputMode(mode) }
    }

    fun setLinkLrVolume(linked: Boolean) {
        viewModelScope.launch { repository.setLinkLrVolume(linked) }
    }

    fun setLeftVolumeDb(db: Float) {
        viewModelScope.launch { repository.setLeftVolumeDb(db) }
    }

    fun setRightVolumeDb(db: Float) {
        viewModelScope.launch { repository.setRightVolumeDb(db) }
    }

    fun toggleLeftMuted() {
        viewModelScope.launch { repository.setLeftMuted(!uiState.value.config.leftMuted) }
    }

    fun toggleRightMuted() {
        viewModelScope.launch { repository.setRightMuted(!uiState.value.config.rightMuted) }
    }

    // Audio Mixer
    fun setMixerMasterDb(db: Float) {
        viewModelScope.launch { repository.setMixerMasterDb(db) }
    }

    fun setMixerMediaBusDb(db: Float) {
        viewModelScope.launch { repository.setMixerMediaBusDb(db) }
    }

    fun setMixerLowBusDb(db: Float) {
        viewModelScope.launch { repository.setMixerLowBusDb(db) }
    }

    fun setMixerMidBusDb(db: Float) {
        viewModelScope.launch { repository.setMixerMidBusDb(db) }
    }

    fun setMixerHighBusDb(db: Float) {
        viewModelScope.launch { repository.setMixerHighBusDb(db) }
    }

    fun toggleMixerMute(bus: String) {
        viewModelScope.launch { repository.toggleMixerMute(bus) }
    }

    fun resetMixer() {
        viewModelScope.launch { repository.resetMixer() }
    }

    // FX & Tone
    fun setBassBoost(amount: Float) {
        viewModelScope.launch { repository.setBassBoost(amount) }
    }

    fun setVirtualizer(amount: Float) {
        viewModelScope.launch { repository.setVirtualizer(amount) }
    }

    fun setLoudness(amount: Float) {
        viewModelScope.launch { repository.setLoudness(amount) }
    }

    fun setMbcPunch(amount: Float) {
        viewModelScope.launch { repository.setMbcPunch(amount) }
    }

    // Crossover / Speaker Management DSP
    fun setCrossoverEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setCrossoverEnabled(enabled) }
    }

    fun applySpeakerProfile(profile: SpeakerProfile) {
        viewModelScope.launch { repository.applySpeakerProfile(profile) }
    }

    fun setMainHpfHz(hz: Float) {
        viewModelScope.launch { repository.setMainHpfHz(hz) }
    }

    fun setSubLpfHz(hz: Float) {
        viewModelScope.launch { repository.setSubLpfHz(hz) }
    }

    fun setMidHighSplitHz(hz: Float) {
        viewModelScope.launch { repository.setMidHighSplitHz(hz) }
    }

    fun setCrossoverSlope(slope: CrossoverSlope) {
        viewModelScope.launch { repository.setCrossoverSlope(slope) }
    }

    fun setSpeakerDelayMs(ms: Float) {
        viewModelScope.launch { repository.setSpeakerDelayMs(ms) }
    }

    // Power Amplifier
    fun setPowerAmpEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setPowerAmpEnabled(enabled) }
    }

    fun setAmpClassMode(mode: AmpClassMode) {
        viewModelScope.launch { repository.setAmpClassMode(mode) }
    }

    fun setAmpDrive(drive: Float) {
        viewModelScope.launch { repository.setAmpDrive(drive) }
    }

    fun setLimiterEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setLimiterEnabled(enabled) }
    }

    fun setLimiterThreshold(db: Float) {
        viewModelScope.launch { repository.setLimiterThreshold(db) }
    }

    // Subwoofer
    fun setSubwooferEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSubwooferEnabled(enabled) }
    }

    fun setSubGainDb(db: Float) {
        viewModelScope.launch { repository.setSubGainDb(db) }
    }

    fun setSubPunch(punch: Float) {
        viewModelScope.launch { repository.setSubPunch(punch) }
    }

    fun setSubPhaseInverted(inverted: Boolean) {
        viewModelScope.launch { repository.setSubPhaseInverted(inverted) }
    }

    // Routing & Presets
    fun setRoutingMode(mode: RoutingMode) {
        viewModelScope.launch { repository.setRoutingMode(mode) }
    }

    fun saveCustomPreset(name: String) {
        viewModelScope.launch {
            repository.saveCustomPreset(name, uiState.value.config)
        }
    }

    fun deleteCustomPreset(presetId: String) {
        viewModelScope.launch {
            repository.deleteCustomPreset(presetId)
        }
    }

    fun toggleTestTone(tone: TestToneType) {
        val current = _activeTestTone.value
        if (current == tone) {
            tonePlayer.stop()
            _activeTestTone.value = null
        } else {
            ensureServiceRunning()
            tonePlayer.start(viewModelScope, tone)
            _activeTestTone.value = tone
        }
    }

    fun rebindDspEngine() {
        ensureServiceRunning()
        viewModelScope.launch {
            val current = uiState.value.config.isDspEnabled
            repository.setDspEnabled(current)
        }
    }

    fun openAccessibilityAudioSettings() {
        val app = getApplication<Application>()
        runCatching {
            val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
        }
    }

    fun launchExternalPlayer(packageName: String, fallbackUrl: String) {
        val app = getApplication<Application>()
        ensureServiceRunning()
        val launchIntent = app.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(launchIntent)
        } else {
            runCatching {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(webIntent)
            }.onFailure {
                Toast.makeText(app, "App not installed on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCleared() {
        tonePlayer.stop()
        super.onCleared()
    }
}
