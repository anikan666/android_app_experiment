package com.sarangi.core.database

import android.content.Context
import androidx.room.Room
import com.sarangi.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SarangiDatabase {
        return Room.databaseBuilder(
            context,
            SarangiDatabase::class.java,
            "sarangi_database"
        ).build()
    }

    @Provides
    fun provideStudentProfileDao(db: SarangiDatabase): StudentProfileDao = db.studentProfileDao()

    @Provides
    fun providePracticeSessionDao(db: SarangiDatabase): PracticeSessionDao = db.practiceSessionDao()

    @Provides
    fun provideSessionActivityDao(db: SarangiDatabase): SessionActivityDao = db.sessionActivityDao()

    @Provides
    fun provideTechnicalObservationDao(db: SarangiDatabase): TechnicalObservationDao = db.technicalObservationDao()

    @Provides
    fun provideTeacherBriefingItemDao(db: SarangiDatabase): TeacherBriefingItemDao = db.teacherBriefingItemDao()

    @Provides
    fun provideConversationMessageDao(db: SarangiDatabase): ConversationMessageDao = db.conversationMessageDao()
}
