package com.sarangi.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarangi.ai.prompts.SystemPromptType
import com.sarangi.ai.session.ConversationManager
import com.sarangi.core.database.SarangiRepository
import com.sarangi.core.database.entity.ConversationMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val id: Long = 0,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

enum class ChatMode(val label: String) {
    GENERAL("General"),
    DIAGNOSE("Diagnose"),
    SESSION("Session")
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isLoading: Boolean = false,
    val chatMode: ChatMode = ChatMode.GENERAL,
    val showSessionMode: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationManager: ConversationManager,
    private val repository: SarangiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var studentId: Long = 0

    init {
        viewModelScope.launch {
            val profile = repository.getActiveProfileOnce()
            studentId = profile?.id ?: 0
            if (studentId > 0) {
                loadMessages()
            }
        }
    }

    private suspend fun loadMessages() {
        val messages = repository.getRecentMessagesOnce(studentId, 50)
        _state.update {
            it.copy(messages = messages.reversed().map { msg ->
                ChatMessage(id = msg.id, role = msg.role, content = msg.content, timestamp = msg.timestamp)
            })
        }
        if (_state.value.messages.isEmpty()) {
            _state.update {
                it.copy(messages = listOf(
                    ChatMessage(
                        role = "assistant",
                        content = "What's on your mind? I can help with technique questions, music theory, practice strategies, or anything else about your violin journey."
                    )
                ))
            }
        }
    }

    fun updateInput(input: String) {
        _state.update { it.copy(currentInput = input) }
    }

    fun setChatMode(mode: ChatMode) {
        _state.update { it.copy(chatMode = mode) }
    }

    fun sendMessage() {
        val input = _state.value.currentInput.trim()
        if (input.isEmpty() || _state.value.isLoading) return

        val userMessage = ChatMessage(role = "user", content = input)
        _state.update {
            it.copy(
                messages = it.messages + userMessage,
                currentInput = "",
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            val promptType = when (_state.value.chatMode) {
                ChatMode.GENERAL -> SystemPromptType.KnowledgeBrain
                ChatMode.DIAGNOSE -> SystemPromptType.DiagnosticConversation
                ChatMode.SESSION -> SystemPromptType.SessionArchitect
            }

            val streamingMessage = ChatMessage(role = "assistant", content = "", isStreaming = true)
            _state.update { it.copy(messages = it.messages + streamingMessage) }

            try {
                val responseBuilder = StringBuilder()
                conversationManager.send(
                    message = input,
                    promptType = promptType,
                    contextKey = _state.value.chatMode.name,
                    studentId = studentId
                ).collect { token ->
                    responseBuilder.append(token)
                    val currentMessages = _state.value.messages.toMutableList()
                    val lastIndex = currentMessages.lastIndex
                    if (lastIndex >= 0) {
                        currentMessages[lastIndex] = currentMessages[lastIndex].copy(
                            content = responseBuilder.toString()
                        )
                        _state.update { it.copy(messages = currentMessages) }
                    }
                }

                val finalMessages = _state.value.messages.toMutableList()
                val lastIndex = finalMessages.lastIndex
                if (lastIndex >= 0) {
                    finalMessages[lastIndex] = finalMessages[lastIndex].copy(isStreaming = false)
                    _state.update { it.copy(messages = finalMessages, isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update {
                    val msgs = it.messages.toMutableList()
                    if (msgs.lastOrNull()?.isStreaming == true) {
                        msgs.removeAt(msgs.lastIndex)
                    }
                    it.copy(
                        messages = msgs,
                        isLoading = false,
                        error = "Failed to get response. Please try again."
                    )
                }
            }
        }
    }
}
