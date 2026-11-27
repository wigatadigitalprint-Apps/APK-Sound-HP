package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.audio.AmpClassMode
import com.example.audio.AudioOutputMode
import com.example.audio.BuiltInPresets
import com.example.audio.CHANNEL_VOL_MAX_DB
import com.example.audio.CHANNEL_VOL_MIN_DB
import com.example.audio.CrossoverSlope
import com.example.audio.DspConfigState
import com.example.audio.EQ_MAX_DB
import com.example.audio.EQ_MIN_DB
import com.example.audio.EqPreset
import com.example.audio.PREAMP_MAX_DB
import com.example.audio.PREAMP_MIN_DB
import com.example.audio.RoutingMode
import com.example.audio.SpeakerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.eqDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "eq_pro_audio_prefs"
)

class EqPreferencesRepository(private val context: Context) {

    private object Keys {
        val DSP_ENABLED = booleanPreferencesKey("dsp_enabled")
        val ACTIVE_PRESET_ID = stringPreferencesKey("active_preset_id")
        val LINK_EQ_CHANNELS = booleanPreferencesKey("link_eq_channels")
        val LEFT_BAND_GAINS_JSON = stringPreferencesKey("left_band_gains_json")
        val RIGHT_BAND_GAINS_JSON = stringPreferencesKey("right_band_gains_json")
        val PRE_AMP_DB = floatPreferencesKey("pre_amp_db")

        val OUTPUT_MODE = stringPreferencesKey("output_mode")
        val LINK_LR_VOLUME = booleanPreferencesKey("link_lr_volume")
        val LEFT_VOLUME_DB = floatPreferencesKey("left_volume_db")
        val RIGHT_VOLUME_DB = floatPreferencesKey("right_volume_db")
        val LEFT_MUTED = booleanPreferencesKey("left_muted")
        val RIGHT_MUTED = booleanPreferencesKey("right_muted")

        val MIXER_MASTER_DB = floatPreferencesKey("mixer_master_db")
        val MIXER_MEDIA_DB = floatPreferencesKey("mixer_media_db")
        val MIXER_LOW_DB = floatPreferencesKey("mixer_low_db")
        val MIXER_MID_DB = floatPreferencesKey("mixer_mid_db")
        val MIXER_HIGH_DB = floatPreferencesKey("mixer_high_db")
        val MIXER_MEDIA_MUTED = booleanPreferencesKey("mixer_media_muted")
        val MIXER_LOW_MUTED = booleanPreferencesKey("mixer_low_muted")
        val MIXER_MID_MUTED = booleanPreferencesKey("mixer_mid_muted")
        val MIXER_HIGH_MUTED = booleanPreferencesKey("mixer_high_muted")

        val BASS_BOOST = floatPreferencesKey("bass_boost")
        val VIRTUALIZER = floatPreferencesKey("virtualizer")
        val LOUDNESS = floatPreferencesKey("loudness")
        val MBC_PUNCH = floatPreferencesKey("mbc_punch")

        val CROSSOVER_ENABLED = booleanPreferencesKey("crossover_enabled")
        val SPEAKER_PROFILE = stringPreferencesKey("speaker_profile")
        val MAIN_HPF_HZ = floatPreferencesKey("main_hpf_hz")
        val SUB_LPF_HZ = floatPreferencesKey("sub_lpf_hz")
        val MID_HIGH_SPLIT_HZ = floatPreferencesKey("mid_high_split_hz")
        val CROSSOVER_SLOPE = stringPreferencesKey("crossover_slope")
        val SPEAKER_DELAY_MS = floatPreferencesKey("speaker_delay_ms")

        val POWER_AMP_ENABLED = booleanPreferencesKey("power_amp_enabled")
        val AMP_CLASS_MODE = stringPreferencesKey("amp_class_mode")
        val AMP_DRIVE = floatPreferencesKey("amp_drive")
        val LIMITER_ENABLED = booleanPreferencesKey("limiter_enabled")
        val LIMITER_THRESHOLD_DB = floatPreferencesKey("limiter_threshold_db")

        val SUBWOOFER_ENABLED = booleanPreferencesKey("subwoofer_enabled")
        val SUB_GAIN_DB = floatPreferencesKey("sub_gain_db")
        val SUB_PUNCH = floatPreferencesKey("sub_punch")
        val SUB_PHASE_INVERTED = booleanPreferencesKey("sub_phase_inverted")

