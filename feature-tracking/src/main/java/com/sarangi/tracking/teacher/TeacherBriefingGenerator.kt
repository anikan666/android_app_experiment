package com.sarangi.tracking.teacher

import com.sarangi.core.database.SarangiRepository
import com.sarangi.core.database.entity.TeacherBriefingItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeacherBriefingGenerator @Inject constructor(
    private val repository: SarangiRepository
) {
    suspend fun generateBriefingText(studentId: Long): String {
        val items = repository.getUndiscussedItemsOnce(studentId)
        if (items.isEmpty()) return "No new items for your next lesson."

        val grouped = items.groupBy { it.category }

        return buildString {
            appendLine("=== Teacher Briefing ===")
            appendLine()

            grouped["technique-issue"]?.let { issues ->
                appendLine("Technique Issues:")
                issues.forEach { appendLine("  - ${it.description}") }
                appendLine()
            }

            grouped["question"]?.let { questions ->
                appendLine("Student Questions:")
                questions.forEach { appendLine("  - ${it.description}") }
                appendLine()
            }

            grouped["progress-note"]?.let { notes ->
                appendLine("Progress Notes:")
                notes.forEach { appendLine("  - ${it.description}") }
                appendLine()
            }

            val profile = repository.getActiveProfileOnce()
            if (profile != null) {
                val recentSessions = repository.getRecentSessionsOnce(profile.id, 5)
                val avgDuration = recentSessions.mapNotNull { it.actualDurationMinutes }.average()
                appendLine("Recent Practice: ${recentSessions.size} sessions, avg ${avgDuration.toInt()} min")
            }
        }
    }
}
