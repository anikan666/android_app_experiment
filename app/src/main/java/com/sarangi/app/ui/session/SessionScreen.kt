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
private fun CheckInPhase(state: SessionState, viewModel: SessionViewModel) {
    val durationOptions = listOf(15, 20, 30, 45, 60)
    val energyEmojis = listOf("1", "2", "3", "4", "5")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Ready to practise?", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Text("How much time do you have?", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            durationOptions.forEach { min ->
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            (1..5).forEach { level ->
                FilterChip(
                    selected = state.energyLevel == level,
                    onClick = { viewModel.updateEnergyLevel(level) },
                    label = { Text("$level") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = state.specificFocus,
            onValueChange = { viewModel.updateSpecificFocus(it) },
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
            enabled = !state.isGeneratingPlan
        ) {
            if (state.isGeneratingPlan) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onTertiary)
                Spacer(modifier = Modifier.width(8.dp))
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
private fun PlanReviewPhase(
    state: SessionState,
    viewModel: SessionViewModel,
    onNavigateToChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Your Session Plan", style = MaterialTheme.typography.headlineMedium)
        Text(
            "${state.availableMinutes} minutes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        state.activities.forEachIndexed { index, activity ->
            ActivityCard(index, activity)
            if (index < state.activities.size - 1) {
                Box(
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .height(16.dp)
                        .width(2.dp)
                        .then(Modifier)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
private fun ActivityCard(index: Int, activity: PlannedActivity) {
    var expanded by remember { mutableStateOf(false) }

    val icon = when (activity.activityType) {
        "warmup" -> Icons.Default.FitnessCenter
        "scale" -> Icons.Default.Straighten
        "exercise" -> Icons.Default.Build
        "repertoire" -> Icons.Default.LibraryMusic
        "sight-reading" -> Icons.Default.Visibility
        "cooldown" -> Icons.Default.SelfImprovement
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
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        activity.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = if (expanded) Int.MAX_VALUE else 2
                    )
                }
                Text(
                    "${activity.plannedDurationMinutes}m",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (expanded && activity.rationale.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Why: ${activity.rationale}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ActivePhase(state: SessionState, viewModel: SessionViewModel) {
    val currentActivity = state.activities.getOrNull(state.currentActivityIndex)
    val activityDurationSec = (currentActivity?.plannedDurationMinutes ?: 0) * 60L
    val remainingSec = (activityDurationSec - state.activityElapsedSeconds).coerceAtLeast(0)

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
            Column {
                Text(
                    formatTime(state.elapsedSeconds),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatTime(remainingSec),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (remainingSec < 30) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground
                )
                Text("Remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }

        // Progress
        LinearProgressIndicator(
            progress = { (state.currentActivityIndex.toFloat() + (state.activityElapsedSeconds.toFloat() / activityDurationSec.coerceAtLeast(1))) / state.activities.size.coerceAtLeast(1) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.tertiary
        )

        // Current activity
        if (currentActivity != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "${state.currentActivityIndex + 1}/${state.activities.size}: ${currentActivity.activityType.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(currentActivity.description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audio feedback placeholders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Mic, contentDescription = "Listening", tint = MaterialTheme.colorScheme.tertiary)
                Text("Pitch: ${state.pitchAccuracy}", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Timer, contentDescription = "Rhythm", tint = MaterialTheme.colorScheme.tertiary)
                Text("Rhythm: ${state.rhythmAccuracy}", style = MaterialTheme.typography.bodySmall)
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
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }
            Button(
                onClick = { viewModel.togglePause() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isPaused) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(if (state.isPaused) "Resume" else "Pause")
            }
            Button(
                onClick = { viewModel.nextActivity() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Next")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = { viewModel.endSession() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("End Session")
        }
    }
}

@Composable
private fun DebriefPhase(
    state: SessionState,
    viewModel: SessionViewModel,
    onNavigateToDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Session Complete", style = MaterialTheme.typography.headlineMedium)
        Text(
            "${state.elapsedSeconds / 60} minutes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (state.isGeneratingDebrief) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Preparing your summary...", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
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
                viewModel.resetSession()
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

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
