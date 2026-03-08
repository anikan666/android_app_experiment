package com.sarangi.core.database.dao

import androidx.room.*
import com.sarangi.core.database.entity.StudentProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentProfileDao {
    @Query("SELECT * FROM student_profile ORDER BY createdAt DESC LIMIT 1")
    fun getActiveProfile(): Flow<StudentProfile?>

    @Query("SELECT * FROM student_profile ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveProfileOnce(): StudentProfile?

    @Query("SELECT * FROM student_profile WHERE id = :id")
    suspend fun getById(id: Long): StudentProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: StudentProfile): Long

    @Update
    suspend fun update(profile: StudentProfile)

    @Delete
    suspend fun delete(profile: StudentProfile)
}
