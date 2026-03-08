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
        private const val MIN_FREQ = 196.0  // G3
        private const val MAX_FREQ = 2637.0 // E7
        private const val CONFIDENCE_THRESHOLD = 0.85
        private const val DB_THRESHOLD = 6.0

        private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    }

    private val _pitchResults = MutableSharedFlow<PitchResult>(replay = 0, extraBufferCapacity = 10)
    val pitchResults: Flow<PitchResult> = _pitchResults.asSharedFlow()

    private var noiseFloorRms: Double = 0.01

    fun setNoiseFloor(rms: Double) {
        noiseFloorRms = rms
    }

    suspend fun processFrame(samples: ShortArray) {
        val floatSamples = DoubleArray(samples.size) { samples[it].toDouble() / Short.MAX_VALUE }

        // Check if signal is above noise floor
        val rms = calculateRms(floatSamples)
        val signalDb = 20 * kotlin.math.log10(rms / noiseFloorRms.coerceAtLeast(0.0001))
        if (signalDb < DB_THRESHOLD) return

        // YIN pitch detection
        val result = yinPitchDetection(floatSamples, SAMPLE_RATE)
        if (result != null && result.confidence > CONFIDENCE_THRESHOLD) {
            if (result.frequency in MIN_FREQ..MAX_FREQ) {
                _pitchResults.emit(result)
            }
        }
    }

    fun yinPitchDetection(buffer: DoubleArray, sampleRate: Int): PitchResult? {
        val bufferSize = buffer.size
        val yinBufferSize = bufferSize / 2

        // Step 1: Difference function
        val difference = DoubleArray(yinBufferSize)
        for (tau in 0 until yinBufferSize) {
            var sum = 0.0
            for (i in 0 until yinBufferSize) {
                val delta = buffer[i] - buffer[i + tau]
                sum += delta * delta
            }
            difference[tau] = sum
        }

        // Step 2: Cumulative mean normalized difference function
        val cmndf = DoubleArray(yinBufferSize)
        cmndf[0] = 1.0
        var runningSum = 0.0
        for (tau in 1 until yinBufferSize) {
            runningSum += difference[tau]
            cmndf[tau] = if (runningSum != 0.0) difference[tau] * tau / runningSum else 1.0
        }

        // Step 3: Absolute threshold
        val threshold = 0.15
        val minTau = (sampleRate.toDouble() / MAX_FREQ).toInt().coerceAtLeast(2)
        val maxTau = (sampleRate.toDouble() / MIN_FREQ).toInt().coerceAtMost(yinBufferSize - 1)

        var bestTau = -1
        for (tau in minTau..maxTau) {
            if (cmndf[tau] < threshold) {
                // Find the local minimum
                while (tau + 1 < yinBufferSize && cmndf[tau + 1] < cmndf[tau]) {
                    bestTau = tau + 1
                }
                if (bestTau == -1) bestTau = tau
                break
            }
        }

        if (bestTau == -1) {
            // No pitch found below threshold — find global minimum
            var minVal = Double.MAX_VALUE
            for (tau in minTau..maxTau) {
                if (cmndf[tau] < minVal) {
                    minVal = cmndf[tau]
                    bestTau = tau
                }
            }
            if (minVal > 0.5) return null // No clear pitch
        }

        // Step 4: Parabolic interpolation
        val interpolatedTau = if (bestTau > 0 && bestTau < yinBufferSize - 1) {
            val s0 = cmndf[bestTau - 1]
            val s1 = cmndf[bestTau]
            val s2 = cmndf[bestTau + 1]
            val betterTau = bestTau + (s2 - s0) / (2 * (2 * s1 - s2 - s0))
            betterTau
        } else {
            bestTau.toDouble()
        }

        val frequency = sampleRate / interpolatedTau
        val confidence = 1.0 - (cmndf[bestTau].coerceIn(0.0, 1.0))

        if (frequency < MIN_FREQ || frequency > MAX_FREQ) return null

        val (noteName, cents) = frequencyToNote(frequency)

        return PitchResult(
            frequency = frequency,
            noteName = noteName,
            centsDeviation = cents,
            confidence = confidence
        )
    }

    private fun frequencyToNote(frequency: Double): Pair<String, Double> {
        val a4 = 440.0
        val semitonesFromA4 = 12 * log2(frequency / a4)
        val nearestSemitone = semitonesFromA4.roundToInt()
        val cents = (semitonesFromA4 - nearestSemitone) * 100

        val noteIndex = ((nearestSemitone % 12) + 12 + 9) % 12 // A = index 9
        val octave = 4 + (nearestSemitone + 9) / 12
        val noteName = "${NOTE_NAMES[noteIndex]}$octave"

        return Pair(noteName, cents)
    }

    private fun calculateRms(samples: DoubleArray): Double {
        var sum = 0.0
        for (s in samples) sum += s * s
        return kotlin.math.sqrt(sum / samples.size)
    }
}
