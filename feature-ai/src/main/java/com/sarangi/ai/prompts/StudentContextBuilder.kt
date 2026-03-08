package com.sarangi.ai.prompts

import com.sarangi.core.database.SarangiRepository
import com.sarangi.core.database.entity.PracticeSession
import com.sarangi.core.database.entity.StudentProfile
import com.sarangi.core.database.entity.TechnicalObservation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentContextBuilder @Inject constructor(
    private val repository: SarangiRepository
) {
    suspend fun buildContext(): String {
        val profile = repository.getActiveProfileOnce() ?: return "No student profile found."
        val recentSessions = repository.getRecentSessionsOnce(profile.id, 5)
        val unresolvedObservations = repository.getUnresolvedObservationsOnce(profile.id)

        return buildString {
            appendLine("=== STUDENT PROFILE ===")
            appendLine("Name: ${profile.name}")
            appendLine("Level: ${profile.currentLevel}")
            appendLine("Hand size: ${profile.handSize ?: "not specified"}")
            profile.physicalConstraints?.let { appendLine("Physical constraints: $it") }
            profile.musicalBackground?.let { appendLine("Musical background: $it") }
            appendLine("Weekly practice goal: ${profile.weeklyPracticeGoalDays} days")
            appendLine("Preferred session length: ${profile.sessionDurationMinutesPref} minutes")
            profile.learningStyle?.let { appendLine("Learning style notes: $it") }

            if (recentSessions.isNotEmpty()) {
                appendLine()
                appendLine("=== RECENT SESSIONS (last ${recentSessions.size}) ===")
                recentSessions.forEach { session ->
                    appendLine("- ${formatSession(session)}")
                }
                val thisWeekCount = countSessionsThisWeek(recentSessions)
                val avgDuration = recentSessions.mapNotNull { it.actualDurationMinutes }.average().takeIf { !it.isNaN() }
                appendLine("Sessions this week: $thisWeekCount")
                avgDuration?.let { appendLine("Average session duration: ${it.toInt()} minutes") }
            }

            if (unresolvedObservations.isNotEmpty()) {
                appendLine()
                appendLine("=== UNRESOLVED TECHNICAL OBSERVATIONS ===")
                unresolvedObservations.forEach { obs ->
                    appendLine("- [${obs.category}/${obs.severity}] ${obs.observation}")
                }
            }
        }
    }

    private fun formatSession(session: PracticeSession): String {
        val duration = session.actualDurationMinutes ?: session.plannedDurationMinutes
        val completion = session.completionRate?.let { "${(it * 100).toInt()}% completed" } ?: "in progress"
        return "${duration}min, $completion${session.notes?.let { ", notes: $it" } ?: ""}"
    }

    private fun countSessionsThisWeek(sessions: List<PracticeSession>): Int {
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        return sessions.count { it.startedAt > weekAgo }
    }
}
