package com.example

import com.example.audio.AmpClassMode
import com.example.audio.AudioDspEngine
import com.example.audio.AudioOutputMode
import com.example.audio.BuiltInPresets
import com.example.audio.DspConfigState
import com.example.audio.EQ_MAX_DB
import com.example.audio.EQ_MIN_DB
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun all_built_in_presets_have_10_valid_bands() {
        BuiltInPresets.all.forEach { preset ->
            assertEquals(10, preset.gainsDb.size)
            preset.gainsDb.forEach { db ->
                assertTrue(db in EQ_MIN_DB..EQ_MAX_DB)
            }
        }
    }

    @Test
    fun ten_band_interpolation_maps_center_frequencies_accurately() {
        val gains = listOf(6f, 4f, 2f, 0f, -2f, 0f, 2f, 4f, 6f, 8f)
        assertEquals(6f, AudioDspEngine.interpolate10BandGain(31f, gains), 0.01f)
        assertEquals(0f, AudioDspEngine.interpolate10BandGain(1000f, gains), 0.01f)
        assertEquals(8f, AudioDspEngine.interpolate10BandGain(16000f, gains), 0.01f)
    }

    @Test
    fun separate_lr_eq_and_mono_sum_behave_accurately() {
        val left = List(10) { 6f }
        val right = List(10) { -2f }

        val stereoConfig = DspConfigState(
            outputMode = AudioOutputMode.STEREO,
            linkEqChannels = false,
            leftBandGainsDb = left,
            rightBandGainsDb = right
        )
        assertEquals(6f, AudioDspEngine.computeEffectivePreEqBands(stereoConfig, isLeft = true)[0], 0.01f)
        assertEquals(-2f, AudioDspEngine.computeEffectivePreEqBands(stereoConfig, isLeft = false)[0], 0.01f)

        val monoConfig = stereoConfig.copy(outputMode = AudioOutputMode.MONO)
        assertEquals(2f, AudioDspEngine.computeEffectivePreEqBands(monoConfig, isLeft = true)[0], 0.01f)
        assertEquals(2f, AudioDspEngine.computeEffectivePreEqBands(monoConfig, isLeft = false)[0], 0.01f)
    }

    @Test
    fun subwoofer_and_power_amp_boost_post_eq_low_frequencies() {
        val cfg = DspConfigState(
            subwooferEnabled = true,
            subGainDb = 6f,
            subLpfHz = 90f,
            powerAmpEnabled = true,
            ampClassMode = AmpClassMode.TUBE_WARM,
            ampDrive = 1f
        )
        val postEq = AudioDspEngine.computePostEqStage(cfg)
        assertTrue(postEq[0] >= 6f) // 31Hz boosted by Subwoofer
        assertTrue(postEq[1] >= 7f) // 62Hz boosted by Sub + Tube Warm
    }

    @Test
    fun fft_conversion_outputs_normalized_bar_magnitudes() {
        val fakeFft = ByteArray(128) { idx ->
            if (idx % 2 == 0) 40 else 20
        }
        val bars = AudioDspEngine.convertFftToBars(fakeFft, 20)
        assertEquals(20, bars.size)
        bars.forEach { mag ->
            assertTrue(mag in 0f..1f)
        }
    }
}
