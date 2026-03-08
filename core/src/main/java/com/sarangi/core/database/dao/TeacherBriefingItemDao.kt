package com.sarangi.core.database.dao

import androidx.room.*
import com.sarangi.core.database.entity.TeacherBriefingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherBriefingItemDao {
    @Query("SELECT * FROM teacher_briefing_item WHERE studentId = :studentId AND discussed = 0 ORDER BY createdAt DESC")
    fun getUndiscussedItems(studentId: Long): Flow<List<TeacherBriefingItem>>

    @Query("SELECT * FROM teacher_briefing_item WHERE studentId = :studentId AND discussed = 0 ORDER BY createdAt DESC")
    suspend fun getUndiscussedItemsOnce(studentId: Long): List<TeacherBriefingItem>

    @Query("SELECT * FROM teacher_briefing_item WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun getAllItems(studentId: Long): Flow<List<TeacherBriefingItem>>

    @Query("UPDATE teacher_briefing_item SET discussed = 1 WHERE studentId = :studentId AND discussed = 0")
    suspend fun markAllDiscussed(studentId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TeacherBriefingItem): Long

    @Update
    suspend fun update(item: TeacherBriefingItem)

    @Delete
    suspend fun delete(item: TeacherBriefingItem)
}
