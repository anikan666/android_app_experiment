package com.sarangi.tracking.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object TrackingModule {
    // ProgressionTracker, TeacherBriefingGenerator, and MilestoneTracker
    // are all @Singleton @Inject constructor so Hilt provides them automatically
}
