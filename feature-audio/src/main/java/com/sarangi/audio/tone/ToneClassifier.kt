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

    private val _toneResults = MutableSharedFlow<ToneResult>(extraBufferCapacity = 10)
    val toneResults: Flow<ToneResult> = _toneResults.asSharedFlow()

    suspend fun processFrame(audioData: ShortArray, noiseLevel: com.sarangi.core.model.NoiseLevel) {
        if (noiseLevel == com.sarangi.core.model.NoiseLevel.HIGH) return

        val floatData = audioData.map { it.toDouble() / Short.MAX_VALUE }.toDoubleArray()
        if (floatData.all { it == 0.0 }) return

        val rms = calculateRms(floatData)
        if (rms < 0.01) return

        val attackTime = detectAttackTime(floatData)
        val sustainStability = calculateSustainStability(floatData)
        val spectralCentroid = calculateSpectralCentroid(floatData)

        val classification = classify(attackTime, sustainStability, spectralCentroid)
        val confidence = if (noiseLevel == com.sarangi.core.model.NoiseLevel.MODERATE) 0.6 else 0.85

        val result = ToneResult(
            classification = classification,
            attackTimeMs = attackTime,
            sustainStability = sustainStability,
            spectralCentroid = spectralCentroid,
            confidence = confidence
        )
        _toneResults.emit(result)
    }

    internal fun classify(attackTimeMs: Double, sustainStability: Double, spectralCentroid: Double): ToneClassification {
        return when {
            spectralCentroid > 5000 -> ToneClassification.WHISTLE_TONE
            sustainStability < 0.3 && spectralCentroid > 3000 -> ToneClassification.SCRATCHY
            sustainStability < 0.5 && spectralCentroid > 2000 -> ToneClassification.SLIGHTLY_SCRATCHY
            sustainStability > 0.7 && spectralCentroid < 3000 -> ToneClassification.CLEAN
            else -> ToneClassification.UNCERTAIN
        }
    }

    internal fun detectAttackTime(data: DoubleArray): Double {
        val envelope = calculateEnvelope(data)
        val maxAmplitude = envelope.maxOrNull() ?: return 0.0
        val threshold = maxAmplitude * 0.9
        val attackSample = envelope.indexOfFirst { it >= threshold }
        return if (attackSample >= 0) {
            (attackSample.toDouble() / 44100) * 1000 // ms
        } else 0.0
    }

    internal fun calculateSustainStability(data: DoubleArray): Double {
        val envelope = calculateEnvelope(data)
        if (envelope.size < 10) return 0.0

        // Skip attack phase (first 10%)
        val sustainStart = (envelope.size * 0.1).toInt()
        val sustainPortion = envelope.sliceArray(sustainStart until envelope.size)
        if (sustainPortion.isEmpty()) return 0.0

        val mean = sustainPortion.average()
        if (mean == 0.0) return 0.0
        val variance = sustainPortion.map { (it - mean) * (it - mean) }.average()
        val cv = sqrt(variance) / mean
        return (1.0 - cv).coerceIn(0.0, 1.0)
    }

    internal fun calculateSpectralCentroid(data: DoubleArray): Double {
        val n = data.size
        val halfN = n / 2
        var weightedSum = 0.0
        var magnitudeSum = 0.0

        for (k in 0 until halfN.coerceAtMost(256)) {
            var real = 0.0
            var imag = 0.0
            for (t in 0 until n.coerceAtMost(512)) {
                val angle = 2.0 * Math.PI * k * t / n
                real += data[t] * kotlin.math.cos(angle)
                imag -= data[t] * kotlin.math.sin(angle)
            }
            val magnitude = sqrt(real * real + imag * imag)
            val freq = k.toDouble() * 44100 / n
            weightedSum += freq * magnitude
            magnitudeSum += magnitude
        }

        return if (magnitudeSum > 0) weightedSum / magnitudeSum else 0.0
    }

    private fun calculateEnvelope(data: DoubleArray): DoubleArray {
        val windowSize = 64
        val envelope = DoubleArray(data.size / windowSize)
        for (i in envelope.indices) {
            val start = i * windowSize
            val end = (start + windowSize).coerceAtMost(data.size)
            var sum = 0.0
            for (j in start until end) {
                sum += data[j] * data[j]
            }
            envelope[i] = sqrt(sum / (end - start))
        }
        return envelope
    }

    private fun calculateRms(data: DoubleArray): Double {
        var sum = 0.0
        for (sample in data) sum += sample * sample
        return sqrt(sum / data.size)
    }
}
