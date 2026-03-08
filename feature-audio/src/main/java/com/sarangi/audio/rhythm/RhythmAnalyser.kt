package com.sarangi.audio.rhythm

import com.sarangi.core.model.RhythmResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

@Singleton
class RhythmAnalyser @Inject constructor() {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val HOP_SIZE = 512
        private const val FFT_SIZE = 1024
        private const val ONSET_THRESHOLD = 1.5
    }

    private val _rhythmResults = MutableSharedFlow<RhythmResult>(extraBufferCapacity = 10)
    val rhythmResults: Flow<RhythmResult> = _rhythmResults.asSharedFlow()

    private val onsetTimes = mutableListOf<Long>()
    private var previousSpectrum: DoubleArray? = null
    private var frameCount = 0

    fun reset() {
        onsetTimes.clear()
        previousSpectrum = null
        frameCount = 0
    }

    suspend fun processFrame(audioData: ShortArray) {
        val floatData = audioData.map { it.toDouble() / Short.MAX_VALUE }.toDoubleArray()

        // Process in windows
        var offset = 0
        while (offset + FFT_SIZE <= floatData.size) {
            val window = floatData.sliceArray(offset until offset + FFT_SIZE)
            val spectrum = computeSpectralMagnitude(window)

            previousSpectrum?.let { prevSpec ->
                val flux = computeSpectralFlux(prevSpec, spectrum)
                if (flux > ONSET_THRESHOLD) {
                    val timeMs = (frameCount.toLong() * HOP_SIZE * 1000) / SAMPLE_RATE
                    onsetTimes.add(timeMs)
                }
            }

            previousSpectrum = spectrum
            frameCount++
            offset += HOP_SIZE
        }
    }

    suspend fun getResult(expectedIntervalMs: Double? = null): RhythmResult? {
        if (onsetTimes.size < 2) return null

        val iois = mutableListOf<Double>()
        for (i in 1 until onsetTimes.size) {
            iois.add((onsetTimes[i] - onsetTimes[i - 1]).toDouble())
        }

        val deviations = if (expectedIntervalMs != null) {
            iois.map { it - expectedIntervalMs }
        } else {
            val avgIoi = iois.average()
            iois.map { it - avgIoi }
        }

        val avgDeviation = deviations.map { abs(it) }.average()

        val result = RhythmResult(
            onsetTimesMs = onsetTimes.toList(),
            deviationsMs = deviations,
            averageDeviationMs = avgDeviation
        )
        _rhythmResults.emit(result)
        return result
    }

    internal fun computeSpectralMagnitude(frame: DoubleArray): DoubleArray {
        // Simple DFT magnitude (not FFT for simplicity — fine for onset detection)
        val n = frame.size
        val halfN = n / 2
        val magnitude = DoubleArray(halfN)

        for (k in 0 until halfN) {
            var real = 0.0
            var imag = 0.0
            for (t in 0 until n) {
                val angle = 2.0 * Math.PI * k * t / n
                real += frame[t] * kotlin.math.cos(angle)
                imag -= frame[t] * kotlin.math.sin(angle)
            }
            magnitude[k] = sqrt(real * real + imag * imag)
        }
        return magnitude
    }

    internal fun computeSpectralFlux(previous: DoubleArray, current: DoubleArray): Double {
        var flux = 0.0
        val size = minOf(previous.size, current.size)
        for (i in 0 until size) {
            val diff = current[i] - previous[i]
            if (diff > 0) flux += diff
        }
        return flux
    }
}
