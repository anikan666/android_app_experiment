package com.sarangi.ai.session

import com.sarangi.ai.client.AnthropicClient
import com.sarangi.ai.client.ApiResult
import com.sarangi.ai.client.Message
import com.sarangi.ai.prompts.MessageBuilder
import com.sarangi.ai.prompts.SystemPromptType
import com.sarangi.core.database.SarangiRepository
import com.sarangi.core.database.entity.ConversationMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationManager @Inject constructor(
    private val anthropicClient: AnthropicClient,
    private val messageBuilder: MessageBuilder,
    private val repository: SarangiRepository
) {
    private val conversationHistories = mutableMapOf<String, MutableList<Message>>()

    suspend fun send(
        message: String,
        promptType: SystemPromptType,
        contextKey: String = "default",
        studentId: Long,
        sessionId: Long? = null,
        additionalContext: String? = null
    ): Flow<String> = flow {
        // Save user message
        repository.insertMessage(
            ConversationMessage(
                studentId = studentId,
                sessionId = sessionId,
                role = "user",
                content = message,
                messageType = promptTypeToMessageType(promptType)
            )
        )

        val history = conversationHistories.getOrPut(contextKey) { mutableListOf() }
        val request = messageBuilder.buildRequest(
            userMessage = message,
            promptType = promptType,
            conversationHistory = history,
            additionalContext = additionalContext
        )

        history.add(Message(role = "user", content = message))

        val fullResponse = StringBuilder()
        anthropicClient.streamMessageTokens(request).collect { token ->
            fullResponse.append(token)
            emit(token)
        }

        val responseText = fullResponse.toString()
        if (responseText.isNotEmpty() && !responseText.startsWith("[Error")) {
            history.add(Message(role = "assistant", content = responseText))
            repository.insertMessage(
                ConversationMessage(
                    studentId = studentId,
                    sessionId = sessionId,
                    role = "assistant",
                    content = responseText,
                    messageType = promptTypeToMessageType(promptType)
                )
            )
        }
    }

    suspend fun sendNonStreaming(
        message: String,
        promptType: SystemPromptType,
        studentId: Long,
        sessionId: Long? = null,
        additionalContext: String? = null
    ): String {
        val request = messageBuilder.buildRequest(
            userMessage = message,
            promptType = promptType,
            additionalContext = additionalContext
        )

        return when (val result = anthropicClient.sendMessage(request)) {
            is ApiResult.Success -> {
                val text = result.data.content.firstOrNull()?.text ?: ""
                repository.insertMessage(
                    ConversationMessage(
                        studentId = studentId,
                        sessionId = sessionId,
                        role = "user",
                        content = message,
                        messageType = promptTypeToMessageType(promptType)
                    )
                )
                repository.insertMessage(
                    ConversationMessage(
                        studentId = studentId,
                        sessionId = sessionId,
                        role = "assistant",
                        content = text,
                        messageType = promptTypeToMessageType(promptType)
                    )
                )
                text
            }
            is ApiResult.Error -> "[Error: ${result.message}]"
            is ApiResult.Loading -> ""
        }
    }

    fun clearHistory(contextKey: String) {
        conversationHistories.remove(contextKey)
    }

    private fun promptTypeToMessageType(type: SystemPromptType): String = when (type) {
        is SystemPromptType.SessionArchitect -> "session-plan"
        is SystemPromptType.DiagnosticConversation -> "diagnostic"
        is SystemPromptType.KnowledgeBrain -> "knowledge"
        is SystemPromptType.PostSessionSummary -> "session-plan"
    }
}
