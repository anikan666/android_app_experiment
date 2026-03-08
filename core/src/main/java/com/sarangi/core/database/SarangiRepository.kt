package com.sarangi.core.database

import com.sarangi.core.database.dao.*
import com.sarangi.core.database.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SarangiRepository @Inject constructor(
    private val studentProfileDao: StudentProfileDao,
    private val practiceSessionDao: PracticeSessionDao,
    private val sessionActivityDao: SessionActivityDao,
    private val technicalObservationDao: TechnicalObservationDao,
    private val teacherBriefingItemDao: TeacherBriefingItemDao,
    private val conversationMessageDao: ConversationMessageDao
) {
    // Student Profile
    fun getActiveProfile(): Flow<StudentProfile?> = studentProfileDao.getActiveProfile()
    suspend fun getActiveProfileOnce(): StudentProfile? = studentProfileDao.getActiveProfileOnce()
    suspend fun insertProfile(profile: StudentProfile): Long = studentProfileDao.insert(profile)
    suspend fun updateProfile(profile: StudentProfile) = studentProfileDao.update(profile)

    // Practice Sessions
    fun getRecentSessions(studentId: Long, limit: Int = 10): Flow<List<PracticeSession>> =
        practiceSessionDao.getRecentSessions(studentId, limit)
    suspend fun getRecentSessionsOnce(studentId: Long, limit: Int = 10): List<PracticeSession> =
        practiceSessionDao.getRecentSessionsOnce(studentId, limit)
    fun getSessionsInRange(studentId: Long, startDate: Long, endDate: Long): Flow<List<PracticeSession>> =
        practiceSessionDao.getSessionsInRange(studentId, startDate, endDate)
    suspend fun getSessionsInRangeOnce(studentId: Long, startDate: Long, endDate: Long): List<PracticeSession> =
        practiceSessionDao.getSessionsInRangeOnce(studentId, startDate, endDate)
    suspend fun getSessionById(sessionId: Long): PracticeSession? = practiceSessionDao.getSessionById(sessionId)
    suspend fun getActiveSession(studentId: Long): PracticeSession? = practiceSessionDao.getActiveSession(studentId)
    suspend fun insertSession(session: PracticeSession): Long = practiceSessionDao.insert(session)
    suspend fun updateSession(session: PracticeSession) = practiceSessionDao.update(session)

    // Session Activities
    fun getActivitiesForSession(sessionId: Long): Flow<List<SessionActivity>> =
        sessionActivityDao.getActivitiesForSession(sessionId)
    suspend fun getActivitiesForSessionOnce(sessionId: Long): List<SessionActivity> =
        sessionActivityDao.getActivitiesForSessionOnce(sessionId)
    suspend fun insertActivity(activity: SessionActivity): Long = sessionActivityDao.insert(activity)
    suspend fun insertActivities(activities: List<SessionActivity>) = sessionActivityDao.insertAll(activities)
    suspend fun updateActivity(activity: SessionActivity) = sessionActivityDao.update(activity)

    // Technical Observations
    fun getUnresolvedObservations(studentId: Long): Flow<List<TechnicalObservation>> =
        technicalObservationDao.getUnresolvedObservations(studentId)
    suspend fun getUnresolvedObservationsOnce(studentId: Long): List<TechnicalObservation> =
        technicalObservationDao.getUnresolvedObservationsOnce(studentId)
    fun getObservationsByCategory(studentId: Long, category: String): Flow<List<TechnicalObservation>> =
        technicalObservationDao.getObservationsByCategory(studentId, category)
    fun getAllObservations(studentId: Long): Flow<List<TechnicalObservation>> =
        technicalObservationDao.getAllObservations(studentId)
    suspend fun insertObservation(observation: TechnicalObservation): Long = technicalObservationDao.insert(observation)
    suspend fun updateObservation(observation: TechnicalObservation) = technicalObservationDao.update(observation)

    // Teacher Briefing
    fun getUndiscussedItems(studentId: Long): Flow<List<TeacherBriefingItem>> =
        teacherBriefingItemDao.getUndiscussedItems(studentId)
    suspend fun getUndiscussedItemsOnce(studentId: Long): List<TeacherBriefingItem> =
        teacherBriefingItemDao.getUndiscussedItemsOnce(studentId)
    fun getAllBriefingItems(studentId: Long): Flow<List<TeacherBriefingItem>> =
        teacherBriefingItemDao.getAllItems(studentId)
    suspend fun markAllBriefingItemsDiscussed(studentId: Long) = teacherBriefingItemDao.markAllDiscussed(studentId)
    suspend fun insertBriefingItem(item: TeacherBriefingItem): Long = teacherBriefingItemDao.insert(item)
    suspend fun updateBriefingItem(item: TeacherBriefingItem) = teacherBriefingItemDao.update(item)

    // Conversation Messages
    fun getRecentMessages(studentId: Long, limit: Int = 50): Flow<List<ConversationMessage>> =
        conversationMessageDao.getRecentMessages(studentId, limit)
    suspend fun getRecentMessagesOnce(studentId: Long, limit: Int = 50): List<ConversationMessage> =
        conversationMessageDao.getRecentMessagesOnce(studentId, limit)
    fun getMessagesForSession(sessionId: Long): Flow<List<ConversationMessage>> =
        conversationMessageDao.getMessagesForSession(sessionId)
    suspend fun getMessagesForSessionOnce(sessionId: Long): List<ConversationMessage> =
        conversationMessageDao.getMessagesForSessionOnce(sessionId)
    fun getGeneralMessages(studentId: Long, limit: Int = 50): Flow<List<ConversationMessage>> =
        conversationMessageDao.getGeneralMessages(studentId, limit)
    suspend fun insertMessage(message: ConversationMessage): Long = conversationMessageDao.insert(message)
    suspend fun deleteAllMessages(studentId: Long) = conversationMessageDao.deleteAllForStudent(studentId)
}
