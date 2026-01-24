package com.dailyplanner.data.repository

import com.dailyplanner.data.local.dao.TaskDao
import com.dailyplanner.data.local.entity.TaskEntity
import com.dailyplanner.data.remote.AnalyzedTaskData
import com.dailyplanner.data.remote.EmailMessage
import com.dailyplanner.data.remote.GmailService
import com.dailyplanner.data.remote.LLMService
import com.google.api.services.gmail.Gmail
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class TaskRepositoryTest {
    
    private val gmailService: GmailService = mockk()
    private val llmService: LLMService = mockk()
    private val taskDao: TaskDao = mockk(relaxed = true)
    private val gmailApi: Gmail = mockk() // Dummy for the signature

    private val repository = TaskRepository(gmailService, llmService, taskDao)

    @Test
    fun `syncTasksFromEmails fetches emails, analyzes them, and saves to DB`() = runTest {
        // GIVEN
        val mockEmail = EmailMessage(
            id = "101",
            subject = "Project Update",
            sender = "boss@company.com",
            date = "2024-01-24",
            body = "We need to finish the report by Friday."
        )
        val mockAnalysis = AnalyzedTaskData(
            summary = "Finish Report",
            category = "TO-DO",
            poc = "Me",
            dueDate = "2024-01-26",
            priority = "HIGH"
        )

        // Mock GmailService returns list of emails
        coEvery { gmailService.fetchRecentEmails(any(), any()) } returns Result.success(listOf(mockEmail))

        // Mock LLMService analyzes the email
        coEvery { llmService.analyzeEmail(mockEmail) } returns mockAnalysis

        // WHEN
        repository.syncTasksFromEmails(gmailApi)

        // THEN
        // Verify task was inserted with correct data
        val slot = slot<TaskEntity>()
        coVerify { taskDao.insertTask(capture(slot)) }
        
        val capturedTask = slot.captured
        assertEquals("Finish Report", capturedTask.title)
        assertEquals("TO-DO", capturedTask.status)
        assertEquals("HIGH", capturedTask.priority)
        assertEquals("101", capturedTask.originalEmailId)
        // Description check?
        assert(capturedTask.description.contains("boss@company.com"))
    }
}
