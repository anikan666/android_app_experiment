package com.sarangi.app.di

import com.sarangi.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @Named("anthropic_api_key")
    fun provideAnthropicApiKey(): String {
        return BuildConfig.ANTHROPIC_API_KEY
    }
}
