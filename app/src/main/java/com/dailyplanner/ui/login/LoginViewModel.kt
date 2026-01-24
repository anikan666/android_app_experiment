package com.dailyplanner.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import com.dailyplanner.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    suspend fun signIn(context: Context): Result<String> {
        return authRepository.signIn(context)
    }
}
