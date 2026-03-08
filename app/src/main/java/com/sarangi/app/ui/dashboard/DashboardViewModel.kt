package com.sarangi.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarangi.core.database.SarangiRepository
import com.sarangi.core.database.entity.PracticeSession
import com.sarangi.core.database.entity.TeacherBriefingItem
import com.sarangi.core.database.entity.TechnicalObservation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val weeklySessionCount: Int = 0,
    val totalMinutesThisWeek: Int = 0,
    val currentStreak: Int = 0,
    val weekDays: List<Boolean> = List(7) { false },
    val recentSessions: List<PracticeSession> = emptyList(),
    val unresolvedObservations: List<TechnicalObservation> = emptyList(),
    val undiscussedBriefingCount: Int = 0,
    val briefingItems: List<TeacherBriefingItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: SarangiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            val profile = repository.getActiveProfileOnce() ?: return@launch
            val studentId = profile.id

            // Recent sessions
            val recentSessions = repository.getRecentSessionsOnce(studentId, 5)

            // This week's data
            val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            val weekSessions = repository.getSessionsInRangeOnce(studentId, weekAgo, now)
            val totalMinutes = weekSessions.sumOf { it.actualDurationMinutes ?: it.plannedDurationMinutes }

            // Week days (simplified - just check which days had sessions)
            val dayOfWeek = java.util.Calendar.getInstance()
            val weekDays = MutableList(7) { false }
            weekSessions.forEach { session ->
                dayOfWeek.timeInMillis = session.startedAt
                val day = (dayOfWeek.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0
                weekDays[day] = true
            }

            // Calculate streak
            var streak = 0
            val calendar = java.util.Calendar.getInstance()
            for (i in 0 until 30) {
                calendar.timeInMillis = System.currentTimeMillis() - i * 24 * 60 * 60 * 1000L
                val dayStart = calendar.clone() as java.util.Calendar
                dayStart.set(java.util.Calendar.HOUR_OF_DAY, 0)
                dayStart.set(java.util.Calendar.MINUTE, 0)
                val dayEnd = calendar.clone() as java.util.Calendar
                dayEnd.set(java.util.Calendar.HOUR_OF_DAY, 23)
                dayEnd.set(java.util.Calendar.MINUTE, 59)
                val daySessions = repository.getSessionsInRangeOnce(studentId, dayStart.timeInMillis, dayEnd.timeInMillis)
                if (daySessions.isNotEmpty()) streak++ else if (i > 0) break
            }

            // Observations
            val observations = repository.getUnresolvedObservationsOnce(studentId)

            // Briefing items
            val briefings = repository.getUndiscussedItemsOnce(studentId)

            _state.update {
                it.copy(
                    weeklySessionCount = weekSessions.size,
                    totalMinutesThisWeek = totalMinutes,
                    currentStreak = streak,
                    weekDays = weekDays,
                    recentSessions = recentSessions,
                    unresolvedObservations = observations,
                    undiscussedBriefingCount = briefings.size,
                    briefingItems = briefings,
                    isLoading = false
                )
            }
        }
    }

    fun resolveObservation(observation: TechnicalObservation) {
        viewModelScope.launch {
            repository.updateObservation(observation.copy(resolved = true, resolvedAt = System.currentTimeMillis()))
            loadDashboard()
        }
    }

    fun markAllBriefingsDiscussed() {
        viewModelScope.launch {
            val profile = repository.getActiveProfileOnce() ?: return@launch
            repository.markAllBriefingItemsDiscussed(profile.id)
            loadDashboard()
        }
    }

    fun refresh() = loadDashboard()
}
