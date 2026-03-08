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
        if (items.isEmpty()) return "No items to discuss at your next lesson."

        val grouped = items.groupBy { it.category }

        return buildString {
            appendLine("=== TEACHER BRIEFING ===")
            appendLine()

            grouped["technique-issue"]?.let { issues ->
                appendLine("TECHNIQUE ISSUES:")
                issues.forEach { item ->
                    appendLine("  - ${item.description}")
                }
                appendLine()
            }

            grouped["question"]?.let { questions ->
                appendLine("QUESTIONS FOR TEACHER:")
                questions.forEach { item ->
                    appendLine("  - ${item.description}")
                }
                appendLine()
            }

            grouped["progress-note"]?.let { notes ->
                appendLine("PROGRESS NOTES:")
                notes.forEach { item ->
                    appendLine("  - ${item.description}")
                }
                appendLine()
            }

            val recentSessions = repository.getRecentSessionsOnce(studentId, 5)
            if (recentSessions.isNotEmpty()) {
                appendLine("RECENT PRACTICE SUMMARY:")
                appendLine("  Sessions this period: ${recentSessions.size}")
                val avgDuration = recentSessions.mapNotNull { it.actualDurationMinutes }.average()
                if (!avgDuration.isNaN()) {
                    appendLine("  Average session duration: ${avgDuration.toInt()} minutes")
                }
                val avgCompletion = recentSessions.mapNotNull { it.completionRate }.average()
                if (!avgCompletion.isNaN()) {
                    appendLine("  Average completion rate: ${(avgCompletion * 100).toInt()}%")
                }
            }
        }
    }
}
