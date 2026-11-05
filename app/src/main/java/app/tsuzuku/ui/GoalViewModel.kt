package app.tsuzuku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.tsuzuku.data.GoalRepository
import app.tsuzuku.model.Goal
import app.tsuzuku.model.GoalDraft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalViewModel(private val repository: GoalRepository) : ViewModel() {
    val goals: StateFlow<List<Goal>> = repository.observeGoals().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun saveGoal(id: Long?, draft: GoalDraft, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            if (id == null) onSaved(repository.addGoal(draft))
            else {
                repository.updateGoal(id, draft)
                onSaved(id)
            }
        }
    }

    fun changeProgress(id: Long, delta: Double) = viewModelScope.launch { repository.changeProgress(id, delta) }
    fun setCompleted(id: Long, completed: Boolean) = viewModelScope.launch { repository.setCompleted(id, completed) }
    fun archiveGoal(id: Long, onDone: () -> Unit) = viewModelScope.launch { repository.archiveGoal(id); onDone() }
    fun deleteGoal(id: Long, onDone: () -> Unit) = viewModelScope.launch { repository.deleteGoal(id); onDone() }

    companion object {
        fun factory(repository: GoalRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = GoalViewModel(repository) as T
        }
    }
}
