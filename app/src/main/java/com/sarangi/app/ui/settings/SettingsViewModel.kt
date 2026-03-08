package com.sarangi.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarangi.core.database.SarangiRepository
import com.sarangi.core.database.entity.StudentProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val profile: StudentProfile? = null,
    val notificationsEnabled: Boolean = true,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 7,
    val maxNudgesPerDay: Int = 1,
    val isLoading: Boolean = true,
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SarangiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = repository.getActiveProfileOnce()
            _state.update { it.copy(profile = profile, isLoading = false) }
        }
    }

    fun updateName(name: String) {
        _state.update { it.copy(profile = it.profile?.copy(name = name)) }
    }

    fun updateHandSize(size: String) {
        _state.update { it.copy(profile = it.profile?.copy(handSize = size)) }
    }

    fun updateSessionDuration(minutes: Int) {
        _state.update { it.copy(profile = it.profile?.copy(sessionDurationMinutesPref = minutes)) }
    }

    fun updateWeeklyGoal(days: Int) {
        _state.update { it.copy(profile = it.profile?.copy(weeklyPracticeGoalDays = days)) }
    }

    fun updateLevel(level: String) {
        _state.update { it.copy(profile = it.profile?.copy(currentLevel = level)) }
    }

    fun toggleNotifications() {
        _state.update { it.copy(notificationsEnabled = !it.notificationsEnabled) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _state.value.profile?.let { profile ->
                repository.updateProfile(profile)
                _state.update { it.copy(saved = true) }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _state.value.profile?.let { profile ->
                repository.deleteAllMessages(profile.id)
                // Note: cascading deletes will handle related data
            }
        }
    }
}
