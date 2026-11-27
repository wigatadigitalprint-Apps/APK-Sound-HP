package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect

class AudioSessionBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)

        when (action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                if (sessionId > 0) {
                    AudioDspSessionHolder.onSessionOpened(context, sessionId, packageName)
                }
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                if (sessionId > 0) {
                    AudioDspSessionHolder.onSessionClosed(sessionId)
                }
            }
        }
    }
}
