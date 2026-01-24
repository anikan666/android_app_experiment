package com.dailyplanner.data.remote

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GmailService @Inject constructor(
    // In a real app, we need a way to get the authenticated Gmail service.
    // Ideally, AuthRepository should provide the credential to build this,
    // or this service should be created via a Factory that takes credentials.
    // For this boilerplate, assuming we can get a configured service or handle it later.
    // Currently putting placeholder logic.
) {
    suspend fun fetchRecentEmails(
        gmailService: Gmail, // Passed from UI/ViewModel after auth for now
        maxResults: Long = 10
    ): Result<List<EmailMessage>> = withContext(Dispatchers.IO) {
        try {
            val listResponse = gmailService.users().messages().list("me")
                .setMaxResults(maxResults)
                .setQ("category:primary") // Filter for primary inbox
                .execute()

            val messages = listResponse.messages ?: emptyList()
            val emailDetails = messages.mapNotNull { message ->
                fetchEmailDetails(gmailService, message.id)
            }
            Result.success(emailDetails)
        } catch (e: UserRecoverableAuthIOException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    private fun fetchEmailDetails(service: Gmail, messageId: String): EmailMessage? {
        return try {
            val message = service.users().messages().get("me", messageId).execute()
            val payload = message.payload
            val headers = payload.headers
            
            val subject = headers.find { it.name.equals("Subject", ignoreCase = true) }?.value ?: "(No Subject)"
            val from = headers.find { it.name.equals("From", ignoreCase = true) }?.value ?: "Unknown"
            val date = headers.find { it.name.equals("Date", ignoreCase = true) }?.value ?: ""
            
            // Basic body extraction (very simplified)
            val body = decodeBody(payload)

            EmailMessage(
                id = messageId,
                subject = subject,
                sender = from,
                date = date,
                body = body
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeBody(payload: com.google.api.services.gmail.model.MessagePart): String {
        return try {
             if (payload.body?.data != null) {
                String(java.util.Base64.getUrlDecoder().decode(payload.body.data))
            } else if (payload.parts != null) {
                payload.parts.joinToString("\n") { decodeBody(it) }
            } else {
                 ""
             }
        } catch (e: Exception) {
            ""
        }
    }
}

// Simple Domain Model for this phase
data class EmailMessage(
    val id: String,
    val subject: String,
    val sender: String,
    val date: String,
    val body: String
)
