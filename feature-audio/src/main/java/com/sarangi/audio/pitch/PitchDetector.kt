package com.sarangi.audio.pitch

import com.sarangi.core.model.PitchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

@Singleton
class PitchDetector @Inject constructor() {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val WINDOW_SIZE = 2048
        private const val THRESHOLD = 0.15
        private const val MIN_FREQ = 196.0  // G3
        private const val MAX_FREQ = 2637.0 // E7
        private const val CONFIDENCE_THRESHOLD = 0.85

        private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    }

    private val _pitchResults = MutableSharedFlow<PitchResult>(extraBufferCapacity = 10)
    val pitchResults: Flow<PitchResult> = _pitchResults.asSharedFlow()

    suspend fun processFrame(audioData: ShortArray, noiseFloorRms: Double = 0.0) {
        if (audioData.size < WINDOW_SIZE) return

        val floatData = audioData.map { it.toDouble() / Short.MAX_VALUE }.toDoubleArray()

        // Check signal level
        val rms = calculateRms(floatData)
        val signalAboveNoise = if (noiseFloorRms > 0) {
            20 * log2(rms / noiseFloorRms) >= 6.0
        } else {
            rms > 0.01
        }
        if (!signalAboveNoise) return

        // YIN algorithm
        val yinResult = yin(floatData)
        if (yinResult.confidence >= CONFIDENCE_THRESHOLD &&
            yinResult.frequency in MIN_FREQ..MAX_FREQ) {
            val (noteName, cents) = frequencyToNote(yinResult.frequency)
            _pitchResults.emit(
                PitchResult(
                    frequency = yinResult.frequency,
                    noteName = noteName,
                    centsDeviation = cents,
                    confidence = yinResult.confidence
                )
            )
        }
    }

    internal fun yin(buffer: DoubleArray): YinResult {
        val halfSize = buffer.size / 2

        // Step 1: Difference function
        val diff = DoubleArray(halfSize)
        for (tau in 1 until halfSize) {
            var sum = 0.0
            for (j in 0 until halfSize) {
                val delta = buffer[j] - buffer[j + tau]
                sum += delta * delta
            }
            diff[tau] = sum
        }

        // Step 2: Cumulative mean normalized difference
        val cmndf = DoubleArray(halfSize)
        cmndf[0] = 1.0
        var runningSum = 0.0
        for (tau in 1 until halfSize) {
            runningSum += diff[tau]
            cmndf[tau] = if (runningSum > 0) diff[tau] * tau / runningSum else 1.0
        }

        // Step 3: Absolute threshold
        var tauEstimate = -1
        for (tau in 2 until halfSize) {
            if (cmndf[tau] < THRESHOLD) {
                // Find the minimum in this valley
                while (tau + 1 < halfSize && cmndf[tau + 1] < cmndf[tau]) {
                    // continue to next tau in loop
                    break
                }
                tauEstimate = tau
                break
            }
        }

        if (tauEstimate == -1) {
            return YinResult(0.0, 0.0)
        }

        // Step 4: Parabolic interpolation
        val betterTau = if (tauEstimate > 0 && tauEstimate < halfSize - 1) {
            val s0 = cmndf[tauEstimate - 1]
            val s1 = cmndf[tauEstimate]
            val s2 = cmndf[tauEstimate + 1]
            val adjustment = (s2 - s0) / (2 * (2 * s1 - s2 - s0))
            if (adjustment.isFinite()) tauEstimate + adjustment else tauEstimate.toDouble()
        } else {
            tauEstimate.toDouble()
        }

        val frequency = SAMPLE_RATE / betterTau
        val confidence = 1.0 - (cmndf.getOrElse(tauEstimate) { 1.0 })

        return YinResult(frequency, confidence.coerceIn(0.0, 1.0))
    }

    internal fun frequencyToNote(frequency: Double): Pair<String, Double> {
        val semitones = 12 * log2(frequency / 440.0) + 69
        val nearestNote = semitones.roundToInt()
        val cents = (semitones - nearestNote) * 100

        val noteName = NOTE_NAMES[((nearestNote % 12) + 12) % 12]
        val octave = (nearestNote / 12) - 1
        return Pair("$noteName$octave", cents)
    }

    private fun calculateRms(data: DoubleArray): Double {
        var sum = 0.0
        for (sample in data) {
            sum += sample * sample
        }
        return kotlin.math.sqrt(sum / data.size)
    }

    data class YinResult(val frequency: Double, val confidence: Double)
}
