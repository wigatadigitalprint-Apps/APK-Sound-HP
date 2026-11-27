package com.example

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioDspEngine
import com.example.audio.AudioDspSessionHolder
import com.example.audio.AudioSessionBroadcastReceiver
import com.example.audio.BuiltInPresets
import com.example.audio.EQ_MAX_DB
import com.example.audio.EQ_MIN_DB
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app_name matches EQ Pro Audio`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("EQ Pro Audio", appName)
    }

    @Test
    fun `youtube session broadcast opens and closes session`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = AudioSessionBroadcastReceiver()

        val openIntent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 9876)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, "com.google.android.youtube")
        }
        receiver.onReceive(context, openIntent)

        val opened = AudioDspSessionHolder.externalSessions.value.find { it.sessionId == 9876 }
        assertTrue(opened != null && opened.isYouTube && opened.appLabel == "YouTube")

        val closeIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 9876)
        }
        receiver.onReceive(context, closeIntent)

        val afterClose = AudioDspSessionHolder.externalSessions.value.find { it.sessionId == 9876 }
        assertTrue(afterClose == null)
    }

    @Test
    fun `all built-in presets have 10 valid bands`() {
        BuiltInPresets.all.forEach { preset ->
            assertEquals(10, preset.gainsDb.size)
            preset.gainsDb.forEach { db ->
                assertTrue(db in EQ_MIN_DB..EQ_MAX_DB)
            }
        }
    }

    @Test
    fun `10-band interpolation maps center frequencies accurately`() {
        val gains = listOf(6f, 4f, 2f, 0f, -2f, 0f, 2f, 4f, 6f, 8f)
        assertEquals(6f, AudioDspEngine.interpolate10BandGain(31f, gains), 0.01f)
        assertEquals(0f, AudioDspEngine.interpolate10BandGain(1000f, gains), 0.01f)
        assertEquals(8f, AudioDspEngine.interpolate10BandGain(16000f, gains), 0.01f)
    }
}
