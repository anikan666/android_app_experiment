package com.dailyplanner.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyplanner.data.local.entity.TaskEntity
import com.dailyplanner.data.remote.CalendarEventModel
import com.dailyplanner.data.repository.CalendarRepository
import com.dailyplanner.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    // Combine tasks and calendar events
    val uiState: StateFlow<PlannerUiState> = combine(
        taskRepository.allTasks,
        calendarRepository.events
    ) { tasks, events ->
        PlannerUiState(
            unscheduledTasks = tasks.filter { !it.isScheduled },
            calendarEvents = events
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlannerUiState()
    )

    fun scheduleTask(task: TaskEntity, startTime: Long) {
        viewModelScope.launch {
            // TODO: Pass actual Google Calendar API instance
            // calendarRepository.scheduleTask(api, task, startTime)
            
            // For MVP UI testing without auth, explicitly update local state if repo requires API
            // Or mock it.
        }
    }
}

data class PlannerUiState(
    val unscheduledTasks: List<TaskEntity> = emptyList(),
    val calendarEvents: List<CalendarEventModel> = emptyList()
)
