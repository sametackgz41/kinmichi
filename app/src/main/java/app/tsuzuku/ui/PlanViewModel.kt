package app.tsuzuku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.tsuzuku.data.PlanRepository
import app.tsuzuku.model.PlanTask
import app.tsuzuku.model.PlanTaskDraft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanViewModel(private val repository: PlanRepository) : ViewModel() {
    val tasks = repository.observeTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val templates = repository.observeTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun create(draft: PlanTaskDraft, onCreated: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repository.create(draft)
        onCreated(id)
    }
    fun update(id: Long, draft: PlanTaskDraft) = viewModelScope.launch { repository.update(id, draft) }
    fun toggle(task: PlanTask) = viewModelScope.launch { repository.toggle(task) }
    fun move(id: Long, day: Long?) = viewModelScope.launch { repository.move(id, day) }
    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
    fun duplicate(task: PlanTask) = viewModelScope.launch { repository.duplicate(task) }
    fun reorder(id: Long, direction: Int) = viewModelScope.launch { repository.reorder(id, direction) }
    fun bulkComplete(items: List<PlanTask>) = viewModelScope.launch { items.filterNot(PlanTask::completed).forEach { repository.toggle(it) } }
    fun bulkMove(ids: Set<Long>, day: Long?) = viewModelScope.launch { ids.forEach { repository.move(it, day) } }
    fun bulkDelete(ids: Set<Long>) = viewModelScope.launch { ids.forEach { repository.delete(it) } }
    fun bulkAddTags(items: List<PlanTask>, tags: String) = viewModelScope.launch { repository.addTags(items, tags) }
    fun addChecklistItem(taskId: Long, title: String) = viewModelScope.launch { repository.addChecklistItem(taskId, title) }
    fun toggleChecklistItem(id: Long) = viewModelScope.launch { repository.toggleChecklistItem(id) }
    fun deleteChecklistItem(id: Long) = viewModelScope.launch { repository.deleteChecklistItem(id) }
    fun setActualMinutes(id: Long, minutes: Int?) = viewModelScope.launch { repository.setActualMinutes(id, minutes) }
    fun addActualMinutes(id: Long, minutes: Int) = viewModelScope.launch { repository.addActualMinutes(id, minutes) }
    fun saveTemplate(draft: PlanTaskDraft) = viewModelScope.launch { repository.saveTemplate(draft) }
    fun createFromTemplate(template: app.tsuzuku.model.PlanTemplate) = viewModelScope.launch { repository.createFromTemplate(template) }
    fun deleteTemplate(id: Long) = viewModelScope.launch { repository.deleteTemplate(id) }

    companion object {
        fun factory(repository: PlanRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PlanViewModel(repository) as T
        }
    }
}
