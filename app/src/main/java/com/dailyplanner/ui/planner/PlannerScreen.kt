package com.dailyplanner.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailyplanner.ui.components.TaskCard

@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top: Calendar / Timeline (Simplified)
        Text(
            text = "Today's Schedule",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Placeholder: List of events
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(state.calendarEvents) { event ->
                    EventItem(title = event.title, time = "TODO: Time")
                }
                if (state.calendarEvents.isEmpty()) {
                    item {
                        Text("No events scheduled using Google Calendar yet.")
                    }
                }
            }
        }

        Divider()

        // Bottom: Unscheduled Tasks
        Text(
            text = "Unscheduled Tasks",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            items(state.unscheduledTasks) { task ->
                TaskCard(
                    task = task,
                    onClick = { /* Open scheduling dialog */ },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EventItem(title: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.White, shape = MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        Text(text = time, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
        Text(text = title)
    }
}
