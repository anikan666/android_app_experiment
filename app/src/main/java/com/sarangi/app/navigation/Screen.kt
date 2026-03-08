package com.sarangi.app.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Session : Screen("session")
    data object Dashboard : Screen("dashboard")
    data object Chat : Screen("chat")
    data object Settings : Screen("settings")
    data object TeacherBriefing : Screen("teacher_briefing")
}