        val ROUTING_MODE = stringPreferencesKey("routing_mode")
        val CUSTOM_PRESETS_JSON = stringPreferencesKey("custom_presets_json")
    }

    val configFlow: Flow<DspConfigState> = context.eqDataStore.data.map { prefs ->
        val defaultPreset = BuiltInPresets.PopGloss
        val leftGains = decodeGains(prefs[Keys.LEFT_BAND_GAINS_JSON], defaultPreset.gainsDb)
        val rightGains = decodeGains(prefs[Keys.RIGHT_BAND_GAINS_JSON], leftGains)

        val outputMode = enumOrDefault(prefs[Keys.OUTPUT_MODE], AudioOutputMode.STEREO)
        val routingMode = enumOrDefault(prefs[Keys.ROUTING_MODE], RoutingMode.AUTO)
        val speakerProfile = enumOrDefault(prefs[Keys.SPEAKER_PROFILE], SpeakerProfile.SYSTEM_2_1)
        val crossoverSlope = enumOrDefault(prefs[Keys.CROSSOVER_SLOPE], CrossoverSlope.SLOPE_24)
        val ampClassMode = enumOrDefault(prefs[Keys.AMP_CLASS_MODE], AmpClassMode.CLASS_AB)

        DspConfigState(
            isDspEnabled = prefs[Keys.DSP_ENABLED] ?: true,
            activePresetId = prefs[Keys.ACTIVE_PRESET_ID] ?: defaultPreset.id,
            linkEqChannels = prefs[Keys.LINK_EQ_CHANNELS] ?: true,
            leftBandGainsDb = leftGains,
            rightBandGainsDb = rightGains,
            preAmpDb = (prefs[Keys.PRE_AMP_DB] ?: 0f).coerceIn(PREAMP_MIN_DB, PREAMP_MAX_DB),
            outputMode = outputMode,
            linkLrVolume = prefs[Keys.LINK_LR_VOLUME] ?: true,
            leftVolumeDb = (prefs[Keys.LEFT_VOLUME_DB] ?: 0f).coerceIn(CHANNEL_VOL_MIN_DB, CHANNEL_VOL_MAX_DB),
            rightVolumeDb = (prefs[Keys.RIGHT_VOLUME_DB] ?: 0f).coerceIn(CHANNEL_VOL_MIN_DB, CHANNEL_VOL_MAX_DB),
            leftMuted = prefs[Keys.LEFT_MUTED] ?: false,
            rightMuted = prefs[Keys.RIGHT_MUTED] ?: false,
            mixerMasterDb = (prefs[Keys.MIXER_MASTER_DB] ?: 0f).coerceIn(-24f, 12f),
            mixerMediaBusDb = (prefs[Keys.MIXER_MEDIA_DB] ?: 0f).coerceIn(-15f, 12f),
            mixerLowBusDb = (prefs[Keys.MIXER_LOW_DB] ?: 0f).coerceIn(-15f, 12f),
            mixerMidBusDb = (prefs[Keys.MIXER_MID_DB] ?: 0f).coerceIn(-15f, 12f),
            mixerHighBusDb = (prefs[Keys.MIXER_HIGH_DB] ?: 0f).coerceIn(-15f, 12f),
            mixerMediaMuted = prefs[Keys.MIXER_MEDIA_MUTED] ?: false,
            mixerLowMuted = prefs[Keys.MIXER_LOW_MUTED] ?: false,
            mixerMidMuted = prefs[Keys.MIXER_MID_MUTED] ?: false,
            mixerHighMuted = prefs[Keys.MIXER_HIGH_MUTED] ?: false,
            bassBoost = (prefs[Keys.BASS_BOOST] ?: defaultPreset.bassBoost).coerceIn(0f, 1f),
            virtualizer = (prefs[Keys.VIRTUALIZER] ?: defaultPreset.virtualizer).coerceIn(0f, 1f),
            loudness = (prefs[Keys.LOUDNESS] ?: defaultPreset.loudness).coerceIn(0f, 1f),
            mbcPunch = (prefs[Keys.MBC_PUNCH] ?: defaultPreset.mbcPunch).coerceIn(0f, 1f),
            crossoverEnabled = prefs[Keys.CROSSOVER_ENABLED] ?: false,
            speakerProfile = speakerProfile,
            mainHpfHz = (prefs[Keys.MAIN_HPF_HZ] ?: 85f).coerceIn(25f, 220f),
            subLpfHz = (prefs[Keys.SUB_LPF_HZ] ?: 90f).coerceIn(40f, 180f),
            midHighSplitHz = (prefs[Keys.MID_HIGH_SPLIT_HZ] ?: 2800f).coerceIn(1200f, 6500f),
            crossoverSlope = crossoverSlope,
            speakerDelayMs = (prefs[Keys.SPEAKER_DELAY_MS] ?: 0f).coerceIn(0f, 20f),
            powerAmpEnabled = prefs[Keys.POWER_AMP_ENABLED] ?: true,
            ampClassMode = ampClassMode,
            ampDrive = (prefs[Keys.AMP_DRIVE] ?: 0.25f).coerceIn(0f, 1f),
            limiterEnabled = prefs[Keys.LIMITER_ENABLED] ?: true,
            limiterThresholdDb = (prefs[Keys.LIMITER_THRESHOLD_DB] ?: -1.5f).coerceIn(-12f, 0f),
            subwooferEnabled = prefs[Keys.SUBWOOFER_ENABLED] ?: true,
            subGainDb = (prefs[Keys.SUB_GAIN_DB] ?: 3.0f).coerceIn(-12f, 15f),
            subPunch = (prefs[Keys.SUB_PUNCH] ?: 0.40f).coerceIn(0f, 1f),
            subPhaseInverted = prefs[Keys.SUB_PHASE_INVERTED] ?: false,
            routingMode = routingMode
        )
    }

