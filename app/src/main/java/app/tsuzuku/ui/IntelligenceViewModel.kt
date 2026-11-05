package app.tsuzuku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.tsuzuku.data.intelligence.IntelligenceRepository
import app.tsuzuku.data.preferences.AppSettings
import app.tsuzuku.model.AskTsuzukuMessage
import app.tsuzuku.model.Insight
import app.tsuzuku.model.MessageSender
import app.tsuzuku.model.PlanDraftProposal
import app.tsuzuku.model.PlanMode
import app.tsuzuku.model.Recommendation
import app.tsuzuku.model.WeeklyAiCommentary
import app.tsuzuku.model.WeeklySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IntelligenceViewModel(
    private val repository: IntelligenceRepository,
) : ViewModel() {

    val insights: StateFlow<List<Insight>> = repository.observeInsights().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val todayInsights: StateFlow<List<Insight>> = repository.observeTodayInsights().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val recommendations: StateFlow<List<Recommendation>> = insights.map { list ->
        list.mapNotNull { it.recommendation }.distinctBy { it.id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _chatMessages = MutableStateFlow<List<AskTsuzukuMessage>>(emptyList())
    val chatMessages: StateFlow<List<AskTsuzukuMessage>> = _chatMessages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activePlanProposal = MutableStateFlow<PlanDraftProposal?>(null)
    val activePlanProposal: StateFlow<PlanDraftProposal?> = _activePlanProposal.asStateFlow()

    private val _weeklyCommentary = MutableStateFlow<WeeklyAiCommentary?>(null)
    val weeklyCommentary: StateFlow<WeeklyAiCommentary?> = _weeklyCommentary.asStateFlow()

    fun askQuestion(question: String, settings: AppSettings) {
        if (question.isBlank()) return
        val userMsg = AskTsuzukuMessage(sender = MessageSender.User, content = question.trim())
        val updatedHistory = _chatMessages.value + userMsg
        _chatMessages.value = updatedHistory
        _isGenerating.value = true

        viewModelScope.launch {
            try {
                val result = repository.answerQuestion(question, settings, updatedHistory)
                result.onSuccess { responseMsg ->
                    _chatMessages.value = _chatMessages.value + responseMsg
                }.onFailure { err ->
                    _chatMessages.value = _chatMessages.value + AskTsuzukuMessage(
                        sender = MessageSender.Assistant,
                        content = "Cevap oluşturulurken bir sorun oluştu: ${err.localizedMessage ?: "Bilinmeyen hata"}",
                    )
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generatePlanDraft(
        prompt: String = "",
        mode: PlanMode = PlanMode.Balanced,
        settings: AppSettings,
    ) {
        _isGenerating.value = true
        viewModelScope.launch {
            try {
                val result = repository.generatePlanDraft(prompt, mode, settings)
                result.onSuccess { proposal ->
                    _activePlanProposal.value = proposal
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun toggleDraftTaskSelection(index: Int) {
        val current = _activePlanProposal.value ?: return
        if (index !in current.proposedTasks.indices) return
        val updated = current.proposedTasks.toMutableList()
        val item = updated[index]
        updated[index] = item.copy(isSelected = !item.isSelected)
        _activePlanProposal.value = current.copy(proposedTasks = updated)
    }

    fun updateDraftTaskTitle(index: Int, newTitle: String) {
        val current = _activePlanProposal.value ?: return
        if (index !in current.proposedTasks.indices) return
        val updated = current.proposedTasks.toMutableList()
        updated[index] = updated[index].copy(title = newTitle)
        _activePlanProposal.value = current.copy(proposedTasks = updated)
    }

    fun updateDraftTaskDuration(index: Int, deltaMinutes: Int) {
        val current = _activePlanProposal.value ?: return
        if (index !in current.proposedTasks.indices) return
        val updated = current.proposedTasks.toMutableList()
        val item = updated[index]
        val newDuration = (item.estimatedMinutes + deltaMinutes).coerceIn(5, 240)
        val newDue = item.startMinutes?.let { it + newDuration } ?: item.dueMinutes
        updated[index] = item.copy(estimatedMinutes = newDuration, dueMinutes = newDue)
        _activePlanProposal.value = current.copy(proposedTasks = updated)
    }

    fun addDraftSubtask(taskIndex: Int, subtask: String) {
        if (subtask.isBlank()) return
        val current = _activePlanProposal.value ?: return
        if (taskIndex !in current.proposedTasks.indices) return
        val updated = current.proposedTasks.toMutableList()
        val item = updated[taskIndex]
        updated[taskIndex] = item.copy(subtasks = item.subtasks + subtask.trim())
        _activePlanProposal.value = current.copy(proposedTasks = updated)
    }

    fun removeDraftSubtask(taskIndex: Int, subtaskIndex: Int) {
        val current = _activePlanProposal.value ?: return
        if (taskIndex !in current.proposedTasks.indices) return
        val updated = current.proposedTasks.toMutableList()
        val item = updated[taskIndex]
        if (subtaskIndex in item.subtasks.indices) {
            val updatedSubtasks = item.subtasks.toMutableList().apply { removeAt(subtaskIndex) }
            updated[taskIndex] = item.copy(subtasks = updatedSubtasks)
            _activePlanProposal.value = current.copy(proposedTasks = updated)
        }
    }

    fun fetchWeeklyCommentary(summary: WeeklySummary, settings: AppSettings) {
        viewModelScope.launch {
            val res = repository.generateWeeklyReviewCommentary(summary, settings)
            res.onSuccess { commentary ->
                _weeklyCommentary.value = commentary
            }
        }
    }

    fun explainInsight(insight: Insight, settings: AppSettings, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.explainInsight(insight, settings)
            onResult(res.getOrDefault(insight.whyWeSayThis))
        }
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }

    fun dismissPlanProposal() {
        _activePlanProposal.value = null
    }

    fun applyRecommendation(recommendation: Recommendation, onApplied: () -> Unit = {}) {
        viewModelScope.launch {
            repository.applyRecommendation(recommendation)
            onApplied()
        }
    }

    companion object {
        fun factory(repository: IntelligenceRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    IntelligenceViewModel(repository) as T
            }
    }
}
