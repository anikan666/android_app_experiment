package com.sarangi.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // Profile Section
        SectionHeader("Profile")
        state.profile?.let { profile ->
            OutlinedTextField(
                value = profile.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text("Hand size", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("small", "medium", "large").forEach { size ->
                    FilterChip(
                        selected = profile.handSize == size,
                        onClick = { viewModel.updateHandSize(size) },
                        label = { Text(size.replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text("Experience level: ${profile.currentLevel}", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Practice Section
        SectionHeader("Practice")
        state.profile?.let { profile ->
            Text("Weekly goal: ${profile.weeklyPracticeGoalDays} days")
            Slider(
                value = profile.weeklyPracticeGoalDays.toFloat(),
                onValueChange = { viewModel.updatePracticeGoal(it.toInt()) },
                valueRange = 2f..7f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.tertiary,
                    activeTrackColor = MaterialTheme.colorScheme.tertiary
                )
            )

            Text("Default session duration: ${profile.sessionDurationMinutesPref} min")
            Slider(
                value = profile.sessionDurationMinutesPref.toFloat(),
                onValueChange = { viewModel.updateSessionDuration(it.toInt()) },
                valueRange = 10f..60f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.tertiary,
                    activeTrackColor = MaterialTheme.colorScheme.tertiary
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Notifications Section
        SectionHeader("Notifications")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Practice nudges")
            Switch(
                checked = state.nudgesEnabled,
                onCheckedChange = { viewModel.updateNudgesEnabled(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.tertiary)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Data Section
        SectionHeader("Data")
        OutlinedButton(
            onClick = { viewModel.clearAllData() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Conversation History")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About Section
        SectionHeader("About")
        Text("Sarangi v1.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text("Your daily violin practice partner", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.saveSettings() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save Changes")
        }

        if (state.saved) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Settings saved.", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
