package com.sarangi.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_profile")
data class StudentProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val handSize: String? = null,
    val physicalConstraints: String? = null,
    val musicalBackground: String? = null,
    val currentLevel: String = "beginner",
    val weeklyPracticeGoalDays: Int = 4,
    val sessionDurationMinutesPref: Int = 30,
    val learningStyle: String? = null,
    val musicalPreferences: String? = null,
    val hasTeacher: Boolean = false,
    val teacherFrequency: String? = null,
    val preferredPracticeTime: String? = null
)
