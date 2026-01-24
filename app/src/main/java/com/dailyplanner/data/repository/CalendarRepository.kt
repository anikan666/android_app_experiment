package com.dailyplanner.data.repository

import com.dailyplanner.data.local.dao.TaskDao
import com.dailyplanner.data.local.entity.TaskEntity
import com.dailyplanner.data.remote.CalendarEventModel
import com.dailyplanner.data.remote.CalendarService
import com.google.api.services.calendar.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    private val calendarService: CalendarService,
    private val taskDao: TaskDao
) {
    private val _events = MutableStateFlow<List<CalendarEventModel>>(emptyList())
    val events: StateFlow<List<CalendarEventModel>> = _events

    suspend fun refreshEvents(calendarApi: Calendar) {
        val result = calendarService.fetchTodayEvents(calendarApi)
        result.onSuccess {
            _events.value = it
        }
    }

    suspend fun scheduleTask(
        calendarApi: Calendar,
        task: TaskEntity,
        startTime: Long,
        durationMinutes: Long = 30
    ) {
        val endTime = startTime + (durationMinutes * 60 * 1000)
        
        // 1. Create Event on Google Calendar
        val result = calendarService.createEvent(
            calendarApi,
            title = task.title,
            description = task.description,
            startTime = startTime,
            endTime = endTime
        )

        // 2. If successful, update local TaskEntity
        result.onSuccess {
            val updatedTask = task.copy(
                isScheduled = true,
                scheduledStartTime = startTime,
                scheduledEndTime = endTime
            )
            taskDao.updateTask(updatedTask)
        }
    }
}
