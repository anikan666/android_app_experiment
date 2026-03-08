package com.sarangi.audio.noise

import com.sarangi.core.model.NoiseLevel
import com.sarangi.core.model.NoiseProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt

@Singleton
class NoiseCalibrationService @Inject constructor() {

    fun calibrate(samples: List<ShortArray>): NoiseProfile {
        if (samples.isEmpty()) {
            return NoiseProfile(
                rmsNoiseFloor = 0.01,
                estimatedDbSpl = 30.0,
                noiseLevel = NoiseLevel.LOW
            )
        }

        val allSamples = samples.flatMap { it.toList() }
        val floatSamples = allSamples.map { it.toDouble() / Short.MAX_VALUE }

        val rms = calculateRms(floatSamples.toDoubleArray())
        val dbSpl = estimateDbSpl(rms)

        val level = when {
            dbSpl < 40 -> NoiseLevel.LOW
            dbSpl < 55 -> NoiseLevel.MODERATE
            else -> NoiseLevel.HIGH
        }

        return NoiseProfile(
            rmsNoiseFloor = rms,
            estimatedDbSpl = dbSpl,
            noiseLevel = level
        )
    }

    private fun calculateRms(data: DoubleArray): Double {
        var sum = 0.0
        for (sample in data) sum += sample * sample
        return sqrt(sum / data.size)
    }

    private fun estimateDbSpl(rms: Double): Double {
        if (rms <= 0) return 0.0
        // Rough estimate: phone mic reference ~94 dB SPL at full scale
        return 94.0 + 20 * log10(rms)
    }
}
