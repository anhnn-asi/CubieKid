package com.example.scanner

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates crisp, dependency-free audio feedback chimes during 30s video Rubik scanning:
 * - 1 chime when a new face is captured (1/6..5/6)
 * - 2 ascending chimes when all 6 faces are captured (6/6)
 *
 * Uses a dedicated serialized background scope so the 2-chime completion cue finishes cleanly
 * even when ScannerScreen immediately navigates to the Solver screen on the 6th face.
 */
object ScanSoundPlayer {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val soundScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO.limitedParallelism(1)
    )

    fun playSingleFaceBeep() {
        soundScope.launch {
            playTone(freqHz = 880.0, durationMs = 120)
        }
    }

    fun playAllSixFacesBeep() {
        soundScope.launch {
            playTone(freqHz = 880.0, durationMs = 110)
            delay(95L)
            playTone(freqHz = 1174.66, durationMs = 180)
        }
    }

    fun playInspectionWarningBeep(isTwelveSecondAlert: Boolean) {
        soundScope.launch {
            if (isTwelveSecondAlert) {
                playTone(freqHz = 1046.5, durationMs = 110)
                delay(75L)
                playTone(freqHz = 1318.5, durationMs = 140)
            } else {
                playTone(freqHz = 987.77, durationMs = 130)
            }
        }
    }

    fun playTrainerSolvedChime() {
        soundScope.launch {
            playTone(freqHz = 783.99, durationMs = 95)
            delay(70L)
            playTone(freqHz = 1046.5, durationMs = 150)
        }
    }

    fun playRubikMoveClick(isDoubleTurn: Boolean = false) {
        soundScope.launch {
            playPcm(buildMoveClickPcm())
            if (isDoubleTurn) {
                delay(55L)
                playPcm(buildMoveClickPcm(baseFreqHz = 510.0))
            }
        }
    }

    private suspend fun playTone(freqHz: Double, durationMs: Int) {
        val sampleRate = 22050
        val pcm = buildSineWavePcm(freqHz = freqHz, durationMs = durationMs, sampleRate = sampleRate)
        playPcm(pcm, sampleRate = sampleRate, waitMs = durationMs.toLong() + 30L)
    }

    private suspend fun playPcm(pcm: ShortArray, sampleRate: Int = 22050, waitMs: Long = 45L) {
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            try {
                track.write(pcm, 0, pcm.size)
                track.play()
                delay(waitMs)
                track.stop()
            } finally {
                track.release()
            }
        }
    }

    private fun buildMoveClickPcm(baseFreqHz: Double = 440.0, sampleRate: Int = 22050): ShortArray {
        val durationMs = 34
        val numSamples = ((sampleRate * durationMs) / 1000).coerceAtLeast(1)
        return ShortArray(numSamples) { i ->
            val t = i.toDouble() / numSamples
            val decay = exp(-6.5 * t)
            val fundamental = sin(2.0 * PI * baseFreqHz * i / sampleRate)
            val overtone = 0.45 * sin(2.0 * PI * (baseFreqHz * 2.15) * i / sampleRate)
            val sample = (fundamental + overtone) * decay * 0.42 * Short.MAX_VALUE
            sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun buildSineWavePcm(freqHz: Double, durationMs: Int, sampleRate: Int): ShortArray {
        val numSamples = ((sampleRate * durationMs) / 1000).coerceAtLeast(1)
        val fadeSamples = ((sampleRate * 12) / 1000).coerceAtLeast(1).coerceAtMost(numSamples / 2)
        return ShortArray(numSamples) { i ->
            val env = when {
                i < fadeSamples -> i.toDouble() / fadeSamples
                i > numSamples - fadeSamples -> (numSamples - i).toDouble() / fadeSamples
                else -> 1.0
            }
            val sample = sin(2.0 * PI * freqHz * i / sampleRate) * env * 0.55 * Short.MAX_VALUE
            sample.toInt().toShort()
        }
    }
}
