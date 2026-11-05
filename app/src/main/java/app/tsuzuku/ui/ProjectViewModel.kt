package app.tsuzuku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.tsuzuku.data.ProjectRepository
import app.tsuzuku.model.Project
import app.tsuzuku.model.ProjectDetail
import app.tsuzuku.model.ProjectDraft
import app.tsuzuku.model.ProjectStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ProjectViewModel(
    private val repository: ProjectRepository,
) : ViewModel() {

    val projects: StateFlow<List<Project>> = repository.observeProjects().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun detail(projectId: Long): Flow<ProjectDetail?> = repository.observeProjectDetail(projectId)

    fun createProject(draft: ProjectDraft, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.addProject(draft)
            onCreated(id)
        }
    }

    fun updateProject(id: Long, draft: ProjectDraft, onUpdated: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateProject(id, draft)
            onUpdated()
        }
    }

    fun setStatus(id: Long, status: ProjectStatus) {
        viewModelScope.launch {
            repository.setProjectStatus(id, status)
        }
    }

    fun deleteProject(id: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteProject(id)
            onDeleted()
        }
    }

    fun addMilestone(projectId: Long, title: String, dueDate: LocalDate? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addMilestone(projectId, title, dueDate)
        }
    }

    fun toggleMilestone(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleMilestone(id, completed)
        }
    }

    fun deleteMilestone(id: Long) {
        viewModelScope.launch {
            repository.deleteMilestone(id)
        }
    }

    fun linkTask(projectId: Long, taskId: Long) {
        viewModelScope.launch {
            repository.linkTask(projectId, taskId)
        }
    }

    fun unlinkTask(projectId: Long, taskId: Long) {
        viewModelScope.launch {
            repository.unlinkTask(projectId, taskId)
        }
    }

    fun linkGoal(projectId: Long, goalId: Long) {
        viewModelScope.launch {
            repository.linkGoal(projectId, goalId)
        }
    }

    fun unlinkGoal(projectId: Long, goalId: Long) {
        viewModelScope.launch {
            repository.unlinkGoal(projectId, goalId)
        }
    }

    companion object {
        fun factory(repository: ProjectRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProjectViewModel(repository) as T
            }
    }
}
