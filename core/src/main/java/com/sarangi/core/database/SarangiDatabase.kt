package com.sarangi.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sarangi.core.database.dao.*
import com.sarangi.core.database.entity.*

@Database(
    entities = [
        StudentProfile::class,
        PracticeSession::class,
        SessionActivity::class,
        TechnicalObservation::class,
        TeacherBriefingItem::class,
        ConversationMessage::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SarangiDatabase : RoomDatabase() {
    abstract fun studentProfileDao(): StudentProfileDao
    abstract fun practiceSessionDao(): PracticeSessionDao
    abstract fun sessionActivityDao(): SessionActivityDao
    abstract fun technicalObservationDao(): TechnicalObservationDao
    abstract fun teacherBriefingItemDao(): TeacherBriefingItemDao
    abstract fun conversationMessageDao(): ConversationMessageDao
}
