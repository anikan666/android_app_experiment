package com.sarangi.tracking.analytics

import com.sarangi.core.database.SarangiRepository
import com.sarangi.core.database.entity.TeacherBriefingItem
import com.sarangi.core.database.entity.TechnicalObservation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressionTracker @Inject constructor(
    private val repository: SarangiRepository
) {
    suspend fun analyzeAfterSession(studentId: Long, sessionId: Long) {
        val recentSessions = repository.getRecentSessionsOnce(studentId, 10)
        if (recentSessions.size < 3) return

        // Check for declining pitch accuracy
        val recentActivities = recentSessions.take(3).flatMap { session ->
            repository.getActivitiesForSessionOnce(session.id)
        }

        val pitchScores = recentActivities.mapNotNull { it.pitchAccuracyScore }
        if (pitchScores.size >= 3) {
            val recentAvg = pitchScores.take(pitchScores.size / 2).average()
            val olderAvg = pitchScores.drop(pitchScores.size / 2).average()
            if (recentAvg < olderAvg - 0.1) {
                repository.insertObservation(
                    TechnicalObservation(
                        studentId = studentId,
                        category = "intonation",
                        observation = "Pitch accuracy has declined over the last few sessions (${(recentAvg * 100).toInt()}% vs ${(olderAvg * 100).toInt()}% previously)",
                        severity = "mild",
                        source = "audio-analysis"
                    )
                )
            }
        }

        // Check for avoided activity types
        val activityTypes = recentActivities.map { it.activityType }
        val skippedTypes = listOf("scale", "exercise", "sight-reading").filter { type ->
            activityTypes.count { it == type } == 0
        }
        skippedTypes.forEach { type ->
            val lastPracticed = findLastPracticed(studentId, type)
            if (lastPracticed != null) {
                val daysSince = (System.currentTimeMillis() - lastPracticed) / (24 * 60 * 60 * 1000)
                if (daysSince > 7) {
                    repository.insertObservation(
                        TechnicalObservation(
                            studentId = studentId,
                            category = "practice-pattern",
                            observation = "${type.replaceFirstChar { it.uppercase() }} practice has been absent for $daysSince days",
                            severity = "info",
                            source = "audio-analysis"
                        )
                    )
                }
            }
        }

        // Check for consistent problems
        val existingObservations = repository.getUnresolvedObservationsOnce(studentId)
        val significantIssues = existingObservations.filter { it.severity == "significant" }
        significantIssues.forEach { obs ->
            val daysSinceObserved = (System.currentTimeMillis() - obs.observedAt) / (24 * 60 * 60 * 1000)
            if (daysSinceObserved > 14) {
                repository.insertBriefingItem(
                    TeacherBriefingItem(
                        studentId = studentId,
                        category = "technique-issue",
                        description = "Persistent issue (${daysSinceObserved} days): ${obs.observation}"
                    )
                )
            }
        }

        // Milestone: consistent practice
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        val thisWeekSessions = repository.getSessionsInRangeOnce(studentId, weekAgo, System.currentTimeMillis())
        if (thisWeekSessions.size >= 4) {
            // Check if they've maintained this for 4 weeks
            val fourWeeksAgo = System.currentTimeMillis() - 28L * 24 * 60 * 60 * 1000
            val monthSessions = repository.getSessionsInRangeOnce(studentId, fourWeeksAgo, System.currentTimeMillis())
            if (monthSessions.size >= 16) {
                repository.insertObservation(
                    TechnicalObservation(
                        studentId = studentId,
                        category = "milestone",
                        observation = "Maintained 4+ sessions/week for a month — excellent consistency",
                        severity = "info",
                        source = "audio-analysis"
                    )
                )
            }
        }
    }

    private suspend fun findLastPracticed(studentId: Long, activityType: String): Long? {
        val sessions = repository.getRecentSessionsOnce(studentId, 20)
        for (session in sessions) {
            val activities = repository.getActivitiesForSessionOnce(session.id)
            if (activities.any { it.activityType == activityType && it.completed }) {
                return session.startedAt
            }
        }
        return null
    }
}
