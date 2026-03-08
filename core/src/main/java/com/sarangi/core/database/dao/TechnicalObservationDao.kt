package com.sarangi.core.database.dao

import androidx.room.*
import com.sarangi.core.database.entity.TechnicalObservation
import kotlinx.coroutines.flow.Flow

@Dao
interface TechnicalObservationDao {
    @Query("SELECT * FROM technical_observation WHERE studentId = :studentId AND resolved = 0 ORDER BY observedAt DESC")
    fun getUnresolvedObservations(studentId: Long): Flow<List<TechnicalObservation>>

    @Query("SELECT * FROM technical_observation WHERE studentId = :studentId AND resolved = 0 ORDER BY observedAt DESC")
    suspend fun getUnresolvedObservationsOnce(studentId: Long): List<TechnicalObservation>

    @Query("SELECT * FROM technical_observation WHERE studentId = :studentId AND category = :category ORDER BY observedAt DESC")
    fun getObservationsByCategory(studentId: Long, category: String): Flow<List<TechnicalObservation>>

    @Query("SELECT * FROM technical_observation WHERE studentId = :studentId ORDER BY observedAt DESC")
    fun getAllObservations(studentId: Long): Flow<List<TechnicalObservation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(observation: TechnicalObservation): Long

    @Update
    suspend fun update(observation: TechnicalObservation)

    @Delete
    suspend fun delete(observation: TechnicalObservation)
}
