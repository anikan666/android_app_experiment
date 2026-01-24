package com.dailyplanner.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMService @Inject constructor() {

    // Ideally, this should be injected or configured with an API key securely.
    // For now, we'll assume the API key is provided during call or generic init.
    // WARNING: Do not hardcode real keys in repo.
    private var generativeModel: GenerativeModel? = null

    fun initialize(apiKey: String) {
        generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = apiKey
        )
    }

    suspend fun analyzeEmail(email: EmailMessage): AnalyzedTaskData = withContext(Dispatchers.IO) {
        val model = generativeModel ?: throw IllegalStateException("LLMService not initialized with API Key")

        val prompt = """
            Analyze the following email and extract actionable task details in JSON format.
            
            Email Subject: ${email.subject}
            Email Sender: ${email.sender}
            Email Body:
            ${email.body.take(2000)} // Truncate to avoid token limits if necessary
            
            Output JSON structure:
            {
                "summary": "Short description of the task",
                "category": "TO-DO" | "FOLLOW-UP" | "FYI",
                "poc": "Person responsible (if mentioned, otherwise 'Me' or 'Unknown')",
                "dueDate": "YYYY-MM-DD (if mentioned, otherwise null)",
                "priority": "HIGH" | "MEDIUM" | "LOW"
            }
            Just return valid JSON.
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val text = response.text ?: "{}"
            parseJsonToTaskData(text)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback
            AnalyzedTaskData(
                summary = email.subject,
                category = "FYI",
                poc = "Unknown",
                dueDate = null,
                priority = "LOW"
            )
        }
    }

    private fun parseJsonToTaskData(jsonString: String): AnalyzedTaskData {
        return try {
            // sanitize markdown code blocks if present
            val cleanJson = jsonString.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(cleanJson)
            AnalyzedTaskData(
                summary = json.optString("summary", "No Summary"),
                category = json.optString("category", "FYI"),
                poc = json.optString("poc", "Unknown"),
                dueDate = if (json.isNull("dueDate")) null else json.getString("dueDate"),
                priority = json.optString("priority", "LOW")
            )
        } catch (e: Exception) {
            AnalyzedTaskData("Failed to parse: $jsonString", "FYI", "Error", null, "LOW")
        }
    }
}

data class AnalyzedTaskData(
    val summary: String,
    val category: String,
    val poc: String,
    val dueDate: String?, // String for now, parse to Long later
    val priority: String
)
