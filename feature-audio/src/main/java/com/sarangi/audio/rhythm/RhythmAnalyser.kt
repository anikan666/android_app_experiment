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
        private const val ONSET_THRESHOLD = 1.5
    }

    private val _rhythmResults = MutableSharedFlow<RhythmResult>(replay = 0, extraBufferCapacity = 5)
    val rhythmResults: Flow<RhythmResult> = _rhythmResults.asSharedFlow()

    private val onsetTimes = mutableListOf<Long>()
    private var previousSpectrum: DoubleArray? = null
    private var sampleCount = 0L

    fun reset() {
        onsetTimes.clear()
        previousSpectrum = null
        sampleCount = 0L
    }

    suspend fun processFrame(samples: ShortArray) {
        val floatSamples = DoubleArray(samples.size) { samples[it].toDouble() / Short.MAX_VALUE }

        // Compute magnitude spectrum using simple DFT on windows
        val windowSize = HOP_SIZE.coerceAtMost(floatSamples.size)
        var offset = 0
        while (offset + windowSize <= floatSamples.size) {
            val window = floatSamples.sliceArray(offset until offset + windowSize)
            val spectrum = computeMagnitudeSpectrum(window)

            val prev = previousSpectrum
            if (prev != null) {
                // Spectral flux: sum of positive differences
                var flux = 0.0
                for (i in spectrum.indices) {
                    val diff = spectrum[i] - prev[i]
                    if (diff > 0) flux += diff
                }

                // Adaptive threshold
                if (flux > ONSET_THRESHOLD) {
                    val timeMs = (sampleCount + offset) * 1000L / SAMPLE_RATE
                    // Debounce: minimum 50ms between onsets
                    if (onsetTimes.isEmpty() || timeMs - onsetTimes.last() > 50) {
                        onsetTimes.add(timeMs)
                    }
                }
            }
            previousSpectrum = spectrum
            offset += HOP_SIZE
        }
        sampleCount += floatSamples.size

        // Emit result when we have enough onsets
        if (onsetTimes.size >= 4) {
            emitResult()
        }
    }

    private suspend fun emitResult() {
        if (onsetTimes.size < 2) return

        val iois = mutableListOf<Long>()
        for (i in 1 until onsetTimes.size) {
            iois.add(onsetTimes[i] - onsetTimes[i - 1])
        }

        val averageIoi = iois.average()
        val deviations = iois.map { abs(it - averageIoi) }
        val avgDeviation = deviations.average()

        _rhythmResults.emit(
            RhythmResult(
                onsetTimesMs = onsetTimes.toList(),
                deviationsMs = deviations,
                averageDeviationMs = avgDeviation
            )
        )
    }

    private fun computeMagnitudeSpectrum(samples: DoubleArray): DoubleArray {
        val n = samples.size
        val spectrum = DoubleArray(n / 2)

        // Simple DFT (not FFT for simplicity — fine for small windows)
        for (k in 0 until n / 2) {
            var real = 0.0
            var imag = 0.0
            for (t in samples.indices) {
                val angle = 2 * Math.PI * k * t / n
                real += samples[t] * kotlin.math.cos(angle)
                imag -= samples[t] * kotlin.math.sin(angle)
            }
            spectrum[k] = sqrt(real * real + imag * imag)
        }
        return spectrum
    }
}
