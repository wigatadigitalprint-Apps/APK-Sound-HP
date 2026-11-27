package com.example.audio

val EQ_BAND_FREQUENCIES_HZ = listOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
val EQ_BAND_CUTOFF_HZ = listOf(45f, 90f, 180f, 350f, 700f, 1400f, 2800f, 5600f, 11200f, 20000f)
val EQ_BAND_LABELS = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")

const val EQ_MIN_DB = -15f
const val EQ_MAX_DB = 15f
const val PREAMP_MIN_DB = -12f
const val PREAMP_MAX_DB = 12f
const val CHANNEL_VOL_MIN_DB = -24f
const val CHANNEL_VOL_MAX_DB = 12f

enum class RoutingMode(val title: String, val subtitle: String) {
    AUTO(
        title = "Auto (YouTube + Global)",
        subtitle = "Attaches to YouTube & player sessions with Global Mix fallback"
    ),
    GLOBAL_MIX(
        title = "Global Output Mix",
        subtitle = "Processes all device audio via master mix (Session 0)"
    ),
    SESSION_ONLY(
        title = "Detected Sessions Only",
        subtitle = "Attaches strictly to external player audio session broadcasts"
    )
}

enum class AudioOutputMode(val label: String) {
    STEREO("STEREO"),
    MONO("MONO")
}

enum class EqChannelEditTarget(val label: String) {
    LINKED("L+R LINK"),
    LEFT("LEFT (L)"),
    RIGHT("RIGHT (R)")
}

enum class CrossoverSlope(val label: String, val dbPerOctave: Int) {
    SLOPE_12("12 dB/oct", 12),
    SLOPE_24("24 dB/oct", 24)
}

enum class SpeakerProfile(
    val label: String,
    val mainHpfHz: Float,
    val subLpfHz: Float,
    val midHighSplitHz: Float
) {
    HEADPHONES("Headphones / IEM", 30f, 80f, 3200f),
    SYSTEM_2_1("2.1 Studio + Sub", 85f, 90f, 2800f),
    CAR_DSP("Car Audio DSP", 70f, 80f, 3500f),
    PA_SYSTEM("PA / Sound System", 100f, 110f, 2400f),
    PHONE_SPEAKER("Phone / Mini", 140f, 120f, 3800f)
}

enum class AmpClassMode(val label: String, val description: String) {
    CLASS_AB("Class-AB", "Warm analog punch & smooth transient headroom"),
    CLASS_D("Class-D", "High-efficiency crisp precision & tight bass"),
    TUBE_WARM("Tube Warm", "Rich harmonic saturation & musical compression")
}

enum class DspEngineTier(val label: String) {
    DYNAMICS_PROCESSING("DynamicsProcessing Dual-Channel DSP (API 28+)"),
    LEGACY_EQUALIZER("Standard Android Equalizer DSP"),
    UNAVAILABLE("Initializing DSP Engine")
}

data class AudioSessionInfo(
    val sessionId: Int,
    val packageName: String,
    val appLabel: String,
    val isYouTube: Boolean,
    val openedAtMs: Long
)

data class EqPreset(
    val id: String,
    val name: String,
    val gainsDb: List<Float>,
    val bassBoost: Float = 0f,
    val virtualizer: Float = 0f,
    val loudness: Float = 0f,
    val mbcPunch: Float = 0f,
    val isBuiltIn: Boolean = true
)

object BuiltInPresets {
    val Flat = EqPreset(
        id = "flat",
        name = "Flat",
        gainsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        bassBoost = 0f,
        virtualizer = 0f,
        loudness = 0f,
        mbcPunch = 0f
    )

    val YouTubeVocal = EqPreset(
        id = "yt_vocal",
        name = "YT Vocal",
        gainsDb = listOf(-2.5f, -1.5f, 0f, 1.5f, 3.0f, 4.0f, 4.5f, 3.0f, 1.5f, 0.5f),
        bassBoost = 0.10f,
        virtualizer = 0.15f,
        loudness = 0.25f,
        mbcPunch = 0.30f
    )

    val DeepBass = EqPreset(
        id = "deep_bass",
        name = "Deep Bass",
        gainsDb = listOf(7.5f, 6.5f, 4.5f, 2.0f, 0f, -0.5f, 0.5f, 1.5f, 2.0f, 1.5f),
        bassBoost = 0.72f,
        virtualizer = 0.25f,
        loudness = 0.20f,
        mbcPunch = 0.35f
    )

    val EdmClub = EqPreset(
        id = "edm_club",
        name = "EDM Club",
        gainsDb = listOf(6.5f, 5.5f, 3.0f, 0f, -1.5f, 0.5f, 2.5f, 4.5f, 5.5f, 4.5f),
        bassBoost = 0.60f,
        virtualizer = 0.45f,
        loudness = 0.30f,
        mbcPunch = 0.45f
    )

    val RockLive = EqPreset(
        id = "rock_live",
        name = "Rock Live",
        gainsDb = listOf(4.5f, 3.5f, 1.5f, -1.0f, -1.5f, 1.0f, 3.0f, 4.5f, 4.0f, 3.0f),
        bassBoost = 0.35f,
        virtualizer = 0.35f,
        loudness = 0.20f,
        mbcPunch = 0.40f
    )

