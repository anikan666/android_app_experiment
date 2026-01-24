package com.dailyplanner.data.remote

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarService @Inject constructor() {

    // Similar to GmailService, assuming we receive the authenticated Calendar service object
    // or eventually inject credentials to build it.

    suspend fun fetchTodayEvents(
        calendarApi: Calendar
    ): Result<List<CalendarEventModel>> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val startOfDay = DateTime(now - (now % 86400000)) // Very rough start of day (UTC issues usually, but okay for prototype)
            val endOfDay = DateTime(startOfDay.value + 86400000)

            val events = calendarApi.events().list("primary")
                .setTimeMin(startOfDay)
                .setTimeMax(endOfDay)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()

            val result = events.items.map { event ->
                CalendarEventModel(
                    id = event.id,
                    title = event.summary ?: "No Title",
                    startTime = event.start.dateTime?.value ?: event.start.date.value, // DateTime or Date (all day)
                    endTime = event.end.dateTime?.value ?: event.end.date.value,
                    description = event.description
                )
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createEvent(
        calendarApi: Calendar,
        title: String,
        description: String,
        startTime: Long,
        endTime: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val event = Event()
                .setSummary(title)
                .setDescription(description)
            
            val start = EventDateTime().setDateTime(DateTime(startTime))
            event.start = start
            
            val end = EventDateTime().setDateTime(DateTime(endTime))
            event.end = end

            val executedEvent = calendarApi.events().insert("primary", event).execute()
            Result.success(executedEvent.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class CalendarEventModel(
    val id: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val description: String?
)
