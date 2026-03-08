package com.sarangi.audio.tone

import com.sarangi.core.model.ToneClassification
import com.sarangi.core.model.ToneResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class ToneClassifier @Inject constructor() {

    companion object {
        private const val SAMPLE_RATE = 44100
    }

    private val _toneResults = MutableSharedFlow<ToneResult>(replay = 0, extraBufferCapacity = 5)
    val toneResults: Flow<ToneResult> = _toneResults.asSharedFlow()

    private var isNoisyEnvironment = false

    fun setNoisyEnvironment(noisy: Boolean) {
        isNoisyEnvironment = noisy
    }

    suspend fun processFrame(samples: ShortArray) {
        if (isNoisyEnvironment) {
            _toneResults.emit(
                ToneResult(
                    classification = ToneClassification.UNCERTAIN,
                    attackTimeMs = 0.0,
                    sustainStability = 0.0,
                    spectralCentroid = 0.0,
                    confidence = 0.0
                )
            )
            return
        }

        val floatSamples = DoubleArray(samples.size) { samples[it].toDouble() / Short.MAX_VALUE }
        val rms = calculateRms(floatSamples)
        if (rms < 0.01) return // Too quiet

        val attackTime = calculateAttackTime(floatSamples)
        val sustainStability = calculateSustainStability(floatSamples)
        val spectralCentroid = calculateSpectralCentroid(floatSamples)

        val classification = classify(attackTime, sustainStability, spectralCentroid)
        val confidence = if (classification == ToneClassification.UNCERTAIN) 0.3 else 0.7

        _toneResults.emit(
            ToneResult(
                classification = classification,
                attackTimeMs = attackTime,
                sustainStability = sustainStability,
                spectralCentroid = spectralCentroid,
                confidence = confidence
            )
        )
    }

    private fun classify(
        attackTimeMs: Double,
        sustainStability: Double,
        spectralCentroid: Double
    ): ToneClassification {
        // Very high spectral centroid suggests scratchy or whistle tone
        if (spectralCentroid > 5000) return ToneClassification.WHISTLE_TONE
        if (sustainStability < 0.3 && spectralCentroid > 3000) return ToneClassification.SCRATCHY
        if (sustainStability < 0.5 && spectralCentroid > 2000) return ToneClassification.SLIGHTLY_SCRATCHY
        if (sustainStability > 0.6 && spectralCentroid < 3000) return ToneClassification.CLEAN
        return ToneClassification.UNCERTAIN
    }

    private fun calculateAttackTime(samples: DoubleArray): Double {
        val windowSize = (SAMPLE_RATE * 0.01).toInt() // 10ms windows
        var maxRms = 0.0
        var maxIndex = 0

        var i = 0
        while (i + windowSize < samples.size) {
            val windowRms = calculateRms(samples.sliceArray(i until i + windowSize))
            if (windowRms > maxRms) {
                maxRms = windowRms
                maxIndex = i
            }
            i += windowSize
        }

        return maxIndex.toDouble() / SAMPLE_RATE * 1000 // ms
    }

    private fun calculateSustainStability(samples: DoubleArray): Double {
        val windowSize = (SAMPLE_RATE * 0.05).toInt() // 50ms windows
        val rmsValues = mutableListOf<Double>()

        // Skip attack (first 20%)
        val start = samples.size / 5
        var i = start
        while (i + windowSize < samples.size) {
            rmsValues.add(calculateRms(samples.sliceArray(i until i + windowSize)))
            i += windowSize
        }

        if (rmsValues.size < 2) return 0.5

        val mean = rmsValues.average()
        if (mean == 0.0) return 0.0
        val variance = rmsValues.map { (it - mean) * (it - mean) }.average()
        val cv = sqrt(variance) / mean

        return (1.0 - cv).coerceIn(0.0, 1.0)
    }

    private fun calculateSpectralCentroid(samples: DoubleArray): Double {
        val n = samples.size.coerceAtMost(2048)
        var weightedSum = 0.0
        var magnitudeSum = 0.0

        for (k in 1 until n / 2) {
            var real = 0.0
            var imag = 0.0
            for (t in 0 until n) {
                val angle = 2 * Math.PI * k * t / n
                real += samples[t] * kotlin.math.cos(angle)
                imag -= samples[t] * kotlin.math.sin(angle)
            }
            val magnitude = sqrt(real * real + imag * imag)
            val frequency = k.toDouble() * SAMPLE_RATE / n
            weightedSum += frequency * magnitude
            magnitudeSum += magnitude
        }

        return if (magnitudeSum > 0) weightedSum / magnitudeSum else 0.0
    }

    private fun calculateRms(samples: DoubleArray): Double {
        var sum = 0.0
        for (s in samples) sum += s * s
        return sqrt(sum / samples.size)
    }
}
