package com.sarangi.core.model

data class NoiseProfile(
    val rmsNoiseFloor: Double = 0.0,
    val estimatedDbSpl: Double = 0.0,
    val noiseLevel: NoiseLevel = NoiseLevel.LOW,
    val calibratedAt: Long = System.currentTimeMillis()
)

enum class NoiseLevel {
    LOW,      // < 40 dB SPL — full feedback
    MODERATE, // 40-55 dB — pitch may be unreliable
    HIGH      // > 55 dB — rhythm and tempo only
}
