package com.sarangi.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "practice_session",
    foreignKeys = [
        ForeignKey(
            entity = StudentProfile::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId")]
)
data class PracticeSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val startedAt: Long,
    val endedAt: Long? = null,
    val plannedDurationMinutes: Int,
    val actualDurationMinutes: Int? = null,
    val energyLevel: Int? = null,
    val mood: String? = null,
    val sessionPlanJson: String = "{}",
    val completionRate: Float? = null,
    val notes: String? = null,
    val userRating: Int? = null
)
