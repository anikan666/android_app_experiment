package com.sarangi.tracking.milestones

import com.sarangi.core.database.SarangiRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MilestoneTracker @Inject constructor(
    private val repository: SarangiRepository
) {
    suspend fun checkMilestones(studentId: Long): List<String> {
        val milestones = mutableListOf<String>()
        val allSessions = repository.getRecentSessionsOnce(studentId, 100)

        // First session
        if (allSessions.size == 1) {
            milestones.add("First practice session completed!")
        }

        // Session count milestones
        val completedSessions = allSessions.filter { it.endedAt != null }
        when (completedSessions.size) {
            10 -> milestones.add("10 practice sessions completed")
            25 -> milestones.add("25 practice sessions completed")
            50 -> milestones.add("50 practice sessions completed")
            100 -> milestones.add("100 practice sessions completed — remarkable dedication!")
        }

        // Total practice time
        val totalMinutes = completedSessions.sumOf { it.actualDurationMinutes ?: 0 }
        when {
            totalMinutes >= 600 && totalMinutes < 610 -> milestones.add("10 hours of practice logged")
            totalMinutes >= 1500 && totalMinutes < 1510 -> milestones.add("25 hours of practice logged")
            totalMinutes >= 3000 && totalMinutes < 3010 -> milestones.add("50 hours of practice logged")
        }

        return milestones
    }
}
