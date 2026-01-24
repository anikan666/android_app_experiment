package com.dailyplanner.data.repository

import com.dailyplanner.data.local.dao.TaskDao
import com.dailyplanner.data.local.entity.TaskEntity
import com.dailyplanner.data.remote.GmailService
import com.dailyplanner.data.remote.LLMService
import com.google.api.services.gmail.Gmail
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val gmailService: GmailService,
    private val llmService: LLMService,
    private val taskDao: TaskDao
) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    /**
     * Core Logic:
     * 1. Fetch emails from Gmail
     * 2. For each email, use LLM to analyze/classify
     * 3. Convert to TaskEntity and save to DB
     */
    suspend fun syncTasksFromEmails(gmailApi: Gmail) {
        val emailResult = gmailService.fetchRecentEmails(gmailApi, maxResults = 10)
        
        emailResult.onSuccess { emails ->
            emails.forEach { email ->
                // Check if task already exists for this email (logic needed in DAO)
                // For now, simple proceed
                
                val analysis = llmService.analyzeEmail(email)
                
                val task = TaskEntity(
                    title = analysis.summary,
                    description = "From: ${email.sender}\n\n${email.body.take(500)}...",
                    status = analysis.category, // TO-DO, FOLLOW-UP, FYI
                    dueDate = null, // TODO: Parse date string to Long
                    poc = analysis.poc,
                    originalEmailId = email.id,
                    isScheduled = false
                )
                
                taskDao.insertTask(task)
            }
        }.onFailure {
            // Handle error (log, emit state)
        }
    }
    
    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }
}
