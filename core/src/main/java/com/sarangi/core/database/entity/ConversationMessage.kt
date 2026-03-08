package com.sarangi.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation_message",
    foreignKeys = [
        ForeignKey(
            entity = StudentProfile::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId"), Index("sessionId")]
)
data class ConversationMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val sessionId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val role: String,
    val content: String,
    val messageType: String = "general"
)
