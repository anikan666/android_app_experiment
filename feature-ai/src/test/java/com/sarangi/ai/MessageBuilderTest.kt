package com.sarangi.ai

import com.sarangi.ai.client.Message
import com.sarangi.ai.prompts.MessageBuilder
import com.sarangi.ai.prompts.StudentContextBuilder
import com.sarangi.ai.prompts.SystemPromptType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MessageBuilderTest {

    private lateinit var messageBuilder: MessageBuilder
    private val contextBuilder = mockk<StudentContextBuilder>()

    @Before
    fun setup() {
        coEvery { contextBuilder.buildContext() } returns "Test student context"
        messageBuilder = MessageBuilder(contextBuilder)
    }

    @Test
    fun `buildRequest includes user message`() = runBlocking {
        val request = messageBuilder.buildRequest(
            userMessage = "How do I tune?",
            promptType = SystemPromptType.KnowledgeBrain
        )

        assertEquals(1, request.messages.size)
        assertEquals("user", request.messages.last().role)
        assertEquals("How do I tune?", request.messages.last().content)
    }

    @Test
    fun `buildRequest includes conversation history`() = runBlocking {
        val history = listOf(
            Message("user", "Hello"),
            Message("assistant", "Hi there!")
        )
        val request = messageBuilder.buildRequest(
            userMessage = "How do I tune?",
            promptType = SystemPromptType.KnowledgeBrain,
            conversationHistory = history
        )

        assertEquals(3, request.messages.size)
    }

    @Test
    fun `buildRequest trims history beyond 20`() = runBlocking {
        val history = (1..25).map { Message("user", "Message $it") }
        val request = messageBuilder.buildRequest(
            userMessage = "New message",
            promptType = SystemPromptType.KnowledgeBrain,
            conversationHistory = history
        )

        assertEquals(21, request.messages.size) // 20 from history + 1 new
    }

    @Test
    fun `buildRequest includes student context in system prompt`() = runBlocking {
        val request = messageBuilder.buildRequest(
            userMessage = "test",
            promptType = SystemPromptType.KnowledgeBrain
        )

        assertNotNull(request.system)
        assertTrue(request.system!!.contains("Test student context"))
        assertTrue(request.system!!.contains("musical knowledge resource"))
    }

    @Test
    fun `buildRequest includes additional context when provided`() = runBlocking {
        val request = messageBuilder.buildRequest(
            userMessage = "test",
            promptType = SystemPromptType.SessionArchitect,
            additionalContext = "Energy level: 3/5"
        )

        assertNotNull(request.system)
        assertTrue(request.system!!.contains("Energy level: 3/5"))
    }
}
