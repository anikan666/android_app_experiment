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
    private val pitchDetector: PitchDetector,
    private val rhythmAnalyser: RhythmAnalyser,
    private val toneClassifier: ToneClassifier
) {
    private val _analysisFrames = MutableSharedFlow<AnalysisFrame>(extraBufferCapacity = 10)
    val analysisFrames: Flow<AnalysisFrame> = _analysisFrames.asSharedFlow()

    private var orchestratorJob: Job? = null
    private var noiseProfile: NoiseProfile = NoiseProfile(0.01, 30.0, NoiseLevel.LOW)

    fun setNoiseProfile(profile: NoiseProfile) {
        noiseProfile = profile
    }

    fun start(scope: CoroutineScope) {
        orchestratorJob?.cancel()
        rhythmAnalyser.reset()

        orchestratorJob = scope.launch {
            AudioCaptureService.audioFrames.collect { frame ->
                launch(Dispatchers.Default) {
                    // Run analysers in parallel
                    val pitchDeferred = async { processPitch(frame) }
                    val rhythmDeferred = async { processRhythm(frame) }
                    val toneDeferred = async { processTone(frame) }

                    val pitch = pitchDeferred.await()
                    rhythmDeferred.await()
                    val tone = toneDeferred.await()

                    if (pitch != null || tone != null) {
                        _analysisFrames.emit(
                            AnalysisFrame(pitch = pitch, tone = tone)
                        )
                    }
                }
            }
        }
    }

    fun stop() {
        orchestratorJob?.cancel()
        orchestratorJob = null
    }

    suspend fun getActivitySummary(): ActivitySummary {
        val rhythm = rhythmAnalyser.getResult()
        return ActivitySummary(
            rhythmScore = rhythm?.let { (1.0 - (it.averageDeviationMs / 100.0).coerceIn(0.0, 1.0)).toFloat() }
        )
    }

    private suspend fun processPitch(frame: ShortArray): PitchResult? {
        pitchDetector.processFrame(frame, noiseProfile.rmsNoiseFloor)
        return null // Results come through the flow
    }

    private suspend fun processRhythm(frame: ShortArray) {
        rhythmAnalyser.processFrame(frame)
    }

    private suspend fun processTone(frame: ShortArray): ToneResult? {
        toneClassifier.processFrame(frame, noiseProfile.noiseLevel)
        return null // Results come through the flow
    }
}
