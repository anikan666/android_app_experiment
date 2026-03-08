package com.sarangi.app.di

import com.sarangi.ai.client.ApiKeyProvider
import com.sarangi.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiKeyProvider(): ApiKeyProvider {
        return object : ApiKeyProvider {
            override fun getApiKey(): String = BuildConfig.ANTHROPIC_API_KEY
        }
    }
}
