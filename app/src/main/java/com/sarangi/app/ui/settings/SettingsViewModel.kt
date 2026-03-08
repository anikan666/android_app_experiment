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

data class SettingsState(
    val profile: StudentProfile? = null,
    val nudgesEnabled: Boolean = true,
    val maxNudgesPerDay: Int = 1,
    val isLoading: Boolean = true,
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SarangiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

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

    fun updatePracticeGoal(days: Int) {
        _state.update { it.copy(profile = it.profile?.copy(weeklyPracticeGoalDays = days)) }
    }

    fun updateSessionDuration(minutes: Int) {
        _state.update { it.copy(profile = it.profile?.copy(sessionDurationMinutesPref = minutes)) }
    }

    fun updateNudgesEnabled(enabled: Boolean) {
        _state.update { it.copy(nudgesEnabled = enabled) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val profile = _state.value.profile ?: return@launch
            repository.updateProfile(profile)
            _state.update { it.copy(saved = true) }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            val profile = _state.value.profile ?: return@launch
            repository.deleteAllMessages(profile.id)
        }
    }
}
