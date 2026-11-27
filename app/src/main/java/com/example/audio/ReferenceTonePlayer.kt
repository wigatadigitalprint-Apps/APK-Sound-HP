package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class ReferenceTonePlayer {
    private var playJob: Job? = null
    private var audioTrack: AudioTrack? = null

    fun start(scope: CoroutineScope, toneType: TestToneType) {
        stop()
        playJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val frames = 1024
            val minBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(frames * 4)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            val pcm = ShortArray(frames * 2) // Stereo interleaved L, R
            var phase1 = 0.0
            var phase2 = 0.0
            var phase3 = 0.0
            var sweepPhase = 0.0
            var lrSampleCounter = 0L

            runCatching { track.play() }

            while (isActive) {
                for (f in 0 until frames) {
                    var left = 0.0
                    var right = 0.0

                    when (toneType) {
                        TestToneType.SUB_BASS -> {
                            phase1 += 2.0 * PI * 65.0 / sampleRate
                            phase2 += 2.0 * PI * 130.0 / sampleRate
                            val s = 0.65 * sin(phase1) + 0.25 * sin(phase2)
                            left = s
                            right = s
                        }
                        TestToneType.LEFT_RIGHT_CHECK -> {
                            // Alternates 1.2s Left (440Hz), 1.2s Right (554.37Hz), 1.2s Center Dual
                            val cycle = ((lrSampleCounter / (sampleRate * 1.2).toLong()) % 3L).toInt()
                            phase1 += 2.0 * PI * 440.0 / sampleRate
                            phase2 += 2.0 * PI * 554.37 / sampleRate
                            when (cycle) {
                                0 -> {
                                    left = 0.65 * sin(phase1)
                                    right = 0.0
                                }
                                1 -> {
                                    left = 0.0
                                    right = 0.65 * sin(phase2)
                                }
                                else -> {
                                    val c = 0.45 * sin(phase1) + 0.35 * sin(phase2)
                                    left = c
                                    right = c
                                }
                            }
                            lrSampleCounter++
                        }
                        TestToneType.WARM_CHORD -> {
                            phase1 += 2.0 * PI * 110.0 / sampleRate
                            phase2 += 2.0 * PI * 277.18 / sampleRate
                            phase3 += 2.0 * PI * 659.25 / sampleRate
                            val s = 0.45 * sin(phase1) + 0.30 * sin(phase2) + 0.20 * sin(phase3)
                            left = s
                            right = s
                        }
                        TestToneType.PINK_SWEEP -> {
                            sweepPhase += 2.0 * PI * 0.28 / sampleRate
                            val lfo = 0.5 + 0.5 * sin(sweepPhase)
                            val freq = 80.0 + lfo * 4200.0
                            phase1 += 2.0 * PI * freq / sampleRate
                            phase2 += 2.0 * PI * 120.0 / sampleRate
                            val s = 0.50 * sin(phase1) + 0.35 * sin(phase2)
                            left = s
                            right = s
                        }
                    }

                    pcm[f * 2] = (left * 11000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    pcm[f * 2 + 1] = (right * 11000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.write(pcm, 0, pcm.size)
                }
            }

            runCatching {
                track.stop()
                track.release()
            }
        }
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
    }
}
