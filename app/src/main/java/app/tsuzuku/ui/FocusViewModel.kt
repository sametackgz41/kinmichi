package app.tsuzuku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.tsuzuku.data.FocusRepository
import app.tsuzuku.data.GoalRepository
import app.tsuzuku.data.HabitRepository
import app.tsuzuku.data.PlanRepository
import app.tsuzuku.model.FocusMode
import app.tsuzuku.model.FocusSession
import app.tsuzuku.model.FocusTargetType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveFocusState(
    val mode: FocusMode = FocusMode.Pomodoro,
    val targetType: FocusTargetType = FocusTargetType.Free,
    val targetId: Long? = null,
    val targetTitle: String = "",
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Int = 0,
    val targetSeconds: Int = 25 * 60,
    val sessionId: Long = 0L,
) {
    val remainingSeconds: Int
        get() = (targetSeconds - elapsedSeconds).coerceAtLeast(0)

    val progressFraction: Float
        get() = if (targetSeconds <= 0) 0f else (elapsedSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
}

class FocusViewModel(
    private val focusRepository: FocusRepository,
    private val planRepository: PlanRepository,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
) : ViewModel() {

    private val _activeState = MutableStateFlow<ActiveFocusState?>(null)
    val activeState: StateFlow<ActiveFocusState?> = _activeState.asStateFlow()

    val sessions: StateFlow<List<FocusSession>> = focusRepository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var timerJob: Job? = null

    fun startSession(
        mode: FocusMode,
        targetType: FocusTargetType = FocusTargetType.Free,
        targetId: Long? = null,
        targetTitle: String = "",
        customDurationMinutes: Int = 25,
    ) {
        val targetSeconds = when (mode) {
            FocusMode.Pomodoro -> 25 * 60
            FocusMode.Stopwatch -> 0
            FocusMode.Custom -> customDurationMinutes * 60
        }

        viewModelScope.launch {
            val sessionId = focusRepository.start(
                mode = mode,
                taskId = if (targetType == FocusTargetType.Task) targetId else null,
                habitId = if (targetType == FocusTargetType.Habit) targetId else null,
                goalId = if (targetType == FocusTargetType.Goal) targetId else null,
            )

            _activeState.value = ActiveFocusState(
                mode = mode,
                targetType = targetType,
                targetId = targetId,
                targetTitle = targetTitle,
                isRunning = true,
                isPaused = false,
                elapsedSeconds = 0,
                targetSeconds = targetSeconds,
                sessionId = sessionId,
            )

            startTimerLoop()
        }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _activeState.value ?: break
                if (current.isRunning && !current.isPaused) {
                    val nextElapsed = current.elapsedSeconds + 1
                    val autoFinish = current.mode != FocusMode.Stopwatch && nextElapsed >= current.targetSeconds
                    if (autoFinish) {
                        _activeState.value = current.copy(elapsedSeconds = current.targetSeconds)
                        finishSession(completed = true)
                        break
                    } else {
                        _activeState.value = current.copy(elapsedSeconds = nextElapsed)
                    }
                }
            }
        }
    }

    fun pauseSession() {
        _activeState.value = _activeState.value?.copy(isPaused = true)
    }

    fun resumeSession() {
        _activeState.value = _activeState.value?.copy(isPaused = false)
    }

    fun finishSession(completed: Boolean = true) {
        val current = _activeState.value ?: return
        timerJob?.cancel()
        timerJob = null

        val durationMinutes = (current.elapsedSeconds / 60).coerceAtLeast(if (completed && current.mode != FocusMode.Stopwatch) current.targetSeconds / 60 else 1)

        viewModelScope.launch {
            focusRepository.finish(current.sessionId, completed = completed, interrupted = !completed)

            if (completed && durationMinutes > 0) {
                when (current.targetType) {
                    FocusTargetType.Task -> current.targetId?.let { taskId ->
                        planRepository.addActualMinutes(taskId, durationMinutes)
                    }
                    FocusTargetType.Habit -> current.targetId?.let { habitId ->
                        habitRepository.addProgress(habitId, durationMinutes.toDouble())
                    }
                    FocusTargetType.Goal -> current.targetId?.let { goalId ->
                        goalRepository.changeProgress(goalId, durationMinutes.toDouble())
                    }
                    FocusTargetType.Free -> {}
                }
            }
            _activeState.value = null
        }
    }

    fun cancelSession() {
        val current = _activeState.value ?: return
        timerJob?.cancel()
        timerJob = null
        viewModelScope.launch {
            focusRepository.finish(current.sessionId, completed = false, interrupted = true)
            _activeState.value = null
        }
    }

    companion object {
        fun factory(
            focusRepository: FocusRepository,
            planRepository: PlanRepository,
            habitRepository: HabitRepository,
            goalRepository: GoalRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FocusViewModel(focusRepository, planRepository, habitRepository, goalRepository) as T
        }
    }
}
