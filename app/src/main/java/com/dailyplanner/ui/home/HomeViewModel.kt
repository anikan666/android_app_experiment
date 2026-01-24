package com.dailyplanner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyplanner.data.local.entity.TaskEntity
import com.dailyplanner.data.repository.TaskRepository
import com.dailyplanner.data.remote.GmailService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    // In a real app, implementation needed to get authenticated Gmail service
    // private val gmailService: GmailService 
) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun refreshTasks() {
        viewModelScope.launch {
            try {
                // TODO: Need actual Gmail instance from Auth flow.
                // For now, this is a placeholder to show where logic goes.
                // taskRepository.syncTasksFromEmails(gmailServiceInstance)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onTaskClicked(task: TaskEntity) {
        // Navigate to detail or edit
    }
}
