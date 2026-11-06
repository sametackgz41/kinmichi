package app.tsuzuku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.tsuzuku.data.ConnectionRepository
import app.tsuzuku.data.GoalRepository
import app.tsuzuku.data.LibraryRepository
import app.tsuzuku.model.HabitGoalContributionMode
import app.tsuzuku.model.HabitGoalLink
import app.tsuzuku.model.HabitLibraryLink
import app.tsuzuku.model.TaskGoalLink
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConnectionViewModel(
    private val connectionRepository: ConnectionRepository,
    private val goalRepository: GoalRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    val habitGoalLinks: StateFlow<List<HabitGoalLink>> = connectionRepository.observeHabitGoalLinks()
        .flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taskGoalLinks: StateFlow<List<TaskGoalLink>> = connectionRepository.observeTaskGoalLinks()
        .flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitLibraryLinks: StateFlow<List<HabitLibraryLink>> = connectionRepository.observeHabitLibraryLinks()
        .flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun linkHabitGoal(habitId: Long, goalId: Long, mode: HabitGoalContributionMode = HabitGoalContributionMode.HabitAmount, value: Double = 1.0) {
        viewModelScope.launch {
            connectionRepository.linkHabitGoal(HabitGoalLink(habitId, goalId, mode, value))
        }
    }

    fun unlinkHabitGoal(habitId: Long, goalId: Long) {
        viewModelScope.launch {
            connectionRepository.unlinkHabitGoal(habitId, goalId)
        }
    }

    fun linkTaskGoal(taskId: Long, goalId: Long, value: Double = 1.0) {
        viewModelScope.launch {
            connectionRepository.linkTaskGoal(TaskGoalLink(taskId, goalId, value))
        }
    }

    fun unlinkTaskGoal(taskId: Long, goalId: Long) {
        viewModelScope.launch {
            connectionRepository.unlinkTaskGoal(taskId, goalId)
        }
    }

    fun linkHabitLibrary(habitId: Long, contentId: Long, multiplier: Double = 1.0) {
        viewModelScope.launch {
            connectionRepository.linkHabitLibrary(HabitLibraryLink(habitId, contentId, multiplier))
        }
    }

    fun unlinkHabitLibrary(habitId: Long, contentId: Long) {
        viewModelScope.launch {
            connectionRepository.unlinkHabitLibrary(habitId, contentId)
        }
    }

    fun onHabitProgressApplied(habitId: Long, amount: Double) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val goalLinks = habitGoalLinks.value.filter { it.habitId == habitId }
            goalLinks.forEach { link ->
                val delta = when (link.contributionMode) {
                    HabitGoalContributionMode.FixedValue -> link.contributionValue
                    HabitGoalContributionMode.HabitAmount -> amount * link.contributionValue
                    HabitGoalContributionMode.FocusDuration -> 0.0
                    HabitGoalContributionMode.Manual -> 0.0
                }
                if (delta > 0.0) {
                    goalRepository.changeProgress(link.goalId, delta)
                }
            }

            val libLinks = habitLibraryLinks.value.filter { it.habitId == habitId }
            libLinks.forEach { link ->
                val delta = amount * link.contributionMultiplier
                if (delta > 0.0) {
                    libraryRepository.changeProgress(link.contentId, delta)
                }
            }
        }
    }

    fun onTaskCompletedApplied(taskId: Long) {
        viewModelScope.launch {
            val links = taskGoalLinks.value.filter { it.taskId == taskId }
            links.forEach { link ->
                if (link.contributionValue > 0.0) {
                    goalRepository.changeProgress(link.goalId, link.contributionValue)
                }
            }
        }
    }

    companion object {
        fun factory(
            connectionRepository: ConnectionRepository,
            goalRepository: GoalRepository,
            libraryRepository: LibraryRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ConnectionViewModel(connectionRepository, goalRepository, libraryRepository) as T
        }
    }
}
