package com.sarangi.ai.client

import com.sarangi.ai.prompts.StudentContextBuilder
import com.sarangi.ai.prompts.SystemPromptType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageBuilder @Inject constructor(
    private val contextBuilder: StudentContextBuilder
) {
    private val maxHistorySize = 20

    suspend fun buildSystemPrompt(promptType: SystemPromptType): String {
        val studentContext = contextBuilder.buildContext()
        return "${promptType.prompt}\n\n$studentContext"
    }

    fun buildMessages(
        history: List<Message>,
        newMessage: String
    ): List<Message> {
        val trimmedHistory = if (history.size > maxHistorySize) {
            history.takeLast(maxHistorySize)
        } else {
            history
        }
        return trimmedHistory + Message(role = "user", content = newMessage)
    }
}
