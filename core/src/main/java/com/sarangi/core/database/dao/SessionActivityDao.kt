package com.sarangi.core.database.dao

import androidx.room.*
import com.sarangi.core.database.entity.SessionActivity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionActivityDao {
    @Query("SELECT * FROM session_activity WHERE sessionId = :sessionId ORDER BY orderIndex")
    fun getActivitiesForSession(sessionId: Long): Flow<List<SessionActivity>>

    @Query("SELECT * FROM session_activity WHERE sessionId = :sessionId ORDER BY orderIndex")
    suspend fun getActivitiesForSessionOnce(sessionId: Long): List<SessionActivity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: SessionActivity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<SessionActivity>)

    @Update
    suspend fun update(activity: SessionActivity)

    @Delete
    suspend fun delete(activity: SessionActivity)
}
