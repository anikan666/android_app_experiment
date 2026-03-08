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
        val allSamples = samples.flatMap { it.toList() }
        if (allSamples.isEmpty()) {
            return NoiseProfile()
        }

        val floatSamples = allSamples.map { it.toDouble() / Short.MAX_VALUE }
        val rms = calculateRms(floatSamples)
        val dbSpl = estimateDbSpl(rms)
        val noiseLevel = when {
            dbSpl < 40.0 -> NoiseLevel.LOW
            dbSpl < 55.0 -> NoiseLevel.MODERATE
            else -> NoiseLevel.HIGH
        }

        return NoiseProfile(
            rmsNoiseFloor = rms,
            estimatedDbSpl = dbSpl,
            noiseLevel = noiseLevel
        )
    }

    private fun calculateRms(samples: List<Double>): Double {
        var sum = 0.0
        for (s in samples) sum += s * s
        return sqrt(sum / samples.size)
    }

    private fun estimateDbSpl(rms: Double): Double {
        // Rough estimation — actual SPL depends on mic sensitivity
        // Using a reference that maps typical phone mic RMS to approximate dB SPL
        val dbFs = 20 * log10(rms.coerceAtLeast(0.00001))
        // Phone mic typically has ~-26 dBFS sensitivity at 94 dB SPL
        return (dbFs + 94 + 26).coerceIn(20.0, 100.0)
    }
}
