package com.sarangi.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_activity",
    foreignKeys = [
        ForeignKey(
            entity = PracticeSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class SessionActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val orderIndex: Int,
    val activityType: String,
    val description: String,
    val plannedDurationMinutes: Int,
    val actualDurationMinutes: Int? = null,
    val completed: Boolean = false,
    val skipped: Boolean = false,
    val pitchAccuracyScore: Float? = null,
    val rhythmAccuracyScore: Float? = null,
    val toneQualityScore: Float? = null,
    val tempoConsistencyScore: Float? = null,
    val aiNotes: String? = null,
    val rationale: String? = null
)
