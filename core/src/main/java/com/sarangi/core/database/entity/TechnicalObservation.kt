package com.sarangi.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "technical_observation",
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
data class TechnicalObservation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val observedAt: Long = System.currentTimeMillis(),
    val category: String,
    val observation: String,
    val severity: String = "info",
    val resolved: Boolean = false,
    val resolvedAt: Long? = null,
    val source: String = "conversational-diagnostic"
)
