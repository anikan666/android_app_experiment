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
            val trend = pitchScores.takeLast(3)
            if (trend.last() < trend.first() - 0.1f) {
                repository.insertObservation(
                    TechnicalObservation(
                        studentId = studentId,
                        category = "intonation",
                        observation = "Pitch accuracy has declined over the last few sessions",
                        severity = "mild",
                        source = "audio-analysis"
                    )
                )
            }
        }

        // Check for avoided activity types
        val activityTypes = recentActivities.map { it.activityType }
        val skippedTypes = recentActivities.filter { it.skipped }.map { it.activityType }
        val consistentlySkipped = skippedTypes.groupBy { it }
            .filter { it.value.size >= 3 }
            .keys

        consistentlySkipped.forEach { type ->
            repository.insertBriefingItem(
                TeacherBriefingItem(
                    studentId = studentId,
                    category = "progress-note",
                    description = "Student has been consistently skipping $type activities"
                )
            )
        }

        // Check practice frequency milestones
        val now = System.currentTimeMillis()
        val monthAgo = now - 30L * 24 * 60 * 60 * 1000
        val monthSessions = repository.getSessionsInRangeOnce(studentId, monthAgo, now)
        val weeksWithFourPlus = monthSessions
            .groupBy { (it.startedAt - monthAgo) / (7 * 24 * 60 * 60 * 1000) }
            .count { it.value.size >= 4 }

        if (weeksWithFourPlus >= 4) {
            repository.insertObservation(
                TechnicalObservation(
                    studentId = studentId,
                    category = "milestone",
                    observation = "Maintained 4+ sessions/week for a month",
                    severity = "info",
                    source = "self-reported"
                )
            )
        }
    }
}
