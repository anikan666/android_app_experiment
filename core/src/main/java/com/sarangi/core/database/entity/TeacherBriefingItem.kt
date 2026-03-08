package com.sarangi.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "teacher_briefing_item",
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
data class TeacherBriefingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val category: String,
    val description: String,
    val audioClipPath: String? = null,
    val discussed: Boolean = false
)
