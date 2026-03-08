package com.sarangi.ai.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AiModule
// The @Named("anthropic_api_key") String is provided by the :app module's AppModule.
// All other AI classes use @Inject constructor and are auto-discovered by Hilt.
