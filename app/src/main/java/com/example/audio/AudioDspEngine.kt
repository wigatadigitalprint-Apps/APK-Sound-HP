package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max

class AudioDspEngine(private val context: Context) {

    private class SessionEffects(
        val sessionId: Int,
        var dynamicsProcessing: DynamicsProcessing? = null,
        var legacyEqualizer: Equalizer? = null,
        var bassBoost: BassBoost? = null,
        var virtualizer: Virtualizer? = null,
        var loudnessEnhancer: LoudnessEnhancer? = null
    ) {
        fun release() {
            runCatching { dynamicsProcessing?.release() }
            runCatching { legacyEqualizer?.release() }
            runCatching { bassBoost?.release() }
            runCatching { virtualizer?.release() }
            runCatching { loudnessEnhancer?.release() }
            dynamicsProcessing = null
            legacyEqualizer = null
            bassBoost = null
            virtualizer = null
            loudnessEnhancer = null
        }
    }

    private val activeEffects = mutableMapOf<Int, SessionEffects>()
    private var visualizer: Visualizer? = null
    private var visualizerSessionId: Int = -1

    @Synchronized
    fun apply(
        config: DspConfigState,
        externalSessions: List<AudioSessionInfo>,
        isMediaPlayingOnDevice: Boolean
    ): DspRuntimeStatus {
        val targetSessions = buildSet {
            when (config.routingMode) {
                RoutingMode.AUTO -> {
                    add(0)
                    externalSessions.forEach { add(it.sessionId) }
                }
                RoutingMode.GLOBAL_MIX -> {
                    add(0)
                }
                RoutingMode.SESSION_ONLY -> {
                    externalSessions.forEach { add(it.sessionId) }
                }
            }
        }

        val toRemove = activeEffects.keys.filter { it !in targetSessions }
        toRemove.forEach { sessionId ->
            activeEffects.remove(sessionId)?.release()
        }

        var highestTier = DspEngineTier.UNAVAILABLE
        var lastError: String? = null

        targetSessions.forEach { sessionId ->
            val bundle = activeEffects.getOrPut(sessionId) { createSessionEffects(sessionId) }
            val tier = applyConfigToSession(bundle, config)
            if (tier == DspEngineTier.DYNAMICS_PROCESSING) {
                highestTier = DspEngineTier.DYNAMICS_PROCESSING
            } else if (tier == DspEngineTier.LEGACY_EQUALIZER && highestTier == DspEngineTier.UNAVAILABLE) {
                highestTier = DspEngineTier.LEGACY_EQUALIZER
            }
            if (tier == DspEngineTier.UNAVAILABLE && lastError == null) {
                lastError = "AudioEffect initialization restricted on session #$sessionId"
            }
        }

        val liveFftActive = syncVisualizer(
            preferredSessionId = externalSessions.firstOrNull()?.sessionId ?: 0,
            dspEnabled = config.isDspEnabled
        )

        val activeSession = externalSessions.firstOrNull()
        val modeTag = config.outputMode.label
        val summary = when {
            !config.isDspEnabled -> "DSP Bypassed • Pass-through audio"
            activeSession != null -> "$modeTag • Locked: ${activeSession.appLabel} (#${activeSession.sessionId})"
            isMediaPlayingOnDevice && 0 in targetSessions -> "$modeTag • Global Mix (Media Playing)"
            0 in targetSessions -> "$modeTag • Ready for YouTube, GoTube & Spotify"
            else -> "Waiting for player audio session broadcast"
        }

        return DspRuntimeStatus(
            serviceRunning = true,
            engineTier = highestTier,
            attachedSessions = externalSessions,
            isGlobalMixAttached = 0 in activeEffects && highestTier != DspEngineTier.UNAVAILABLE,
            isMediaPlayingOnDevice = isMediaPlayingOnDevice,
            liveFftActive = liveFftActive,
            activeAppSummary = summary,
            lastError = lastError
        )
    }

