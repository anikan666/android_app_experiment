package com.dailyplanner.di

import com.dailyplanner.data.remote.CalendarService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CalendarModule {

    @Provides
    @Singleton
    fun provideCalendarService(): CalendarService {
        return CalendarService()
    }
}
