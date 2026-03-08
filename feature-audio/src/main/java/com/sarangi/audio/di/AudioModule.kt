package com.sarangi.audio.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {
    // PitchDetector, RhythmAnalyser, ToneClassifier, NoiseCalibrationService,
    // and AudioAnalysisOrchestrator are all @Singleton @Inject constructor
    // so Hilt can provide them automatically
}
