package com.dailyplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailyplanner.ui.login.LoginScreen
import com.dailyplanner.ui.planner.PlannerScreen
import com.dailyplanner.ui.theme.DailyPlannerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DailyPlannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // MainViewModel checks if user is logged in
                    val mainViewModel: MainViewModel = hiltViewModel()
                    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState(initial = false)

                    if (isLoggedIn) {
                        PlannerScreen()
                    } else {
                        LoginScreen(
                            onLoginSuccess = {
                                // State flow will update and trigger recomposition
                            }
                        )
                    }
                }
            }
        }
    }
}