    val Acoustic = EqPreset(
        id = "acoustic",
        name = "Acoustic",
        gainsDb = listOf(1.5f, 2.5f, 2.0f, 1.0f, 1.5f, 1.0f, 2.0f, 3.0f, 3.5f, 2.5f),
        bassBoost = 0.15f,
        virtualizer = 0.30f,
        loudness = 0.10f,
        mbcPunch = 0.15f
    )

    val PopGloss = EqPreset(
        id = "pop_gloss",
        name = "Pop Gloss",
        gainsDb = listOf(3.5f, 4.0f, 2.0f, 0f, 1.0f, 2.5f, 3.5f, 3.0f, 2.5f, 3.5f),
        bassBoost = 0.40f,
        virtualizer = 0.35f,
        loudness = 0.25f,
        mbcPunch = 0.30f
    )

    val LateNight = EqPreset(
        id = "late_night",
        name = "Late Night",
        gainsDb = listOf(2.0f, 1.5f, 1.0f, 1.5f, 2.0f, 2.5f, 2.0f, 1.0f, 0f, -1.0f),
        bassBoost = 0.25f,
        virtualizer = 0.20f,
        loudness = 0.55f,
        mbcPunch = 0.65f
    )

    val all: List<EqPreset> = listOf(
        Flat,
        YouTubeVocal,
        DeepBass,
        EdmClub,
        RockLive,
        Acoustic,
        PopGloss,
        LateNight
    )
}

data class DspConfigState(
    val isDspEnabled: Boolean = true,
    val activePresetId: String = BuiltInPresets.PopGloss.id,
    // Dual-Channel Separate Equalizer (Left & Right 10-band)
    val linkEqChannels: Boolean = true,
    val leftBandGainsDb: List<Float> = BuiltInPresets.PopGloss.gainsDb,
    val rightBandGainsDb: List<Float> = BuiltInPresets.PopGloss.gainsDb,
    val preAmpDb: Float = 0f,
    // Stereo / Mono & Separate L/R Volume
    val outputMode: AudioOutputMode = AudioOutputMode.STEREO,
    val linkLrVolume: Boolean = true,
    val leftVolumeDb: Float = 0f,
    val rightVolumeDb: Float = 0f,
    val leftMuted: Boolean = false,
    val rightMuted: Boolean = false,
    // Audio Mixer Buses (dB trims & Mute)
    val mixerMasterDb: Float = 0f,
    val mixerMediaBusDb: Float = 0f,
    val mixerLowBusDb: Float = 0f,
    val mixerMidBusDb: Float = 0f,
    val mixerHighBusDb: Float = 0f,
    val mixerMediaMuted: Boolean = false,
    val mixerLowMuted: Boolean = false,
    val mixerMidMuted: Boolean = false,
    val mixerHighMuted: Boolean = false,
    // FX & Tone
    val bassBoost: Float = BuiltInPresets.PopGloss.bassBoost,
    val virtualizer: Float = BuiltInPresets.PopGloss.virtualizer,
    val loudness: Float = BuiltInPresets.PopGloss.loudness,
    val mbcPunch: Float = BuiltInPresets.PopGloss.mbcPunch,
    // Crossover / Speaker Management (DSP)
    val crossoverEnabled: Boolean = false,
    val speakerProfile: SpeakerProfile = SpeakerProfile.SYSTEM_2_1,
    val mainHpfHz: Float = 85f,
    val subLpfHz: Float = 90f,
    val midHighSplitHz: Float = 2800f,
    val crossoverSlope: CrossoverSlope = CrossoverSlope.SLOPE_24,
    val speakerDelayMs: Float = 0f,
    // Power Amplifier Stage
    val powerAmpEnabled: Boolean = true,
    val ampClassMode: AmpClassMode = AmpClassMode.CLASS_AB,
    val ampDrive: Float = 0.25f,
    val limiterEnabled: Boolean = true,
    val limiterThresholdDb: Float = -1.5f,
    // Dedicated Subwoofer Engine
    val subwooferEnabled: Boolean = true,
    val subGainDb: Float = 3.0f,
    val subPunch: Float = 0.40f,
    val subPhaseInverted: Boolean = false,
    // Routing
    val routingMode: RoutingMode = RoutingMode.AUTO
)

data class DspRuntimeStatus(
    val serviceRunning: Boolean = false,
    val engineTier: DspEngineTier = DspEngineTier.UNAVAILABLE,
    val attachedSessions: List<AudioSessionInfo> = emptyList(),
    val isGlobalMixAttached: Boolean = false,
    val isMediaPlayingOnDevice: Boolean = false,
    val liveFftActive: Boolean = false,
    val activeAppSummary: String = "Waiting for YouTube or media playback",
    val lastError: String? = null
)

enum class TestToneType(val label: String) {
    SUB_BASS("Sub 65Hz"),
    LEFT_RIGHT_CHECK("L/R Check"),
    WARM_CHORD("Chord"),
    PINK_SWEEP("Sweep")
}
