package com.sarangi.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sarangi.core.database.entity.TeacherBriefingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherBriefingScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items = state.briefingItems

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("For Your Next Lesson") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        TextButton(onClick = {
                            viewModel.markAllBriefingsDiscussed()
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark All Discussed")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Nothing to discuss",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Items will appear here as Sarangi identifies\nthings to bring up with your teacher.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                val grouped = items.groupBy { it.category }

                grouped["technique-issue"]?.let { issues ->
                    BriefingSection("Technique Issues", issues)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                grouped["question"]?.let { questions ->
                    BriefingSection("Questions for Teacher", questions)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                grouped["progress-note"]?.let { notes ->
                    BriefingSection("Progress Notes", notes)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    "Show this screen to your teacher at your next lesson.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun BriefingSection(title: String, items: List<TeacherBriefingItem>) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    items.forEach { item ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
