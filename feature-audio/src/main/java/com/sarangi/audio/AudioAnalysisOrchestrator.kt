package com.sarangi.audio

import com.sarangi.audio.capture.AudioCaptureService
import com.sarangi.audio.pitch.PitchDetector
import com.sarangi.audio.rhythm.RhythmAnalyser
import com.sarangi.audio.tone.ToneClassifier
import com.sarangi.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioAnalysisOrchestrator @Inject constructor(
    private val audioCaptureService: AudioCaptureService,
    private val pitchDetector: PitchDetector,
    private val rhythmAnalyser: RhythmAnalyser,
    private val toneClassifier: ToneClassifier
) {
    private val _analysisFrames = MutableSharedFlow<AnalysisFrame>(replay = 0, extraBufferCapacity = 10)
    val analysisFrames: SharedFlow<AnalysisFrame> = _analysisFrames.asSharedFlow()

    private var analysisJob: Job? = null
    private val analysisScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Accumulated results for summary
    private val pitchResults = mutableListOf<PitchResult>()
    private val rhythmResults = mutableListOf<RhythmResult>()
    private val toneResults = mutableListOf<ToneResult>()

    fun start(noiseProfile: NoiseProfile? = null) {
        pitchResults.clear()
        rhythmResults.clear()
        toneResults.clear()
        rhythmAnalyser.reset()

        noiseProfile?.let {
            pitchDetector.setNoiseFloor(it.rmsNoiseFloor)
            toneClassifier.setNoisyEnvironment(it.noiseLevel == NoiseLevel.HIGH)
        }

        analysisJob = analysisScope.launch {
            // Collect pitch results
            launch {
                pitchDetector.pitchResults.collect { result ->
                    pitchResults.add(result)
                    _analysisFrames.emit(AnalysisFrame(pitch = result))
                }
            }

            // Collect rhythm results
            launch {
                rhythmAnalyser.rhythmResults.collect { result ->
                    rhythmResults.add(result)
                    _analysisFrames.emit(AnalysisFrame(rhythm = result))
                }
            }

            // Collect tone results
            launch {
                toneClassifier.toneResults.collect { result ->
                    toneResults.add(result)
                    _analysisFrames.emit(AnalysisFrame(tone = result))
                }
            }

            // Process audio frames
            audioCaptureService.audioFrames.collect { samples ->
                launch { pitchDetector.processFrame(samples) }
                launch { rhythmAnalyser.processFrame(samples) }
                launch { toneClassifier.processFrame(samples) }
            }
        }
    }

    fun stop() {
        analysisJob?.cancel()
        analysisJob = null
        audioCaptureService.stopRecording()
    }

    fun getActivitySummary(): ActivitySummary {
        val avgPitchAccuracy = if (pitchResults.isNotEmpty()) {
            val inTuneCount = pitchResults.count { kotlin.math.abs(it.centsDeviation) < 15 }
            inTuneCount.toFloat() / pitchResults.size
        } else null

        val rhythmScore = if (rhythmResults.isNotEmpty()) {
            val avgDeviation = rhythmResults.map { it.averageDeviationMs }.average()
            // Convert deviation to score: 0ms = 1.0, 100ms+ = 0.0
            (1.0 - (avgDeviation / 100.0)).coerceIn(0.0, 1.0).toFloat()
        } else null

        val toneDistribution = toneResults
            .groupBy { it.classification }
            .mapValues { it.value.size }

        return ActivitySummary(
            averagePitchAccuracy = avgPitchAccuracy,
            rhythmScore = rhythmScore,
            toneQualityDistribution = toneDistribution
        )
    }
}
