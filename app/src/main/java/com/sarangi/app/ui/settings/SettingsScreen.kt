package com.sarangi.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Spacer(modifier = Modifier.height(20.dp))

        // Profile section
        Text("Profile", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
        Spacer(modifier = Modifier.height(8.dp))

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
            Text("Level", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("beginner", "early-intermediate", "intermediate").forEach { level ->
                    FilterChip(
                        selected = profile.currentLevel == level,
                        onClick = { viewModel.updateLevel(level) },
                        label = { Text(level.replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Practice section
        Text("Practice", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
        Spacer(modifier = Modifier.height(8.dp))

        state.profile?.let { profile ->
            Text("Default session duration: ${profile.sessionDurationMinutesPref} min")
            Slider(
                value = profile.sessionDurationMinutesPref.toFloat(),
                onValueChange = { viewModel.updateSessionDuration(it.toInt()) },
                valueRange = 10f..60f,
                steps = 9,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.tertiary, activeTrackColor = MaterialTheme.colorScheme.tertiary)
            )

            Text("Weekly goal: ${profile.weeklyPracticeGoalDays} days")
            Slider(
                value = profile.weeklyPracticeGoalDays.toFloat(),
                onValueChange = { viewModel.updateWeeklyGoal(it.toInt()) },
                valueRange = 1f..7f,
                steps = 5,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.tertiary, activeTrackColor = MaterialTheme.colorScheme.tertiary)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Notifications section
        Text("Notifications", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Practice nudges")
            Switch(
                checked = state.notificationsEnabled,
                onCheckedChange = { viewModel.toggleNotifications() },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.tertiary)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Save button
        Button(
            onClick = { viewModel.saveProfile() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (state.saved) "Saved" else "Save Changes")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Danger zone
        var showClearDialog by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { showClearDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear All Data")
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear all data?") },
                text = { Text("This will delete all your practice sessions, observations, and chat history. This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllData()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Clear") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Sarangi v1.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
