package com.example.neonrunner.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.sin

/**
 * Procedural retro synthwave sound effects engine using native AudioTrack.
 * Generates all sounds procedurally in memory - zero external audio assets or downloads required.
 */
class SoundManager(private val isSoundEnabled: () -> Boolean) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sampleRate = 44100
    private val soundBuffers = ConcurrentHashMap<SoundEffect, ShortArray>()

    enum class SoundEffect {
        JUMP,
        SLIDE,
        COIN,
        POWERUP,
        HIT,
        GAME_OVER,
        LANE_SWITCH
    }

    init {
        scope.launch {
            // Pre-synthesize sound buffers in background
            soundBuffers[SoundEffect.JUMP] = synthesizeJump()
            soundBuffers[SoundEffect.SLIDE] = synthesizeSlide()
            soundBuffers[SoundEffect.COIN] = synthesizeCoin()
            soundBuffers[SoundEffect.POWERUP] = synthesizePowerup()
            soundBuffers[SoundEffect.HIT] = synthesizeHit()
            soundBuffers[SoundEffect.GAME_OVER] = synthesizeGameOver()
            soundBuffers[SoundEffect.LANE_SWITCH] = synthesizeLaneSwitch()
        }
    }

    fun play(effect: SoundEffect) {
        if (!isSoundEnabled()) return
        scope.launch {
            val buffer = soundBuffers[effect] ?: return@launch
            playBuffer(buffer)
        }
    }

    private fun playBuffer(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            // Release after playback finishes
            val durationMs = (buffer.size * 1000L) / sampleRate
            Thread.sleep(durationMs + 50)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Exception) {
            // Audio error recovery, fail gracefully without crashing
        }
    }

    // --- Procedural Synthesis Generators ---

    private fun synthesizeJump(): ShortArray {
        val durationMs = 180
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val startFreq = 300.0
        val endFreq = 750.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * t * t
            val envelope = (1.0 - t) * 0.9
            val sample = sin(2.0 * PI * freq * (i.toDouble() / sampleRate)) * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun synthesizeSlide(): ShortArray {
        val durationMs = 150
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val startFreq = 450.0
        val endFreq = 160.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val freq = startFreq - (startFreq - endFreq) * t
            // Add subtle white noise grit for whoosh feel
            val noise = (Math.random() * 2.0 - 1.0) * 0.25
            val envelope = sin(PI * t) * 0.8
            val tone = sin(2.0 * PI * freq * (i.toDouble() / sampleRate)) * 0.75 + noise
            val sample = tone * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun synthesizeCoin(): ShortArray {
        val durationMs = 160
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val half = numSamples / 2
        val freq1 = 987.77 // B5
        val freq2 = 1318.51 // E6

        for (i in 0 until numSamples) {
            val (freq, localT) = if (i < half) {
                Pair(freq1, i.toDouble() / half)
            } else {
                Pair(freq2, (i - half).toDouble() / half)
            }
            val envelope = (1.0 - localT) * 0.85
            val sample = sin(2.0 * PI * freq * (i.toDouble() / sampleRate)) * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun synthesizePowerup(): ShortArray {
        val durationMs = 280
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
        val segmentSize = numSamples / notes.size

        for (i in 0 until numSamples) {
            val noteIdx = (i / segmentSize).coerceAtMost(notes.size - 1)
            val noteFreq = notes[noteIdx]
            val localT = (i % segmentSize).toDouble() / segmentSize
            val globalT = i.toDouble() / numSamples
            val envelope = (1.0 - localT * 0.5) * (1.0 - globalT * 0.3) * 0.85
            val sample = sin(2.0 * PI * noteFreq * (i.toDouble() / sampleRate)) * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun synthesizeHit(): ShortArray {
        val durationMs = 220
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val baseFreq = 120.0 * (1.0 - t * 0.7)
            val noise = (Math.random() * 2.0 - 1.0) * (1.0 - t) * 0.7
            val tone = sin(2.0 * PI * baseFreq * (i.toDouble() / sampleRate)) * 0.6
            val envelope = (1.0 - t * t) * 0.95
            val sample = (tone + noise) * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun synthesizeGameOver(): ShortArray {
        val durationMs = 450
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val startFreq = 500.0
        val endFreq = 90.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val freq = startFreq - (startFreq - endFreq) * t
            val vibrato = sin(2.0 * PI * 12.0 * t) * 20.0
            val envelope = (1.0 - t) * 0.9
            val sample = sin(2.0 * PI * (freq + vibrato) * (i.toDouble() / sampleRate)) * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun synthesizeLaneSwitch(): ShortArray {
        val durationMs = 60
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val freq = 420.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val envelope = (1.0 - t) * 0.6
            val sample = sin(2.0 * PI * freq * (i.toDouble() / sampleRate)) * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }
}
