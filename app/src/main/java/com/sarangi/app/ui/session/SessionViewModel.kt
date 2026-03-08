package com.sarangi.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarangi.ai.prompts.SystemPromptType
import com.sarangi.ai.session.ConversationManager
import com.sarangi.core.database.SarangiRepository
import com.sarangi.core.database.entity.PracticeSession
import com.sarangi.core.database.entity.SessionActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class SessionPhase {
    CHECK_IN, PLAN_REVIEW, ACTIVE, DEBRIEF
}

@Serializable
data class ActivityPlan(
    val activityType: String = "",
    val description: String = "",
    val plannedDurationMinutes: Int = 5,
    val rationale: String = ""
)

data class SessionUiState(
    val phase: SessionPhase = SessionPhase.CHECK_IN,
    val availableMinutes: Int = 30,
    val energyLevel: Int = 3,
    val focusRequest: String = "",
    val isGenerating: Boolean = false,
    val activities: List<ActivityPlan> = emptyList(),
    val currentActivityIndex: Int = 0,
    val elapsedSeconds: Long = 0,
    val activityElapsedSeconds: Long = 0,
    val isPaused: Boolean = false,
    val sessionId: Long = 0,
    val debriefSummary: String = "",
    val sessionRating: Int = 0,
    val error: String? = null,
    val pitchAccuracy: String = "--%",
    val rhythmAccuracy: String = "--%"
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val conversationManager: ConversationManager,
    private val repository: SarangiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var studentId: Long = 0
    private val json = Json { ignoreUnknownKeys = true }

    init {
        viewModelScope.launch {
            val profile = repository.getActiveProfileOnce()
            studentId = profile?.id ?: 0
            profile?.let {
                _state.update { s -> s.copy(availableMinutes = it.sessionDurationMinutesPref) }
            }
            // Check for active session to resume
            if (studentId > 0) {
                val activeSession = repository.getActiveSession(studentId)
                if (activeSession != null) {
                    resumeSession(activeSession)
                }
            }
        }
    }

    fun updateAvailableMinutes(minutes: Int) { _state.update { it.copy(availableMinutes = minutes) } }
    fun updateEnergyLevel(level: Int) { _state.update { it.copy(energyLevel = level) } }
    fun updateFocusRequest(text: String) { _state.update { it.copy(focusRequest = text) } }

    fun generatePlan() {
        _state.update { it.copy(isGenerating = true, error = null) }
        viewModelScope.launch {
            try {
                val prompt = buildString {
                    appendLine("Generate a practice session plan.")
                    appendLine("Available time: ${_state.value.availableMinutes} minutes")
                    appendLine("Energy level: ${_state.value.energyLevel}/5")
                    if (_state.value.focusRequest.isNotBlank()) {
                        appendLine("Student wants to focus on: ${_state.value.focusRequest}")
                    }
                }

                val response = conversationManager.sendNonStreaming(
                    message = prompt,
                    promptType = SystemPromptType.SessionArchitect,
                    studentId = studentId
                )

                val activities = try {
                    json.decodeFromString<List<ActivityPlan>>(response.trim())
                } catch (_: Exception) {
                    // Fallback plan
                    generateFallbackPlan()
                }

                // Save session to DB
                val session = PracticeSession(
                    studentId = studentId,
                    plannedDurationMinutes = _state.value.availableMinutes,
                    energyLevel = _state.value.energyLevel,
                    sessionPlanJson = response
                )
                val sessionId = repository.insertSession(session)

                activities.forEachIndexed { index, plan ->
                    repository.insertActivity(
                        SessionActivity(
                            sessionId = sessionId,
                            orderIndex = index,
                            activityType = plan.activityType,
                            description = plan.description,
                            plannedDurationMinutes = plan.plannedDurationMinutes
                        )
                    )
                }

                _state.update {
                    it.copy(
                        isGenerating = false,
                        activities = activities,
                        sessionId = sessionId,
                        phase = SessionPhase.PLAN_REVIEW
                    )
                }
            } catch (e: Exception) {
                val fallback = generateFallbackPlan()
                val session = PracticeSession(
                    studentId = studentId,
                    plannedDurationMinutes = _state.value.availableMinutes,
                    energyLevel = _state.value.energyLevel,
                    sessionPlanJson = "offline-fallback"
                )
                val sessionId = repository.insertSession(session)
                _state.update {
                    it.copy(
                        isGenerating = false,
                        activities = fallback,
                        sessionId = sessionId,
                        phase = SessionPhase.PLAN_REVIEW,
                        error = "Generated offline plan — connect for AI-tailored sessions."
                    )
                }
            }
        }
    }

    fun startSession() {
        _state.update { it.copy(phase = SessionPhase.ACTIVE) }
        startTimer()
    }

    fun togglePause() {
        val paused = !_state.value.isPaused
        _state.update { it.copy(isPaused = paused) }
        if (paused) timerJob?.cancel() else startTimer()
    }

    fun nextActivity() {
        val current = _state.value.currentActivityIndex
        if (current < _state.value.activities.lastIndex) {
            viewModelScope.launch {
                // Mark current activity as completed
                markActivityCompleted(current)
                _state.update {
                    it.copy(
                        currentActivityIndex = current + 1,
                        activityElapsedSeconds = 0
                    )
                }
            }
        } else {
            endSession()
        }
    }

    fun skipActivity() {
        nextActivity()
    }

    fun endSession() {
        timerJob?.cancel()
        _state.update { it.copy(phase = SessionPhase.DEBRIEF, isGenerating = true) }
        viewModelScope.launch {
            // Mark current activity completed
            markActivityCompleted(_state.value.currentActivityIndex)

            // Update session end time
            val session = repository.getSessionById(_state.value.sessionId)
            session?.let {
                val completedCount = (_state.value.currentActivityIndex + 1).coerceAtMost(_state.value.activities.size)
                val completionRate = if (_state.value.activities.isNotEmpty())
                    completedCount.toFloat() / _state.value.activities.size else 0f
                repository.updateSession(
                    it.copy(
                        endedAt = System.currentTimeMillis(),
                        actualDurationMinutes = (_state.value.elapsedSeconds / 60).toInt(),
                        completionRate = completionRate
                    )
                )
            }

            // Generate debrief
            try {
                val summary = conversationManager.sendNonStreaming(
                    message = "Session completed. Duration: ${_state.value.elapsedSeconds / 60} minutes. Activities completed: ${_state.value.currentActivityIndex + 1}/${_state.value.activities.size}.",
                    promptType = SystemPromptType.PostSessionSummary,
                    studentId = studentId,
                    sessionId = _state.value.sessionId
                )
                _state.update { it.copy(debriefSummary = summary, isGenerating = false) }
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        debriefSummary = "Great work today! You practised for ${_state.value.elapsedSeconds / 60} minutes.",
                        isGenerating = false
                    )
                }
            }
        }
    }

    fun rateSession(rating: Int) {
        _state.update { it.copy(sessionRating = rating) }
    }

    fun finishSession() {
        viewModelScope.launch {
            val session = repository.getSessionById(_state.value.sessionId)
            session?.let {
                repository.updateSession(it.copy(notes = _state.value.debriefSummary))
            }
        }
        // Reset state
        _state.update { SessionUiState() }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_state.value.isPaused) {
                    _state.update {
                        it.copy(
                            elapsedSeconds = it.elapsedSeconds + 1,
                            activityElapsedSeconds = it.activityElapsedSeconds + 1
                        )
                    }
                }
            }
        }
    }

    private suspend fun markActivityCompleted(index: Int) {
        if (index < 0 || index >= _state.value.activities.size) return
        val activities = repository.getActivitiesForSessionOnce(_state.value.sessionId)
        activities.getOrNull(index)?.let { activity ->
            repository.updateActivity(
                activity.copy(
                    completed = true,
                    actualDurationMinutes = (_state.value.activityElapsedSeconds / 60).toInt().coerceAtLeast(1)
                )
            )
        }
    }

    private fun resumeSession(session: PracticeSession) {
        viewModelScope.launch {
            val activities = repository.getActivitiesForSessionOnce(session.id)
            val plans = activities.map {
                ActivityPlan(
                    activityType = it.activityType,
                    description = it.description,
                    plannedDurationMinutes = it.plannedDurationMinutes
                )
            }
            val completedCount = activities.count { it.completed }
            _state.update {
                it.copy(
                    phase = SessionPhase.ACTIVE,
                    activities = plans,
                    sessionId = session.id,
                    currentActivityIndex = completedCount.coerceAtMost(plans.lastIndex.coerceAtLeast(0)),
                    availableMinutes = session.plannedDurationMinutes
                )
            }
            startTimer()
        }
    }

    private fun generateFallbackPlan(): List<ActivityPlan> {
        val total = _state.value.availableMinutes
        return listOf(
            ActivityPlan("warmup", "Open string bowing: long, slow bows on each string. Focus on consistent contact point and even tone.", (total * 0.15).toInt().coerceAtLeast(2), "Warming up bow arm and establishing good tone production."),
            ActivityPlan("scale", "G major scale, two octaves. Slow tempo, focus on intonation. Use tuner if available.", (total * 0.2).toInt().coerceAtLeast(3), "Building left hand accuracy and muscle memory."),
            ActivityPlan("exercise", "Simple etude or shifting exercise appropriate to current level.", (total * 0.25).toInt().coerceAtLeast(4), "Technical development."),
            ActivityPlan("repertoire", "Work on your current piece. Focus on one specific passage that needs attention.", (total * 0.3).toInt().coerceAtLeast(5), "Applying technique to musical context."),
            ActivityPlan("cooldown", "Play something you enjoy and know well. Focus on musical expression.", (total * 0.1).toInt().coerceAtLeast(2), "Ending on a positive note.")
        )
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
