package com.sarangi.ai.client

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
    suspend fun send(
        userMessage: String,
        promptType: SystemPromptType,
        studentId: Long,
        sessionId: Long? = null,
        conversationHistory: List<Message> = emptyList()
    ): Flow<String> = flow {
        // Save user message
        repository.insertMessage(
            ConversationMessage(
                studentId = studentId,
                sessionId = sessionId,
                role = "user",
                content = userMessage,
                messageType = getMessageType(promptType)
            )
        )

        val systemPrompt = messageBuilder.buildSystemPrompt(promptType)
        val messages = messageBuilder.buildMessages(conversationHistory, userMessage)

        val fullResponse = StringBuilder()
        anthropicClient.streamMessage(messages, systemPrompt).collect { chunk ->
            fullResponse.append(chunk)
            emit(chunk)
        }

        // Save assistant response
        repository.insertMessage(
            ConversationMessage(
                studentId = studentId,
                sessionId = sessionId,
                role = "assistant",
                content = fullResponse.toString(),
                messageType = getMessageType(promptType)
            )
        )
    }

    suspend fun sendNonStreaming(
        userMessage: String,
        promptType: SystemPromptType,
        studentId: Long,
        sessionId: Long? = null,
        conversationHistory: List<Message> = emptyList()
    ): ApiResult<String> {
        repository.insertMessage(
            ConversationMessage(
                studentId = studentId,
                sessionId = sessionId,
                role = "user",
                content = userMessage,
                messageType = getMessageType(promptType)
            )
        )

        val systemPrompt = messageBuilder.buildSystemPrompt(promptType)
        val messages = messageBuilder.buildMessages(conversationHistory, userMessage)

        val result = anthropicClient.sendMessage(messages, systemPrompt)
        if (result is ApiResult.Success) {
            repository.insertMessage(
                ConversationMessage(
                    studentId = studentId,
                    sessionId = sessionId,
                    role = "assistant",
                    content = result.data,
                    messageType = getMessageType(promptType)
                )
            )
        }
        return result
    }

    private fun getMessageType(promptType: SystemPromptType): String = when (promptType) {
        is SystemPromptType.SessionArchitect -> "session-plan"
        is SystemPromptType.DiagnosticConversation -> "diagnostic"
        is SystemPromptType.KnowledgeBrain -> "knowledge"
        is SystemPromptType.PostSessionSummary -> "session-plan"
    }
}
