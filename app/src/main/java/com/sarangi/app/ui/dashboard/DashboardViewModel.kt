package com.sarangi.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarangi.core.database.SarangiRepository
import com.sarangi.core.database.entity.PracticeSession
import com.sarangi.core.database.entity.TeacherBriefingItem
import com.sarangi.core.database.entity.TechnicalObservation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeekDay(val dayOfWeek: Int, val practiced: Boolean, val isToday: Boolean)

data class DashboardState(
    val weekDays: List<WeekDay> = emptyList(),
    val sessionsThisWeek: Int = 0,
    val totalMinutesThisWeek: Int = 0,
    val streakDays: Int = 0,
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

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            val profile = repository.getActiveProfileOnce() ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val studentId = profile.id
            val now = System.currentTimeMillis()
            val weekAgo = now - 7 * 24 * 60 * 60 * 1000L

            val recentSessions = repository.getRecentSessionsOnce(studentId, 5)
            val weekSessions = repository.getSessionsInRangeOnce(studentId, weekAgo, now)
            val unresolvedObs = repository.getUnresolvedObservationsOnce(studentId)
            val briefingItems = repository.getUndiscussedItemsOnce(studentId)

            val totalMinutes = weekSessions.sumOf { it.actualDurationMinutes ?: it.plannedDurationMinutes }

            // Build week view
            val calendar = java.util.Calendar.getInstance()
            val todayDow = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            val sessionDays = weekSessions.map { session ->
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = session.startedAt
                cal.get(java.util.Calendar.DAY_OF_WEEK)
            }.toSet()

            val weekDays = (1..7).map { dow ->
                WeekDay(
                    dayOfWeek = dow,
                    practiced = dow in sessionDays,
                    isToday = dow == todayDow
                )
            }

            // Simple streak calculation
            var streak = 0
            val sortedSessions = weekSessions.sortedByDescending { it.startedAt }
            val cal = java.util.Calendar.getInstance()
            for (i in 0..6) {
                cal.timeInMillis = now - i * 24 * 60 * 60 * 1000L
                val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
                if (dow in sessionDays) streak++ else if (i > 0) break
            }

            _state.update {
                it.copy(
                    weekDays = weekDays,
                    sessionsThisWeek = weekSessions.size,
                    totalMinutesThisWeek = totalMinutes,
                    streakDays = streak,
                    recentSessions = recentSessions,
                    unresolvedObservations = unresolvedObs,
                    undiscussedBriefingCount = briefingItems.size,
                    briefingItems = briefingItems,
                    isLoading = false
                )
            }
        }
    }

    fun markObservationResolved(observation: TechnicalObservation) {
        viewModelScope.launch {
            repository.updateObservation(observation.copy(resolved = true, resolvedAt = System.currentTimeMillis()))
            loadDashboard()
        }
    }

    fun markAllBriefingDiscussed() {
        viewModelScope.launch {
            val profile = repository.getActiveProfileOnce() ?: return@launch
            repository.markAllBriefingItemsDiscussed(profile.id)
            loadDashboard()
        }
    }

    fun refresh() {
        _state.update { it.copy(isLoading = true) }
        loadDashboard()
    }
}
