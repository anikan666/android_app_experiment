package com.sarangi.ai.di

import com.sarangi.ai.client.ApiKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {
    // ApiKeyProvider will be provided by the app module which has access to BuildConfig
}
