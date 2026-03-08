package com.sarangi.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarangi.ai.client.ApiResult
import com.sarangi.ai.client.ConversationManager
import com.sarangi.ai.prompts.SystemPromptType
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
import kotlinx.serialization.json.*
import javax.inject.Inject

enum class SessionPhase {
    CHECK_IN, PLAN_REVIEW, ACTIVE, DEBRIEF
}

@Serializable
data class PlannedActivity(
    val activityType: String = "exercise",
    val description: String = "",
    val plannedDurationMinutes: Int = 5,
    val rationale: String = ""
)

data class SessionState(
    val phase: SessionPhase = SessionPhase.CHECK_IN,
    val availableMinutes: Int = 30,
    val energyLevel: Int = 3,
    val specificFocus: String = "",
    val isGeneratingPlan: Boolean = false,
    val activities: List<PlannedActivity> = emptyList(),
    val currentActivityIndex: Int = 0,
    val elapsedSeconds: Long = 0,
    val activityElapsedSeconds: Long = 0,
    val isPaused: Boolean = false,
    val sessionId: Long? = null,
    val debriefSummary: String = "",
    val isGeneratingDebrief: Boolean = false,
    val sessionRating: Int? = null,
    val error: String? = null,
    val pitchAccuracy: String = "--%",
    val rhythmAccuracy: String = "--%"
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: SarangiRepository,
    private val conversationManager: ConversationManager
) : ViewModel() {

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var studentId: Long = 0
    private val json = Json { ignoreUnknownKeys = true }

    init {
        viewModelScope.launch {
            val profile = repository.getActiveProfileOnce()
            if (profile != null) {
                studentId = profile.id
                _state.update { it.copy(availableMinutes = profile.sessionDurationMinutesPref) }
                // Check for active session to resume
                val activeSession = repository.getActiveSession(studentId)
                if (activeSession != null) {
                    _state.update { it.copy(sessionId = activeSession.id, phase = SessionPhase.ACTIVE) }
                    loadActivities(activeSession.id)
                }
            }
        }
    }

    fun updateAvailableMinutes(minutes: Int) { _state.update { it.copy(availableMinutes = minutes) } }
    fun updateEnergyLevel(level: Int) { _state.update { it.copy(energyLevel = level) } }
    fun updateSpecificFocus(focus: String) { _state.update { it.copy(specificFocus = focus) } }

    fun generatePlan() {
        _state.update { it.copy(isGeneratingPlan = true, error = null) }
        viewModelScope.launch {
            try {
                val s = _state.value
                val prompt = buildString {
                    appendLine("Design a ${s.availableMinutes}-minute practice session.")
                    appendLine("Energy level: ${s.energyLevel}/5")
                    if (s.specificFocus.isNotBlank()) appendLine("Specific focus: ${s.specificFocus}")
                    appendLine("Return ONLY a JSON array of activities.")
                }

                val result = conversationManager.sendNonStreaming(
                    userMessage = prompt,
                    promptType = SystemPromptType.SessionArchitect,
                    studentId = studentId
                )

                when (result) {
                    is ApiResult.Success -> {
                        val activities = parseActivities(result.data)
                        _state.update {
                            it.copy(
                                activities = activities,
                                isGeneratingPlan = false,
                                phase = SessionPhase.PLAN_REVIEW
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        // Fallback to a default plan
                        val defaults = generateDefaultPlan(s.availableMinutes, s.energyLevel)
                        _state.update {
                            it.copy(
                                activities = defaults,
                                isGeneratingPlan = false,
                                phase = SessionPhase.PLAN_REVIEW,
                                error = "Used offline plan template. ${result.message}"
                            )
                        }
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                val defaults = generateDefaultPlan(_state.value.availableMinutes, _state.value.energyLevel)
                _state.update {
                    it.copy(
                        activities = defaults,
                        isGeneratingPlan = false,
                        phase = SessionPhase.PLAN_REVIEW,
                        error = "Used offline plan template."
                    )
                }
            }
        }
    }

    fun startSession() {
        viewModelScope.launch {
            val s = _state.value
            val session = PracticeSession(
                studentId = studentId,
                startedAt = System.currentTimeMillis(),
                plannedDurationMinutes = s.availableMinutes,
                energyLevel = s.energyLevel,
                sessionPlanJson = json.encodeToString(
                    JsonArray.serializer(),
                    JsonArray(s.activities.map { a ->
                        buildJsonObject {
                            put("activityType", a.activityType)
                            put("description", a.description)
                            put("plannedDurationMinutes", a.plannedDurationMinutes)
                        }
                    })
                )
            )
            val sessionId = repository.insertSession(session)

            s.activities.forEachIndexed { index, activity ->
                repository.insertActivity(
                    SessionActivity(
                        sessionId = sessionId,
                        orderIndex = index,
                        activityType = activity.activityType,
                        description = activity.description,
                        plannedDurationMinutes = activity.plannedDurationMinutes,
                        rationale = activity.rationale
                    )
                )
            }

            _state.update {
                it.copy(
                    sessionId = sessionId,
                    phase = SessionPhase.ACTIVE,
                    currentActivityIndex = 0,
                    elapsedSeconds = 0,
                    activityElapsedSeconds = 0
                )
            }
            startTimer()
        }
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
                    // Check if current activity time is up
                    val s = _state.value
                    val currentActivity = s.activities.getOrNull(s.currentActivityIndex)
                    if (currentActivity != null) {
                        val activityDurationSec = currentActivity.plannedDurationMinutes * 60L
                        if (s.activityElapsedSeconds >= activityDurationSec) {
                            nextActivity()
                        }
                    }
                }
            }
        }
    }

    fun togglePause() {
        _state.update { it.copy(isPaused = !it.isPaused) }
    }

    fun nextActivity() {
        val s = _state.value
        if (s.currentActivityIndex < s.activities.size - 1) {
            _state.update {
                it.copy(
                    currentActivityIndex = it.currentActivityIndex + 1,
                    activityElapsedSeconds = 0
                )
            }
        } else {
            endSession()
        }
    }

    fun skipActivity() {
        viewModelScope.launch {
            val s = _state.value
            val sessionId = s.sessionId ?: return@launch
            val activities = repository.getActivitiesForSessionOnce(sessionId)
            val current = activities.getOrNull(s.currentActivityIndex)
            if (current != null) {
                repository.updateActivity(current.copy(skipped = true))
            }
            nextActivity()
        }
    }

    fun endSession() {
        timerJob?.cancel()
        _state.update { it.copy(phase = SessionPhase.DEBRIEF, isGeneratingDebrief = true) }

        viewModelScope.launch {
            val s = _state.value
            val sessionId = s.sessionId ?: return@launch

            // Update session
            val completedCount = s.currentActivityIndex + 1
            val totalCount = s.activities.size
            val completionRate = completedCount.toFloat() / totalCount.coerceAtLeast(1)

            repository.updateSession(
                PracticeSession(
                    id = sessionId,
                    studentId = studentId,
                    startedAt = System.currentTimeMillis() - s.elapsedSeconds * 1000,
                    endedAt = System.currentTimeMillis(),
                    plannedDurationMinutes = s.availableMinutes,
                    actualDurationMinutes = (s.elapsedSeconds / 60).toInt(),
                    energyLevel = s.energyLevel,
                    completionRate = completionRate,
                    sessionPlanJson = "{}"
                )
            )

            // Generate debrief
            try {
                val debriefPrompt = "Session completed. Duration: ${s.elapsedSeconds / 60} minutes. " +
                    "Completed ${completedCount} of ${totalCount} activities. " +
                    "Activities: ${s.activities.joinToString(", ") { it.description }}"

                val result = conversationManager.sendNonStreaming(
                    userMessage = debriefPrompt,
                    promptType = SystemPromptType.PostSessionSummary,
                    studentId = studentId,
                    sessionId = sessionId
                )

                val summary = when (result) {
                    is ApiResult.Success -> result.data
                    is ApiResult.Error -> "Session complete. You practised for ${s.elapsedSeconds / 60} minutes and completed $completedCount of $totalCount activities."
                    is ApiResult.Loading -> ""
                }

                _state.update { it.copy(debriefSummary = summary, isGeneratingDebrief = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        debriefSummary = "Session complete. You practised for ${s.elapsedSeconds / 60} minutes.",
                        isGeneratingDebrief = false
                    )
                }
            }
        }
    }

    fun rateSession(rating: Int) {
        _state.update { it.copy(sessionRating = rating) }
        viewModelScope.launch {
            val sessionId = _state.value.sessionId ?: return@launch
            val session = repository.getSessionById(sessionId) ?: return@launch
            repository.updateSession(session.copy(userRating = rating))
        }
    }

    fun resetSession() {
        timerJob?.cancel()
        _state.update { SessionState(availableMinutes = it.availableMinutes) }
    }

    private fun parseActivities(jsonString: String): List<PlannedActivity> {
        return try {
            val cleaned = jsonString.trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val array = json.parseToJsonElement(cleaned).jsonArray
            array.map { element ->
                val obj = element.jsonObject
                PlannedActivity(
                    activityType = obj["activityType"]?.jsonPrimitive?.content ?: "exercise",
                    description = obj["description"]?.jsonPrimitive?.content ?: "",
                    plannedDurationMinutes = obj["plannedDurationMinutes"]?.jsonPrimitive?.int ?: 5,
                    rationale = obj["rationale"]?.jsonPrimitive?.content ?: ""
                )
            }
        } catch (e: Exception) {
            generateDefaultPlan(_state.value.availableMinutes, _state.value.energyLevel)
        }
    }

    private fun generateDefaultPlan(totalMinutes: Int, energyLevel: Int): List<PlannedActivity> {
        val warmupMin = (totalMinutes * 0.15).toInt().coerceAtLeast(3)
        val cooldownMin = (totalMinutes * 0.10).toInt().coerceAtLeast(2)
        val remainingMin = totalMinutes - warmupMin - cooldownMin
        val scaleMin = (remainingMin * 0.3).toInt()
        val exerciseMin = (remainingMin * 0.3).toInt()
        val repertoireMin = remainingMin - scaleMin - exerciseMin

        return listOf(
            PlannedActivity("warmup", "Open strings and slow bowing. Focus on even tone and consistent bow speed.", warmupMin, "Warming up prepares your body and focuses your ear."),
            PlannedActivity("scale", "G major scale, two octaves. Slow tempo, focusing on intonation.", scaleMin, "Scales build the foundation for everything else."),
            PlannedActivity("exercise", "Shifting exercise: practice shifts between 1st and 3rd position.", exerciseMin, "Regular shifting practice builds confidence and accuracy."),
            PlannedActivity("repertoire", "Work on your current piece. Start from the section you find most challenging.", repertoireMin, "Applying technique to real music is where it all comes together."),
            PlannedActivity("cooldown", "Play something you enjoy. No pressure, just music.", cooldownMin, "Ending on a positive note keeps you motivated.")
        )
    }

    private fun loadActivities(sessionId: Long) {
        viewModelScope.launch {
            val dbActivities = repository.getActivitiesForSessionOnce(sessionId)
            val planned = dbActivities.map {
                PlannedActivity(it.activityType, it.description, it.plannedDurationMinutes, it.rationale ?: "")
            }
            _state.update { it.copy(activities = planned) }
            startTimer()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
