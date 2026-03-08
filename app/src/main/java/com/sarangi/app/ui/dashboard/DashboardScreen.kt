package com.sarangi.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTeacherBriefing: () -> Unit
) {
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
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))

        // Week overview
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("This Week", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                // Streak dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                    state.weekDays.forEachIndexed { index, completed ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(dayLabels[index], style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (completed) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("${state.weeklySessionCount}", "sessions")
                    StatItem("${state.totalMinutesThisWeek}", "minutes")
                    StatItem("${state.currentStreak}", "day streak")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent sessions
        if (state.recentSessions.isNotEmpty()) {
            Text("Recent Sessions", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            state.recentSessions.forEach { session ->
                SessionCard(session)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Technical observations
        if (state.unresolvedObservations.isNotEmpty()) {
            Text("Active Focus Areas", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            state.unresolvedObservations.forEach { obs ->
                ObservationCard(obs, onResolve = { viewModel.resolveObservation(obs) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Teacher briefing
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToTeacherBriefing,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("For Your Next Lesson", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${state.undiscussedBriefingCount} items to discuss",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun SessionCard(session: com.sarangi.core.database.entity.PracticeSession) {
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(dateFormat.format(Date(session.startedAt)), style = MaterialTheme.typography.titleSmall)
                Text(
                    "${session.actualDurationMinutes ?: session.plannedDurationMinutes} min",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            session.completionRate?.let { rate ->
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { rate },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            session.notes?.let { notes ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(notes, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun ObservationCard(
    observation: com.sarangi.core.database.entity.TechnicalObservation,
    onResolve: () -> Unit
) {
    val severityColor = when (observation.severity) {
        "significant" -> MaterialTheme.colorScheme.error
        "mild" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(severityColor)
                    .offset(y = 6.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(observation.category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium)
                Text(observation.observation, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onResolve) {
                Text("Resolved", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