    val customPresetsFlow: Flow<List<EqPreset>> = context.eqDataStore.data.map { prefs ->
        decodeCustomPresets(prefs[Keys.CUSTOM_PRESETS_JSON])
    }

    suspend fun setDspEnabled(enabled: Boolean) {
        context.eqDataStore.edit { it[Keys.DSP_ENABLED] = enabled }
    }

    suspend fun setLinkEqChannels(linked: Boolean) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.LINK_EQ_CHANNELS] = linked
            if (linked) {
                val left = prefs[Keys.LEFT_BAND_GAINS_JSON]
                if (left != null) prefs[Keys.RIGHT_BAND_GAINS_JSON] = left
            }
        }
    }

    suspend fun updateChannelBandGain(
        channel: String, // "LEFT", "RIGHT", or "LINKED"
        bandIndex: Int,
        gainDb: Float
    ) {
        if (bandIndex !in 0..9) return
        val clamped = gainDb.coerceIn(EQ_MIN_DB, EQ_MAX_DB)
        context.eqDataStore.edit { prefs ->
            val left = decodeGains(prefs[Keys.LEFT_BAND_GAINS_JSON], BuiltInPresets.PopGloss.gainsDb).toMutableList()
            val right = decodeGains(prefs[Keys.RIGHT_BAND_GAINS_JSON], left).toMutableList()
            when (channel) {
                "LEFT" -> {
                    left[bandIndex] = clamped
                    prefs[Keys.LEFT_BAND_GAINS_JSON] = encodeGains(left)
                }
                "RIGHT" -> {
                    right[bandIndex] = clamped
                    prefs[Keys.RIGHT_BAND_GAINS_JSON] = encodeGains(right)
                }
                else -> {
                    left[bandIndex] = clamped
                    right[bandIndex] = clamped
                    val encoded = encodeGains(left)
                    prefs[Keys.LEFT_BAND_GAINS_JSON] = encoded
                    prefs[Keys.RIGHT_BAND_GAINS_JSON] = encoded
                }
            }
            prefs[Keys.ACTIVE_PRESET_ID] = "custom"
        }
    }

    suspend fun applyPreset(preset: EqPreset) {
        context.eqDataStore.edit { prefs ->
            val encoded = encodeGains(preset.gainsDb)
            prefs[Keys.ACTIVE_PRESET_ID] = preset.id
            prefs[Keys.LEFT_BAND_GAINS_JSON] = encoded
            prefs[Keys.RIGHT_BAND_GAINS_JSON] = encoded
            prefs[Keys.BASS_BOOST] = preset.bassBoost.coerceIn(0f, 1f)
            prefs[Keys.VIRTUALIZER] = preset.virtualizer.coerceIn(0f, 1f)
            prefs[Keys.LOUDNESS] = preset.loudness.coerceIn(0f, 1f)
            prefs[Keys.MBC_PUNCH] = preset.mbcPunch.coerceIn(0f, 1f)
        }
    }

    suspend fun resetFlat() {
        applyPreset(BuiltInPresets.Flat)
        context.eqDataStore.edit { prefs ->
            prefs[Keys.PRE_AMP_DB] = 0f
            prefs[Keys.LEFT_VOLUME_DB] = 0f
            prefs[Keys.RIGHT_VOLUME_DB] = 0f
            prefs[Keys.LEFT_MUTED] = false
            prefs[Keys.RIGHT_MUTED] = false
        }
    }

    suspend fun setPreAmp(db: Float) {
        context.eqDataStore.edit { it[Keys.PRE_AMP_DB] = db.coerceIn(PREAMP_MIN_DB, PREAMP_MAX_DB) }
    }

    suspend fun setOutputMode(mode: AudioOutputMode) {
        context.eqDataStore.edit { it[Keys.OUTPUT_MODE] = mode.name }
    }

    suspend fun setLinkLrVolume(linked: Boolean) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.LINK_LR_VOLUME] = linked
            if (linked) {
                val left = prefs[Keys.LEFT_VOLUME_DB] ?: 0f
                prefs[Keys.RIGHT_VOLUME_DB] = left
            }
        }
    }

    suspend fun setLeftVolumeDb(db: Float) {
        val clamped = db.coerceIn(CHANNEL_VOL_MIN_DB, CHANNEL_VOL_MAX_DB)
        context.eqDataStore.edit { prefs ->
            prefs[Keys.LEFT_VOLUME_DB] = clamped
            if (prefs[Keys.LINK_LR_VOLUME] != false) {
                prefs[Keys.RIGHT_VOLUME_DB] = clamped
            }
        }
    }

    suspend fun setRightVolumeDb(db: Float) {
        val clamped = db.coerceIn(CHANNEL_VOL_MIN_DB, CHANNEL_VOL_MAX_DB)
        context.eqDataStore.edit { prefs ->
            prefs[Keys.RIGHT_VOLUME_DB] = clamped
            if (prefs[Keys.LINK_LR_VOLUME] != false) {
                prefs[Keys.LEFT_VOLUME_DB] = clamped
            }
        }
    }

    suspend fun setLeftMuted(muted: Boolean) {
        context.eqDataStore.edit { it[Keys.LEFT_MUTED] = muted }
    }

    suspend fun setRightMuted(muted: Boolean) {
        context.eqDataStore.edit { it[Keys.RIGHT_MUTED] = muted }
    }

    suspend fun setMixerMasterDb(db: Float) {
        context.eqDataStore.edit { it[Keys.MIXER_MASTER_DB] = db.coerceIn(-24f, 12f) }
    }

    suspend fun setMixerMediaBusDb(db: Float) {
        context.eqDataStore.edit { it[Keys.MIXER_MEDIA_DB] = db.coerceIn(-15f, 12f) }
    }

    suspend fun setMixerLowBusDb(db: Float) {
        context.eqDataStore.edit { it[Keys.MIXER_LOW_DB] = db.coerceIn(-15f, 12f) }
    }

    suspend fun setMixerMidBusDb(db: Float) {
        context.eqDataStore.edit { it[Keys.MIXER_MID_DB] = db.coerceIn(-15f, 12f) }
    }

    suspend fun setMixerHighBusDb(db: Float) {
        context.eqDataStore.edit { it[Keys.MIXER_HIGH_DB] = db.coerceIn(-15f, 12f) }
    }

    suspend fun toggleMixerMute(bus: String) {
        context.eqDataStore.edit { prefs ->
            when (bus) {
                "MEDIA" -> prefs[Keys.MIXER_MEDIA_MUTED] = !(prefs[Keys.MIXER_MEDIA_MUTED] ?: false)
                "LOW" -> prefs[Keys.MIXER_LOW_MUTED] = !(prefs[Keys.MIXER_LOW_MUTED] ?: false)
                "MID" -> prefs[Keys.MIXER_MID_MUTED] = !(prefs[Keys.MIXER_MID_MUTED] ?: false)
                "HIGH" -> prefs[Keys.MIXER_HIGH_MUTED] = !(prefs[Keys.MIXER_HIGH_MUTED] ?: false)
            }
        }
    }

    suspend fun resetMixer() {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.MIXER_MASTER_DB] = 0f
            prefs[Keys.MIXER_MEDIA_DB] = 0f
            prefs[Keys.MIXER_LOW_DB] = 0f
            prefs[Keys.MIXER_MID_DB] = 0f
            prefs[Keys.MIXER_HIGH_DB] = 0f
            prefs[Keys.MIXER_MEDIA_MUTED] = false
            prefs[Keys.MIXER_LOW_MUTED] = false
            prefs[Keys.MIXER_MID_MUTED] = false
            prefs[Keys.MIXER_HIGH_MUTED] = false
        }
    }

    suspend fun setBassBoost(amount: Float) {
        context.eqDataStore.edit {
            it[Keys.BASS_BOOST] = amount.coerceIn(0f, 1f)
            it[Keys.ACTIVE_PRESET_ID] = "custom"
        }
    }

    suspend fun setVirtualizer(amount: Float) {
        context.eqDataStore.edit {
            it[Keys.VIRTUALIZER] = amount.coerceIn(0f, 1f)
            it[Keys.ACTIVE_PRESET_ID] = "custom"
        }
    }

    suspend fun setLoudness(amount: Float) {
        context.eqDataStore.edit {
            it[Keys.LOUDNESS] = amount.coerceIn(0f, 1f)
            it[Keys.ACTIVE_PRESET_ID] = "custom"
        }
    }

    suspend fun setMbcPunch(amount: Float) {
        context.eqDataStore.edit {
            it[Keys.MBC_PUNCH] = amount.coerceIn(0f, 1f)
            it[Keys.ACTIVE_PRESET_ID] = "custom"
        }
    }

    suspend fun setCrossoverEnabled(enabled: Boolean) {
        context.eqDataStore.edit { it[Keys.CROSSOVER_ENABLED] = enabled }
    }

    suspend fun applySpeakerProfile(profile: SpeakerProfile) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.SPEAKER_PROFILE] = profile.name
            prefs[Keys.MAIN_HPF_HZ] = profile.mainHpfHz
            prefs[Keys.SUB_LPF_HZ] = profile.subLpfHz
            prefs[Keys.MID_HIGH_SPLIT_HZ] = profile.midHighSplitHz
        }
    }

    suspend fun setMainHpfHz(hz: Float) {
        context.eqDataStore.edit { it[Keys.MAIN_HPF_HZ] = hz.coerceIn(25f, 220f) }
    }

    suspend fun setSubLpfHz(hz: Float) {
        context.eqDataStore.edit { it[Keys.SUB_LPF_HZ] = hz.coerceIn(40f, 180f) }
    }

    suspend fun setMidHighSplitHz(hz: Float) {
        context.eqDataStore.edit { it[Keys.MID_HIGH_SPLIT_HZ] = hz.coerceIn(1200f, 6500f) }
    }

    suspend fun setCrossoverSlope(slope: CrossoverSlope) {
        context.eqDataStore.edit { it[Keys.CROSSOVER_SLOPE] = slope.name }
    }

    suspend fun setSpeakerDelayMs(ms: Float) {
        context.eqDataStore.edit { it[Keys.SPEAKER_DELAY_MS] = ms.coerceIn(0f, 20f) }
    }

    suspend fun setPowerAmpEnabled(enabled: Boolean) {
        context.eqDataStore.edit { it[Keys.POWER_AMP_ENABLED] = enabled }
    }

    suspend fun setAmpClassMode(mode: AmpClassMode) {
        context.eqDataStore.edit { it[Keys.AMP_CLASS_MODE] = mode.name }
    }

    suspend fun setAmpDrive(drive: Float) {
        context.eqDataStore.edit { it[Keys.AMP_DRIVE] = drive.coerceIn(0f, 1f) }
    }

    suspend fun setLimiterEnabled(enabled: Boolean) {
        context.eqDataStore.edit { it[Keys.LIMITER_ENABLED] = enabled }
    }

    suspend fun setLimiterThreshold(db: Float) {
        context.eqDataStore.edit { it[Keys.LIMITER_THRESHOLD_DB] = db.coerceIn(-12f, 0f) }
    }

    suspend fun setSubwooferEnabled(enabled: Boolean) {
        context.eqDataStore.edit { it[Keys.SUBWOOFER_ENABLED] = enabled }
    }

    suspend fun setSubGainDb(db: Float) {
        context.eqDataStore.edit { it[Keys.SUB_GAIN_DB] = db.coerceIn(-12f, 15f) }
    }

    suspend fun setSubPunch(punch: Float) {
        context.eqDataStore.edit { it[Keys.SUB_PUNCH] = punch.coerceIn(0f, 1f) }
    }

    suspend fun setSubPhaseInverted(inverted: Boolean) {
        context.eqDataStore.edit { it[Keys.SUB_PHASE_INVERTED] = inverted }
    }

    suspend fun setRoutingMode(mode: RoutingMode) {
        context.eqDataStore.edit { it[Keys.ROUTING_MODE] = mode.name }
    }

    suspend fun saveCustomPreset(name: String, state: DspConfigState) {
        val cleanName = name.trim().take(24)
        if (cleanName.isEmpty()) return
        val newPreset = EqPreset(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            gainsDb = state.leftBandGainsDb,
            bassBoost = state.bassBoost,
            virtualizer = state.virtualizer,
            loudness = state.loudness,
            mbcPunch = state.mbcPunch,
            isBuiltIn = false
        )
        context.eqDataStore.edit { prefs ->
            val list = decodeCustomPresets(prefs[Keys.CUSTOM_PRESETS_JSON]).toMutableList()
            list.removeAll { it.name.equals(cleanName, ignoreCase = true) }
            list.add(0, newPreset)
            prefs[Keys.CUSTOM_PRESETS_JSON] = encodeCustomPresets(list.take(15))
            prefs[Keys.ACTIVE_PRESET_ID] = newPreset.id
        }
    }

    suspend fun deleteCustomPreset(presetId: String) {
        context.eqDataStore.edit { prefs ->
            val list = decodeCustomPresets(prefs[Keys.CUSTOM_PRESETS_JSON])
                .filterNot { it.id == presetId }
            prefs[Keys.CUSTOM_PRESETS_JSON] = encodeCustomPresets(list)
            if (prefs[Keys.ACTIVE_PRESET_ID] == presetId) {
                prefs[Keys.ACTIVE_PRESET_ID] = "custom"
            }
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }

    private fun encodeGains(gains: List<Float>): String {
        val arr = JSONArray()
        gains.take(10).forEach { arr.put(it.toDouble()) }
        return arr.toString()
    }

    private fun decodeGains(json: String?, fallback: List<Float>): List<Float> {
        if (json.isNullOrBlank()) return fallback
        return runCatching {
            val arr = JSONArray(json)
            if (arr.length() != 10) return fallback
            List(10) { idx -> arr.optDouble(idx, 0.0).toFloat().coerceIn(EQ_MIN_DB, EQ_MAX_DB) }
        }.getOrDefault(fallback)
    }

    private fun encodeCustomPresets(presets: List<EqPreset>): String {
        val arr = JSONArray()
        presets.forEach { preset ->
            val obj = JSONObject().apply {
                put("id", preset.id)
                put("name", preset.name)
                put("bassBoost", preset.bassBoost.toDouble())
                put("virtualizer", preset.virtualizer.toDouble())
                put("loudness", preset.loudness.toDouble())
                put("mbcPunch", preset.mbcPunch.toDouble())
                val gainsArr = JSONArray()
                preset.gainsDb.forEach { gainsArr.put(it.toDouble()) }
                put("gainsDb", gainsArr)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeCustomPresets(json: String?): List<EqPreset> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                val gainsArr = obj.getJSONArray("gainsDb")
                val gains = List(10) { band ->
                    gainsArr.optDouble(band, 0.0).toFloat().coerceIn(EQ_MIN_DB, EQ_MAX_DB)
                }
                EqPreset(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    gainsDb = gains,
                    bassBoost = obj.optDouble("bassBoost", 0.0).toFloat().coerceIn(0f, 1f),
                    virtualizer = obj.optDouble("virtualizer", 0.0).toFloat().coerceIn(0f, 1f),
                    loudness = obj.optDouble("loudness", 0.0).toFloat().coerceIn(0f, 1f),
                    mbcPunch = obj.optDouble("mbcPunch", 0.0).toFloat().coerceIn(0f, 1f),
                    isBuiltIn = false
                )
            }
        }.getOrDefault(emptyList())
    }
}
