package com.sarangi.core.database.dao

import androidx.room.*
import com.sarangi.core.database.entity.PracticeSession
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeSessionDao {
    @Query("SELECT * FROM practice_session WHERE studentId = :studentId ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentSessions(studentId: Long, limit: Int = 10): Flow<List<PracticeSession>>

    @Query("SELECT * FROM practice_session WHERE studentId = :studentId ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentSessionsOnce(studentId: Long, limit: Int = 10): List<PracticeSession>

    @Query("SELECT * FROM practice_session WHERE studentId = :studentId AND startedAt BETWEEN :startDate AND :endDate ORDER BY startedAt DESC")
    fun getSessionsInRange(studentId: Long, startDate: Long, endDate: Long): Flow<List<PracticeSession>>

    @Query("SELECT * FROM practice_session WHERE studentId = :studentId AND startedAt BETWEEN :startDate AND :endDate ORDER BY startedAt DESC")
    suspend fun getSessionsInRangeOnce(studentId: Long, startDate: Long, endDate: Long): List<PracticeSession>

    @Query("SELECT * FROM practice_session WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): PracticeSession?

    @Query("SELECT * FROM practice_session WHERE studentId = :studentId AND endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(studentId: Long): PracticeSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: PracticeSession): Long

    @Update
    suspend fun update(session: PracticeSession)

    @Delete
    suspend fun delete(session: PracticeSession)
}
