package com.dailyplanner.data.repository

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.dailyplanner.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    suspend fun signIn(context: Context): Result<String> // Returns ID Token
    suspend fun signOut()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val context: Context
) : AuthRepository {

    private val credentialManager = CredentialManager.create(context)
    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: Flow<Boolean> = _isLoggedIn

    override suspend fun signIn(activityContext: Context): Result<String> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                .setAutoSelectEnabled(false) // Let user choose
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = activityContext, // Must be Activity Context
                request = request
            )

            handleSignIn(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun handleSignIn(response: GetCredentialResponse): Result<String> {
        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
             try {
                 val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                 val idToken = googleIdTokenCredential.idToken
                 
                 _isLoggedIn.value = true
                 // TODO: Use idToken to authenticate backend or Google API Client
                 // userEmail = googleIdTokenCredential.id
                 
                 return Result.success(idToken)
             } catch (e: Exception) {
                 return Result.failure(e)
             }
        }
        return Result.failure(Exception("Unknown credential type"))
    }

    override suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _isLoggedIn.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
