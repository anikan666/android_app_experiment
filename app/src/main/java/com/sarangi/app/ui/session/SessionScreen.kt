package com.sarangi.app.ui.session

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = state.phase,
        label = "session_phase"
    ) { phase ->
        when (phase) {
            SessionPhase.CHECK_IN -> CheckInPhase(state, viewModel)
            SessionPhase.PLAN_REVIEW -> PlanReviewPhase(state, viewModel, onNavigateToChat)
            SessionPhase.ACTIVE -> ActivePhase(state, viewModel)
            SessionPhase.DEBRIEF -> DebriefPhase(state, viewModel, onNavigateToDashboard)
        }
    }
}

@Composable
private fun CheckInPhase(state: SessionUiState, viewModel: SessionViewModel) {
    val durations = listOf(15, 20, 30, 45, 60)
    val energyEmojis = listOf("1", "2", "3", "4", "5")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Ready to practise?", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Text("How much time do you have?", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            durations.forEach { min ->
                FilterChip(
                    selected = state.availableMinutes == min,
                    onClick = { viewModel.updateAvailableMinutes(min) },
                    label = { Text("$min min") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Energy level?", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            energyEmojis.forEachIndexed { index, label ->
                FilterChip(
                    selected = state.energyLevel == index + 1,
                    onClick = { viewModel.updateEnergyLevel(index + 1) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = state.focusRequest,
            onValueChange = { viewModel.updateFocusRequest(it) },
            label = { Text("Anything specific you want to work on?") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Optional") },
            minLines = 2
        )

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.generatePlan() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(16.dp),
            enabled = !state.isGenerating
        ) {
            if (state.isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onTertiary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Designing your session...")
            } else {
                Text("Generate My Session", style = MaterialTheme.typography.titleMedium)
            }
        }

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PlanReviewPhase(state: SessionUiState, viewModel: SessionViewModel, onNavigateToChat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Your Session Plan", style = MaterialTheme.typography.headlineMedium)
        Text("${state.availableMinutes} minutes", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(16.dp))

        state.activities.forEachIndexed { index, activity ->
            ActivityCard(index + 1, activity)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToChat,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Adjust")
            }
            Button(
                onClick = { viewModel.startSession() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Session")
            }
        }
    }
}

@Composable
private fun ActivityCard(number: Int, activity: ActivityPlan) {
    var expanded by remember { mutableStateOf(false) }
    val icon = when (activity.activityType) {
        "warmup" -> Icons.Default.Whatshot
        "scale" -> Icons.Default.LinearScale
        "exercise" -> Icons.Default.FitnessCenter
        "repertoire" -> Icons.Default.LibraryMusic
        "sight-reading" -> Icons.Default.MenuBook
        "cooldown" -> Icons.Default.Spa
        else -> Icons.Default.MusicNote
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        activity.activityType.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(activity.description, style = MaterialTheme.typography.bodySmall, maxLines = if (expanded) Int.MAX_VALUE else 2)
                }
                Text("${activity.plannedDurationMinutes}m", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            if (expanded && activity.rationale.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    activity.rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ActivePhase(state: SessionUiState, viewModel: SessionViewModel) {
    val currentActivity = state.activities.getOrNull(state.currentActivityIndex)
    val totalMinutes = state.elapsedSeconds / 60
    val totalSeconds = state.elapsedSeconds % 60
    val activityMinutes = state.activityElapsedSeconds / 60
    val activitySeconds = state.activityElapsedSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                String.format("%02d:%02d", totalMinutes, totalSeconds),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "${state.currentActivityIndex + 1}/${state.activities.size}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (state.activities.isNotEmpty()) (state.currentActivityIndex + 1).toFloat() / state.activities.size else 0f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Current activity
        currentActivity?.let { activity ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        activity.activityType.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        String.format("%02d:%02d", activityMinutes, activitySeconds),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(activity.description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audio feedback placeholders
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text("Pitch", style = MaterialTheme.typography.labelSmall)
                    Text(state.pitchAccuracy, style = MaterialTheme.typography.titleMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text("Rhythm", style = MaterialTheme.typography.labelSmall)
                    Text(state.rhythmAccuracy, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.skipActivity() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Skip")
            }
            Button(
                onClick = { viewModel.togglePause() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isPaused) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (state.isPaused) "Resume" else "Pause")
            }
            Button(
                onClick = { viewModel.nextActivity() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (state.currentActivityIndex >= state.activities.lastIndex) "Finish" else "Next")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = { viewModel.endSession() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("End Session Early")
        }
    }
}

@Composable
private fun DebriefPhase(
    state: SessionUiState,
    viewModel: SessionViewModel,
    onNavigateToDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.tertiary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Session Complete", style = MaterialTheme.typography.headlineMedium)
        Text(
            "${state.elapsedSeconds / 60} minutes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isGenerating) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Generating summary...")
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    state.debriefSummary,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("How did that feel?", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            (1..5).forEach { rating ->
                FilterChip(
                    selected = state.sessionRating == rating,
                    onClick = { viewModel.rateSession(rating) },
                    label = { Text("$rating") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                viewModel.finishSession()
                onNavigateToDashboard()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Done", style = MaterialTheme.typography.titleMedium)
        }
    }
}
