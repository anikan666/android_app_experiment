package com.sarangi.core.database.dao

import androidx.room.*
import com.sarangi.core.database.entity.ConversationMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationMessageDao {
    @Query("SELECT * FROM conversation_message WHERE studentId = :studentId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(studentId: Long, limit: Int = 50): Flow<List<ConversationMessage>>

    @Query("SELECT * FROM conversation_message WHERE studentId = :studentId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesOnce(studentId: Long, limit: Int = 50): List<ConversationMessage>

    @Query("SELECT * FROM conversation_message WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ConversationMessage>>

    @Query("SELECT * FROM conversation_message WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionOnce(sessionId: Long): List<ConversationMessage>

    @Query("SELECT * FROM conversation_message WHERE studentId = :studentId AND sessionId IS NULL ORDER BY timestamp DESC LIMIT :limit")
    fun getGeneralMessages(studentId: Long, limit: Int = 50): Flow<List<ConversationMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ConversationMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ConversationMessage>)

    @Delete
    suspend fun delete(message: ConversationMessage)

    @Query("DELETE FROM conversation_message WHERE studentId = :studentId")
    suspend fun deleteAllForStudent(studentId: Long)
}
