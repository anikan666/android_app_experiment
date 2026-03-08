package com.sarangi.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarangi.ai.client.ConversationManager
import com.sarangi.ai.client.Message
import com.sarangi.ai.prompts.SystemPromptType
import com.sarangi.core.database.SarangiRepository
import com.sarangi.core.database.entity.ConversationMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChatMode(val label: String) {
    GENERAL("General"),
    DIAGNOSE("Diagnose"),
    SESSION("Session")
}

data class ChatMessage(
    val id: Long = 0,
    val role: String,
    val content: String,
    val isStreaming: Boolean = false
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val chatMode: ChatMode = ChatMode.GENERAL,
    val isInSession: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationManager: ConversationManager,
    private val repository: SarangiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var studentId: Long = 0

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            val profile = repository.getActiveProfileOnce() ?: return@launch
            studentId = profile.id
            val dbMessages = repository.getRecentMessagesOnce(studentId, 50)
            val chatMessages = dbMessages.reversed().map { msg ->
                ChatMessage(id = msg.id, role = msg.role, content = msg.content)
            }
            _state.update { it.copy(messages = chatMessages) }
        }
    }

    fun updateInput(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun setChatMode(mode: ChatMode) {
        _state.update { it.copy(chatMode = mode) }
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank() || _state.value.isLoading) return

        val userMessage = ChatMessage(role = "user", content = text)
        _state.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        val streamingMessage = ChatMessage(role = "assistant", content = "", isStreaming = true)
        _state.update { it.copy(messages = it.messages + streamingMessage) }

        val promptType = when (_state.value.chatMode) {
            ChatMode.GENERAL -> SystemPromptType.KnowledgeBrain
            ChatMode.DIAGNOSE -> SystemPromptType.DiagnosticConversation
            ChatMode.SESSION -> SystemPromptType.SessionArchitect
        }

        val history = _state.value.messages
            .filter { it.role in listOf("user", "assistant") && !it.isStreaming }
            .takeLast(20)
            .map { Message(role = it.role, content = it.content) }

        viewModelScope.launch {
            try {
                val fullResponse = StringBuilder()
                conversationManager.send(
                    userMessage = text,
                    promptType = promptType,
                    studentId = studentId,
                    conversationHistory = history.dropLast(1) // don't double the user message
                ).collect { chunk ->
                    fullResponse.append(chunk)
                    _state.update { state ->
                        val msgs = state.messages.toMutableList()
                        val lastIdx = msgs.lastIndex
                        if (lastIdx >= 0) {
                            msgs[lastIdx] = msgs[lastIdx].copy(content = fullResponse.toString())
                        }
                        state.copy(messages = msgs)
                    }
                }
                _state.update { state ->
                    val msgs = state.messages.toMutableList()
                    val lastIdx = msgs.lastIndex
                    if (lastIdx >= 0) {
                        msgs[lastIdx] = msgs[lastIdx].copy(isStreaming = false)
                    }
                    state.copy(messages = msgs, isLoading = false)
                }
            } catch (e: Exception) {
                _state.update { state ->
                    val msgs = state.messages.toMutableList()
                    if (msgs.lastOrNull()?.isStreaming == true) {
                        msgs.removeAt(msgs.lastIndex)
                    }
                    state.copy(
                        messages = msgs,
                        isLoading = false,
                        error = "Failed to get response. Please try again."
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
