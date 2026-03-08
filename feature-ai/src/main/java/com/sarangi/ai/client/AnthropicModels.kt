package com.sarangi.ai.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageRequest(
    val model: String = "claude-sonnet-4-20250514",
    @SerialName("max_tokens") val maxTokens: Int = 4096,
    val system: String? = null,
    val messages: List<Message>,
    val stream: Boolean = false
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class MessageResponse(
    val id: String = "",
    val type: String = "",
    val role: String = "",
    val content: List<ContentBlock> = emptyList(),
    @SerialName("stop_reason") val stopReason: String? = null
)

@Serializable
data class ContentBlock(
    val type: String = "text",
    val text: String = ""
)

@Serializable
data class StreamEvent(
    val type: String = "",
    val delta: Delta? = null,
    val index: Int? = null
)

@Serializable
data class Delta(
    val type: String = "",
    val text: String = "",
    @SerialName("stop_reason") val stopReason: String? = null
)
