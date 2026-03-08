package com.sarangi.ai.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class ApiRequest(
    val model: String = "claude-sonnet-4-20250514",
    val max_tokens: Int = 4096,
    val system: String? = null,
    val messages: List<Message>,
    val stream: Boolean = false
)

@Singleton
class AnthropicClient @Inject constructor(
    @Named("anthropic_api_key") private val apiKey: String
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val baseUrl = "https://api.anthropic.com/v1/messages"

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

    suspend fun sendMessage(
        messages: List<Message>,
        systemPrompt: String? = null,
        maxRetries: Int = 3
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        var lastError: String? = null
        repeat(maxRetries) { attempt ->
            try {
                val request = ApiRequest(
                    system = systemPrompt,
                    messages = messages,
                    stream = false
                )
                val body = json.encodeToString(request)
                    .toRequestBody("application/json".toMediaType())

                val httpRequest = Request.Builder()
                    .url(baseUrl)
                    .post(body)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .build()

                val response = client.newCall(httpRequest).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@withContext ApiResult.Error("Empty response")
                    val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
                    val content = jsonResponse["content"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("text")?.jsonPrimitive?.content
                        ?: return@withContext ApiResult.Error("No content in response")
                    return@withContext ApiResult.Success(content)
                } else {
                    lastError = "HTTP ${response.code}: ${response.body?.string()}"
                    if (response.code in 500..599) {
                        val delay = (1L shl attempt) * 1000
                        Thread.sleep(delay)
                    } else {
                        return@withContext ApiResult.Error(lastError!!, response.code)
                    }
                }
            } catch (e: IOException) {
                lastError = e.message ?: "Network error"
                val delay = (1L shl attempt) * 1000
                Thread.sleep(delay)
            }
        }
        ApiResult.Error(lastError ?: "Unknown error")
    }

    fun streamMessage(
        messages: List<Message>,
        systemPrompt: String? = null
    ): Flow<String> = flow {
        val request = ApiRequest(
            system = systemPrompt,
            messages = messages,
            stream = true
        )
        val body = json.encodeToString(request)
            .toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(baseUrl)
            .post(body)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .build()

        val response = streamingClient.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            throw IOException("API error: ${response.code}")
        }

        val reader = response.body?.byteStream()?.bufferedReader()
            ?: throw IOException("Empty response body")

        reader.use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (l.startsWith("data: ")) {
                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val event = json.parseToJsonElement(data).jsonObject
                        val type = event["type"]?.jsonPrimitive?.content
                        if (type == "content_block_delta") {
                            val delta = event["delta"]?.jsonObject
                            val text = delta?.get("text")?.jsonPrimitive?.content
                            if (text != null) emit(text)
                        }
                    } catch (_: Exception) { }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
