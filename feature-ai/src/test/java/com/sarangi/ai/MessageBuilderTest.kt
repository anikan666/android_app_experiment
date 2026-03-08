package com.sarangi.ai

import com.sarangi.ai.client.Message
import com.sarangi.ai.client.MessageBuilder
import com.sarangi.ai.prompts.StudentContextBuilder
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
    fun `buildMessages adds new message to history`() {
        val history = listOf(
            Message("user", "Hello"),
            Message("assistant", "Hi there!")
        )
        val result = messageBuilder.buildMessages(history, "How do I tune?")

        assertEquals(3, result.size)
        assertEquals("user", result.last().role)
        assertEquals("How do I tune?", result.last().content)
    }

    @Test
    fun `buildMessages trims history beyond 20 messages`() {
        val history = (1..25).map { Message("user", "Message $it") }
        val result = messageBuilder.buildMessages(history, "New message")

        assertEquals(21, result.size) // 20 from history + 1 new
    }

    @Test
    fun `buildMessages handles empty history`() {
        val result = messageBuilder.buildMessages(emptyList(), "First message")

        assertEquals(1, result.size)
        assertEquals("First message", result.first().content)
    }

    @Test
    fun `buildSystemPrompt includes student context`() = runBlocking {
        val prompt = messageBuilder.buildSystemPrompt(
            com.sarangi.ai.prompts.SystemPromptType.KnowledgeBrain
        )

        assertTrue(prompt.contains("Test student context"))
        assertTrue(prompt.contains("musical knowledge resource"))
    }
}
