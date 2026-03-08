package com.sarangi.ai.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnthropicClient @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val streamingClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(request: MessageRequest): ApiResult<MessageResponse> {
        return withContext(Dispatchers.IO) {
            retryWithBackoff {
                val requestBody = json.encodeToString(MessageRequest.serializer(), request.copy(stream = false))
                val httpRequest = buildRequest(requestBody)

                val response = client.newCall(httpRequest).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@retryWithBackoff ApiResult.Error("Empty response")
                    val messageResponse = json.decodeFromString(MessageResponse.serializer(), body)
                    ApiResult.Success(messageResponse)
                } else {
                    ApiResult.Error("API error: ${response.code} ${response.message}", response.code)
                }
            }
        }
    }

    fun streamMessage(request: MessageRequest): Flow<String> = flow {
        val requestBody = json.encodeToString(MessageRequest.serializer(), request.copy(stream = true))
        val httpRequest = buildRequest(requestBody)

        val response = streamingClient.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            emit("[Error: ${response.code} ${response.message}]")
            return@flow
        }

        val reader = response.body?.source()?.inputStream()?.bufferedReader()
            ?: return@flow

        try {
            reader.forEachLine { line ->
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data.isNotEmpty() && data != "[DONE]") {
                        try {
                            val event = json.decodeFromString(StreamEvent.serializer(), data)
                            if (event.type == "content_block_delta" && event.delta?.text?.isNotEmpty() == true) {
                                // emit handled below
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
        } finally {
            reader.close()
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    fun streamMessageTokens(request: MessageRequest): Flow<String> = flow {
        val requestBody = json.encodeToString(MessageRequest.serializer(), request.copy(stream = true))
        val httpRequest = buildRequest(requestBody)

        val response = streamingClient.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            emit("[Error: ${response.code} ${response.message}]")
            return@flow
        }

        val reader: BufferedReader = response.body?.source()?.inputStream()?.bufferedReader()
            ?: return@flow

        try {
            var line = reader.readLine()
            while (line != null) {
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data.isNotEmpty() && data != "[DONE]") {
                        try {
                            val event = json.decodeFromString(StreamEvent.serializer(), data)
                            if (event.type == "content_block_delta" && event.delta?.text?.isNotEmpty() == true) {
                                emit(event.delta.text)
                            }
                        } catch (_: Exception) { }
                    }
                }
                line = reader.readLine()
            }
        } finally {
            reader.close()
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    private fun buildRequest(body: String): Request {
        return Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .post(body.toRequestBody("application/json".toMediaType()))
            .addHeader("x-api-key", apiKeyProvider.getApiKey())
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .build()
    }

    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        block: suspend () -> ApiResult<T>
    ): ApiResult<T> {
        var lastError: ApiResult.Error? = null
        repeat(maxRetries) { attempt ->
            val result = try {
                block()
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
            when (result) {
                is ApiResult.Success -> return result
                is ApiResult.Error -> {
                    lastError = result
                    if (attempt < maxRetries - 1) {
                        delay((1L shl (attempt + 1)) * 1000)
                    }
                }
                is ApiResult.Loading -> { }
            }
        }
        return lastError ?: ApiResult.Error("Max retries exceeded")
    }
}

interface ApiKeyProvider {
    fun getApiKey(): String
}
