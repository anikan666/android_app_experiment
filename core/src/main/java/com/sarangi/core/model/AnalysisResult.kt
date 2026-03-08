package com.sarangi.core.model

data class PitchResult(
    val frequency: Double,
    val noteName: String,
    val centsDeviation: Double,
    val confidence: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class RhythmResult(
    val onsetTimesMs: List<Long>,
    val deviationsMs: List<Double>,
    val averageDeviationMs: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class ToneResult(
    val classification: ToneClassification,
    val attackTimeMs: Double,
    val sustainStability: Double,
    val spectralCentroid: Double,
    val confidence: Double,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ToneClassification {
    CLEAN, SLIGHTLY_SCRATCHY, SCRATCHY, WHISTLE_TONE, UNCERTAIN
}

data class TempoResult(
    val averageBpm: Double,
    val tempoDrift: Double,
    val consistencyScore: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class AnalysisFrame(
    val pitch: PitchResult? = null,
    val rhythm: RhythmResult? = null,
    val tone: ToneResult? = null,
    val tempo: TempoResult? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActivitySummary(
    val averagePitchAccuracy: Float? = null,
    val rhythmScore: Float? = null,
    val toneQualityDistribution: Map<ToneClassification, Int> = emptyMap(),
    val tempoConsistency: Float? = null
)
