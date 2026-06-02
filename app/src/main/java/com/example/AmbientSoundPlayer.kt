package com.example

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.sin
import java.util.concurrent.ConcurrentHashMap

enum class SoundType(val displayName: String) {
    ZEN_BOWL("Zen Bowl"),
    CALM_BELL("Calm Bell"),
    WOOD_BLOCK("Wood Block"),
    GENTLE_GONG("Gentle Gong")
}

class AmbientSoundPlayer {
    private val sampleRate = 44100
    private val soundBuffers = ConcurrentHashMap<SoundType, ShortArray>()
    
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("AmbientSoundPlayer", "Unhandled exception in playerScopeCoroutines", throwable)
    }
    private val playerScope = CoroutineScope(Dispatchers.Default + exceptionHandler + SupervisorJob())

    init {
        // Pre-synthesize all sounds off the Main thread to keep the application startup snappy and responsive
        playerScope.launch {
            try {
                for (type in SoundType.values()) {
                    if (!soundBuffers.containsKey(type)) {
                        soundBuffers[type] = synthesizeSound(type)
                    }
                }
            } catch (e: Throwable) {
                Log.e("AmbientSoundPlayer", "Failed to pre-synthesize sounds asynchronously", e)
            }
        }
    }

    private fun synthesizeSound(type: SoundType): ShortArray {
        val durationSec = when (type) {
            SoundType.ZEN_BOWL -> 4.5
            SoundType.CALM_BELL -> 2.5
            SoundType.WOOD_BLOCK -> 0.3
            SoundType.GENTLE_GONG -> 4.8
        }
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)

        val fundamental = when (type) {
            SoundType.ZEN_BOWL -> 220.0     // A3: Deep resonates
            SoundType.CALM_BELL -> 523.25   // C5: Clear melodic ring
            SoundType.WOOD_BLOCK -> 880.0   // A5: Earthy percussion click
            SoundType.GENTLE_GONG -> 110.0  // A2: Deep vibrating sub-gong
        }

        val decayRate = when (type) {
            SoundType.ZEN_BOWL -> 1.4
            SoundType.CALM_BELL -> 1.9
            SoundType.WOOD_BLOCK -> 22.0
            SoundType.GENTLE_GONG -> 1.0
        }

        val attackSec = when (type) {
            SoundType.ZEN_BOWL -> 0.08
            SoundType.CALM_BELL -> 0.01
            SoundType.WOOD_BLOCK -> 0.002
            SoundType.GENTLE_GONG -> 0.15
        }

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            
            // Generate composite overtone harmonic combinations for rich metallic acoustics
            val raw = when (type) {
                SoundType.ZEN_BOWL -> {
                    val fundamentalWave = sin(2.0 * Math.PI * fundamental * t)
                    val overtone1 = sin(2.0 * Math.PI * (fundamental * 1.503) * t) * 0.4
                    val overtone2 = sin(2.0 * Math.PI * (fundamental * 2.012) * t) * 0.25
                    val overtone3 = sin(2.0 * Math.PI * (fundamental * 2.62) * t) * 0.15
                    val overtone4 = sin(2.0 * Math.PI * (fundamental * 3.51) * t) * 0.08
                    (fundamentalWave + overtone1 + overtone2 + overtone3 + overtone4) / 1.88
                }
                SoundType.CALM_BELL -> {
                    val fundamentalWave = sin(2.0 * Math.PI * fundamental * t)
                    val overtone1 = sin(2.0 * Math.PI * (fundamental * 2.0) * t) * 0.35
                    val overtone2 = sin(2.0 * Math.PI * (fundamental * 3.01) * t) * 0.2
                    val overtone3 = sin(2.0 * Math.PI * (fundamental * 4.15) * t) * 0.10
                    (fundamentalWave + overtone1 + overtone2 + overtone3) / 1.65
                }
                SoundType.WOOD_BLOCK -> {
                    val fundamentalWave = sin(2.0 * Math.PI * fundamental * t)
                    val partial1 = sin(2.0 * Math.PI * (fundamental * 1.68) * t) * 0.26
                    val partial2 = sin(2.0 * Math.PI * (fundamental * 2.45) * t) * 0.12
                    (fundamentalWave + partial1 + partial2) / 1.38
                }
                SoundType.GENTLE_GONG -> {
                    val fundamentalWave = sin(2.0 * Math.PI * fundamental * t)
                    val subHarmonic = sin(2.0 * Math.PI * (fundamental * 0.5) * t) * 0.18
                    val partial1 = sin(2.0 * Math.PI * (fundamental * 1.25) * t) * 0.35
                    val partial2 = sin(2.0 * Math.PI * (fundamental * 1.76) * t) * 0.22
                    val partial3 = sin(2.0 * Math.PI * (fundamental * 2.32) * t) * 0.12
                    (fundamentalWave + subHarmonic + partial1 + partial2 + partial3) / 1.87
                }
            }

            // Exponential decay envelope
            val envelope = exp(-decayRate * t)
            
            // Linear rise attack to eliminate initial clicking and pops
            val attack = if (t < attackSec) {
                t / attackSec
            } else {
                1.0
            }

            // Amplify and map to 16-bit PCM integer range with safe custom clamping
            val sampleVal = (raw * envelope * attack * 32767.0).toInt()
            val clampedVal = if (sampleVal < -32768) -32768 else if (sampleVal > 32767) 32767 else sampleVal
            samples[i] = clampedVal.toShort()
        }

        return samples
    }

    fun playSound(type: SoundType) {
        playerScope.launch {
            try {
                // Ensure buffer is synthesized (synthesize on-demand if background initialization has not finished yet)
                val buffer = soundBuffers[type] ?: synthesizeSound(type).also { soundBuffers[type] = it }

                // Using USAGE_ASSISTANCE_SONIFICATION treats this as an overlay notice sound.
                // It ensures the stream is mixed harmoniously alongside other system sounds
                // and background players (like Spotify) instead of pausing them.
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val format = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(maxOf(minBufferSize, buffer.size * 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()
                audioTrack.write(buffer, 0, buffer.size)
                
                // Allow the track to complete playback fully before clearing resources
                val delayTimeMs = (buffer.size.toFloat() / sampleRate * 1000).toLong()
                kotlinx.coroutines.delay(delayTimeMs)
                
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Throwable) {
                Log.e("AmbientSoundPlayer", "Error during sound playback", e)
            }
        }
    }
}
