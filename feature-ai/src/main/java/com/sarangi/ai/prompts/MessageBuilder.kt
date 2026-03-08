package com.sarangi.ai.prompts

import com.sarangi.ai.client.Message
import com.sarangi.ai.client.MessageRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageBuilder @Inject constructor(
    private val studentContextBuilder: StudentContextBuilder
) {
    suspend fun buildRequest(
        userMessage: String,
        promptType: SystemPromptType,
        conversationHistory: List<Message> = emptyList(),
        additionalContext: String? = null,
        maxTokens: Int = 4096
    ): MessageRequest {
        val studentContext = studentContextBuilder.buildContext()

        val systemPrompt = buildString {
            appendLine(promptType.prompt)
            appendLine()
            appendLine(studentContext)
            additionalContext?.let {
                appendLine()
                appendLine("=== ADDITIONAL CONTEXT ===")
                appendLine(it)
            }
        }

        val messages = buildList {
            // Keep last 20 messages for context
            val recentHistory = conversationHistory.takeLast(20)
            addAll(recentHistory)
            add(Message(role = "user", content = userMessage))
        }

        return MessageRequest(
            system = systemPrompt,
            messages = messages,
            maxTokens = maxTokens
        )
    }
}
