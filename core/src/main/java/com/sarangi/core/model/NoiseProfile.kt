package com.sarangi.core.model

data class NoiseProfile(
    val rmsNoiseFloor: Double,
    val estimatedDbSpl: Double,
    val noiseLevel: NoiseLevel
)

enum class NoiseLevel {
    LOW,      // < 40 dB SPL: full feedback
    MODERATE, // 40-55 dB: pitch may be unreliable
    HIGH      // > 55 dB: rhythm and tempo only
}
