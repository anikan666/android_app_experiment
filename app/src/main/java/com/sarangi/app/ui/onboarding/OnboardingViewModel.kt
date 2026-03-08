package com.sarangi.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarangi.core.database.SarangiRepository
import com.sarangi.core.database.entity.StudentProfile
import com.sarangi.core.model.NoiseLevel
import com.sarangi.core.model.NoiseProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val currentStep: Int = 0,
    val totalSteps: Int = 7,
    val name: String = "",
    val experience: String = "Never played",
    val hasTeacher: Boolean = false,
    val teacherFrequency: String = "weekly",
    val practiceGoalDays: Int = 4,
    val sessionDurationMinutes: Int = 30,
    val practiceTime: String = "Evening",
    val handSize: String = "Medium",
    val physicalConstraints: String = "",
    val musicalBackground: String = "",
    val isCalibrating: Boolean = false,
    val calibrationComplete: Boolean = false,
    val noiseProfile: NoiseProfile? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: SarangiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existingProfile = repository.getActiveProfileOnce()
            if (existingProfile != null) {
                _state.update { it.copy(currentStep = 7) } // Skip to end
            }
        }
    }

    fun nextStep() {
        _state.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(it.totalSteps - 1)) }
    }

    fun previousStep() {
        _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    fun updateName(name: String) { _state.update { it.copy(name = name) } }
    fun updateExperience(exp: String) { _state.update { it.copy(experience = exp) } }
    fun updateHasTeacher(has: Boolean) { _state.update { it.copy(hasTeacher = has) } }
    fun updateTeacherFrequency(freq: String) { _state.update { it.copy(teacherFrequency = freq) } }
    fun updatePracticeGoalDays(days: Int) { _state.update { it.copy(practiceGoalDays = days) } }
    fun updateSessionDuration(min: Int) { _state.update { it.copy(sessionDurationMinutes = min) } }
    fun updatePracticeTime(time: String) { _state.update { it.copy(practiceTime = time) } }
    fun updateHandSize(size: String) { _state.update { it.copy(handSize = size) } }
    fun updatePhysicalConstraints(text: String) { _state.update { it.copy(physicalConstraints = text) } }
    fun updateMusicalBackground(text: String) { _state.update { it.copy(musicalBackground = text) } }

    fun startCalibration() {
        _state.update { it.copy(isCalibrating = true) }
        viewModelScope.launch {
            // Mock calibration — real implementation comes in feature-audio
            delay(10_000)
            val mockProfile = NoiseProfile(
                rmsNoiseFloor = 0.02,
                estimatedDbSpl = 35.0,
                noiseLevel = NoiseLevel.LOW
            )
            _state.update {
                it.copy(
                    isCalibrating = false,
                    calibrationComplete = true,
                    noiseProfile = mockProfile
                )
            }
        }
    }

    fun saveProfile(onComplete: () -> Unit) {
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val s = _state.value
            val level = when (s.experience) {
                "Never played", "Played a little (< 3 months)" -> "beginner"
                "Some experience (3-12 months)" -> "early-intermediate"
                else -> "beginner"
            }
            val profile = StudentProfile(
                name = s.name.ifBlank { "Student" },
                currentLevel = level,
                handSize = s.handSize.lowercase(),
                physicalConstraints = s.physicalConstraints.ifBlank { null },
                musicalBackground = s.musicalBackground.ifBlank { null },
                weeklyPracticeGoalDays = s.practiceGoalDays,
                sessionDurationMinutesPref = s.sessionDurationMinutes,
                hasTeacher = s.hasTeacher,
                teacherFrequency = if (s.hasTeacher) s.teacherFrequency else null,
                preferredPracticeTime = s.practiceTime
            )
            repository.insertProfile(profile)
            _state.update { it.copy(isSaving = false) }
            onComplete()
        }
    }

    fun hasExistingProfile(): Boolean = _state.value.currentStep >= 7
}
