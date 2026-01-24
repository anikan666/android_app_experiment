package com.dailyplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val status: String, // TO-DO, FOLLOW-UP, FYI
    val dueDate: Long?,
    val poc: String?,
    val originalEmailId: String?,
    val priority: String = "LOW",
    val isScheduled: Boolean = false,
    val scheduledStartTime: Long? = null,
    val scheduledEndTime: Long? = null
)