    private fun createSessionEffects(sessionId: Int): SessionEffects {
        val effects = SessionEffects(sessionId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val dpConfig = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2,
                    true,
                    10,
                    true,
                    3,
                    true,
                    10,
                    true
                ).build()
                effects.dynamicsProcessing = DynamicsProcessing(0, sessionId, dpConfig)
            }.onFailure { err ->
                Log.w(TAG, "DynamicsProcessing failed on session $sessionId: ${err.message}")
            }
        }

        if (effects.dynamicsProcessing == null) {
            runCatching {
                effects.legacyEqualizer = Equalizer(0, sessionId)
            }.onFailure { err ->
                Log.w(TAG, "Legacy Equalizer failed on session $sessionId: ${err.message}")
            }
        }

        runCatching {
            effects.bassBoost = BassBoost(0, sessionId)
        }.onFailure { err ->
            Log.w(TAG, "BassBoost unavailable on session $sessionId: ${err.message}")
        }

        runCatching {
            effects.virtualizer = Virtualizer(0, sessionId)
        }.onFailure { err ->
            Log.w(TAG, "Virtualizer unavailable on session $sessionId: ${err.message}")
        }

        runCatching {
            effects.loudnessEnhancer = LoudnessEnhancer(sessionId)
        }.onFailure { err ->
            Log.w(TAG, "LoudnessEnhancer unavailable on session $sessionId: ${err.message}")
        }

        return effects
    }

    private fun applyConfigToSession(effects: SessionEffects, config: DspConfigState): DspEngineTier {
        var tier = DspEngineTier.UNAVAILABLE
        val enabled = config.isDspEnabled

        val ampGainDb = if (config.powerAmpEnabled) config.ampDrive * 7.5f else 0f
        val mediaTrimDb = if (config.mixerMediaMuted) -48f else config.mixerMediaBusDb
        val baseMasterDb = config.preAmpDb + config.mixerMasterDb + mediaTrimDb + (ampGainDb * 0.5f)

        val leftInputGain = if (config.leftMuted) -72f else (baseMasterDb + config.leftVolumeDb).coerceIn(-72f, 24f)
        val rightInputGain = if (config.rightMuted) -72f else (baseMasterDb + config.rightVolumeDb).coerceIn(-72f, 24f)

        val effectiveLeftBands = computeEffectivePreEqBands(config, isLeft = true)
        val effectiveRightBands = computeEffectivePreEqBands(config, isLeft = false)
        val postEqBands = computePostEqStage(config)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            effects.dynamicsProcessing?.let { dp ->
                runCatching {
                    dp.enabled = enabled
                    if (enabled) {
                        dp.setInputGainbyChannel(0, leftInputGain)
                        dp.setInputGainbyChannel(1, rightInputGain)

                        for (band in 0 until 10) {
                            dp.setPreEqBandByChannelIndex(
                                0,
                                band,
                                DynamicsProcessing.EqBand(true, EQ_BAND_CUTOFF_HZ[band], effectiveLeftBands[band])
                            )
                            dp.setPreEqBandByChannelIndex(
                                1,
                                band,
                                DynamicsProcessing.EqBand(true, EQ_BAND_CUTOFF_HZ[band], effectiveRightBands[band])
                            )
                            dp.setPostEqBandByChannelIndex(
                                0,
                                band,
                                DynamicsProcessing.EqBand(true, EQ_BAND_CUTOFF_HZ[band], postEqBands[band])
                            )
                            dp.setPostEqBandByChannelIndex(
                                1,
                                band,
                                DynamicsProcessing.EqBand(true, EQ_BAND_CUTOFF_HZ[band], postEqBands[band])
                            )
                        }

                        configureMbc(dp, config)

                        val limiter = DynamicsProcessing.Limiter(
                            true,
                            config.limiterEnabled,
                            0,
                            1.0f,
                            60.0f,
                            10.0f,
                            config.limiterThresholdDb,
                            0f
                        )
                        dp.setLimiterAllChannelsTo(limiter)
                    }
                    tier = DspEngineTier.DYNAMICS_PROCESSING
                }.onFailure { err ->
                    Log.w(TAG, "Failed updating DynamicsProcessing: ${err.message}")
                }
            }
        }

        if (tier == DspEngineTier.UNAVAILABLE) {
            effects.legacyEqualizer?.let { eq ->
                runCatching {
                    eq.enabled = enabled
                    if (enabled) {
                        val combinedBands = List(10) { i ->
                            ((effectiveLeftBands[i] + effectiveRightBands[i]) * 0.5f) + postEqBands[i]
                        }
                        val bandCount = eq.numberOfBands.toInt()
                        val levelRange = eq.bandLevelRange
                        val minLevel = levelRange[0].toFloat()
                        val maxLevel = levelRange[1].toFloat()
                        for (b in 0 until bandCount) {
                            val centerHz = eq.getCenterFreq(b.toShort()) / 1000f
                            val interpolatedDb = interpolate10BandGain(centerHz, combinedBands) +
                                ((leftInputGain + rightInputGain) * 0.25f)
                            val targetMb = (interpolatedDb * 100f).coerceIn(minLevel, maxLevel)
                            eq.setBandLevel(b.toShort(), targetMb.toInt().toShort())
                        }
                    }
                    tier = DspEngineTier.LEGACY_EQUALIZER
                }.onFailure { err ->
                    Log.w(TAG, "Failed updating legacy Equalizer: ${err.message}")
                }
            }
        }

        effects.bassBoost?.let { bb ->
            runCatching {
                val subDrive = if (config.subwooferEnabled && !config.mixerLowMuted) {
                    max(config.bassBoost, config.subPunch * 0.9f)
                } else {
                    config.bassBoost
                }
                val active = enabled && subDrive > 0.01f && !config.mixerLowMuted
                bb.enabled = active
                if (active && bb.strengthSupported) {
                    val strength = (subDrive * 1000f).toInt().coerceIn(0, 1000).toShort()
                    bb.setStrength(strength)
                }
            }
        }

        effects.virtualizer?.let { virt ->
            runCatching {
                // Mono mode collapses spatial stereo width to 0
                val stereoActive = config.outputMode == AudioOutputMode.STEREO
                val active = enabled && stereoActive && config.virtualizer > 0.01f
                virt.enabled = active
                if (active && virt.strengthSupported) {
                    val strength = (config.virtualizer * 1000f).toInt().coerceIn(0, 1000).toShort()
                    virt.setStrength(strength)
                }
            }
        }

        effects.loudnessEnhancer?.let { le ->
            runCatching {
                val ampDriveContribution = if (config.powerAmpEnabled) config.ampDrive * 0.45f else 0f
                val totalLoudness = (config.loudness + ampDriveContribution).coerceIn(0f, 1.25f)
                val active = enabled && totalLoudness > 0.01f && !config.mixerMediaMuted
                le.enabled = active
                if (active) {
                    val gainMb = (totalLoudness * 1100f).toInt().coerceIn(0, 1400)
                    le.setTargetGain(gainMb)
                }
            }
        }

        return tier
    }

    private fun configureMbc(dp: DynamicsProcessing, config: DspConfigState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val ampCharacterBoost = if (config.powerAmpEnabled) {
            when (config.ampClassMode) {
                AmpClassMode.CLASS_AB -> 0.15f
                AmpClassMode.CLASS_D -> 0.08f
                AmpClassMode.TUBE_WARM -> 0.28f
            } * config.ampDrive
        } else 0f

        val punch = (config.mbcPunch + ampCharacterBoost).coerceIn(0f, 1f)
        val active = punch > 0.02f || config.crossoverEnabled
        val lowCutoff = if (config.crossoverEnabled) config.subLpfHz.coerceIn(45f, 300f) else 220f
        val highCutoff = if (config.crossoverEnabled) config.midHighSplitHz.coerceIn(1200f, 7000f) else 3200f
        val cutoffs = floatArrayOf(lowCutoff, highCutoff, 20000f)

        val ratio = 1f + (punch * 3.5f)
        val threshold = -14f - (punch * 18f)
        val basePostGain = punch * 3.2f

        for (band in 0 until 3) {
            val bandPostGain = when (band) {
                0 -> if (config.mixerLowMuted) -36f else basePostGain + (if (config.subwooferEnabled) config.subPunch * 2.0f else 0f)
                1 -> if (config.mixerMidMuted) -36f else basePostGain
                else -> if (config.mixerHighMuted) -36f else basePostGain
            }
            val mbcBand = DynamicsProcessing.MbcBand(
                active,
                cutoffs[band],
                8f,
                90f,
                ratio,
                threshold,
                4f,
                -70f,
                1f,
                0f,
                bandPostGain
            )
            dp.setMbcBandAllChannelsTo(band, mbcBand)
        }
    }

    private fun syncVisualizer(preferredSessionId: Int, dspEnabled: Boolean): Boolean {
        val hasRecordPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasRecordPerm || !dspEnabled) {
            releaseVisualizer()
            if (!hasRecordPerm) {
                AudioDspSessionHolder.updateFftSpectrum(List(20) { 0f })
            }
            return false
        }

        val targetSession = preferredSessionId.coerceAtLeast(0)
        if (visualizer != null && visualizerSessionId == targetSession) {
            return true
        }

        releaseVisualizer()
        return runCatching {
            val viz = Visualizer(targetSession).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            v: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) = Unit

                        override fun onFftDataCapture(
                            v: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft == null || fft.size < 4) return
                            val bins = convertFftToBars(fft, 20)
                            AudioDspSessionHolder.updateFftSpectrum(bins)
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                enabled = true
            }
            visualizer = viz
            visualizerSessionId = targetSession
            true
        }.getOrElse {
            releaseVisualizer()
            false
        }
    }

    private fun releaseVisualizer() {
        runCatching { visualizer?.enabled = false }
        runCatching { visualizer?.release() }
        visualizer = null
        visualizerSessionId = -1
    }

    @Synchronized
    fun releaseAll() {
        releaseVisualizer()
        activeEffects.values.forEach { it.release() }
        activeEffects.clear()
    }

    companion object {
        private const val TAG = "AudioDspEngine"

        fun computeEffectivePreEqBands(config: DspConfigState, isLeft: Boolean): List<Float> {
            return List(10) { idx ->
                val rawBand = if (config.outputMode == AudioOutputMode.MONO) {
                    (config.leftBandGainsDb.getOrElse(idx) { 0f } +
                        config.rightBandGainsDb.getOrElse(idx) { 0f }) * 0.5f
                } else if (isLeft || config.linkEqChannels) {
                    config.leftBandGainsDb.getOrElse(idx) { 0f }
                } else {
                    config.rightBandGainsDb.getOrElse(idx) { 0f }
                }

                // Mixer Low / Mid / High bus trims
                val busTrim = when {
                    idx <= 2 -> if (config.mixerLowMuted) -30f else config.mixerLowBusDb
                    idx <= 6 -> if (config.mixerMidMuted) -30f else config.mixerMidBusDb
                    else -> if (config.mixerHighMuted) -30f else config.mixerHighBusDb
                }
                (rawBand + busTrim).coerceIn(-36f, 24f)
            }
        }

        fun computePostEqStage(config: DspConfigState): List<Float> {
            return List(10) { idx ->
                val freq = EQ_BAND_FREQUENCIES_HZ[idx]
                var gain = 0f

                // Subwoofer low-frequency shelf
                if (config.subwooferEnabled && !config.mixerLowMuted) {
                    if (freq <= config.subLpfHz) {
                        val phaseScale = if (config.subPhaseInverted) 0.75f else 1f
                        gain += config.subGainDb * phaseScale
                    } else if (freq <= config.subLpfHz * 1.6f) {
                        gain += config.subGainDb * 0.35f
                    }
                }

                // Crossover Speaker Management HPF / LPF shaping
                if (config.crossoverEnabled) {
                    val slopeFactor = if (config.crossoverSlope == CrossoverSlope.SLOPE_24) 1.0f else 0.55f
                    if (freq < config.mainHpfHz && !config.subwooferEnabled) {
                        val octavesBelow = log10(config.mainHpfHz / freq) / log10(2f)
                        gain -= (octavesBelow * 12f * slopeFactor).coerceIn(0f, 24f)
                    }
                }

                // Power Amplifier harmonic contour
                if (config.powerAmpEnabled) {
                    gain += when (config.ampClassMode) {
                        AmpClassMode.CLASS_AB -> when (idx) {
                            1, 2 -> 1.2f * config.ampDrive
                            5, 6 -> 0.8f * config.ampDrive
                            else -> 0.3f * config.ampDrive
                        }
                        AmpClassMode.CLASS_D -> when (idx) {
                            0, 1 -> 1.0f * config.ampDrive
                            7, 8, 9 -> 1.6f * config.ampDrive
                            else -> 0f
                        }
                        AmpClassMode.TUBE_WARM -> when (idx) {
                            1, 2, 3 -> 1.8f * config.ampDrive
                            4, 5 -> 1.2f * config.ampDrive
                            8, 9 -> -0.6f * config.ampDrive
                            else -> 0f
                        }
                    }
                }

                gain.coerceIn(-24f, 18f)
            }
        }

        fun interpolate10BandGain(freqHz: Float, gainsDb: List<Float>): Float {
            if (gainsDb.size < 10) return 0f
            if (freqHz <= EQ_BAND_FREQUENCIES_HZ.first()) return gainsDb.first()
            if (freqHz >= EQ_BAND_FREQUENCIES_HZ.last()) return gainsDb.last()
            for (i in 0 until EQ_BAND_FREQUENCIES_HZ.size - 1) {
                val f0 = EQ_BAND_FREQUENCIES_HZ[i]
                val f1 = EQ_BAND_FREQUENCIES_HZ[i + 1]
                if (freqHz in f0..f1) {
                    val ratio = ((log10(freqHz) - log10(f0)) / (log10(f1) - log10(f0))).coerceIn(0f, 1f)
                    return gainsDb[i] + (gainsDb[i + 1] - gainsDb[i]) * ratio
                }
            }
            return 0f
        }

        fun convertFftToBars(fft: ByteArray, barCount: Int): List<Float> {
            val n = fft.size / 2
            if (n <= 1) return List(barCount) { 0f }
            val result = FloatArray(barCount)
            for (bar in 0 until barCount) {
                val startBin = (1 + (bar * bar * (n - 2)) / (barCount * barCount)).coerceIn(1, n - 1)
                val endBin = (1 + ((bar + 1) * (bar + 1) * (n - 2)) / (barCount * barCount)).coerceIn(startBin, n - 1)
                var maxMag = 0f
                for (bin in startBin..endBin) {
                    val rfk = fft[2 * bin].toFloat()
                    val ifk = fft[2 * bin + 1].toFloat()
                    val mag = hypot(rfk, ifk)
                    if (mag > maxMag) maxMag = mag
                }
                val db = if (maxMag > 0.5f) (20f * log10(maxMag)) else 0f
                result[bar] = ((db - 4f) / 38f).coerceIn(0f, 1f)
            }
            return result.toList()
        }
    }
}
