package app.tsuzuku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.tsuzuku.data.HabitRepository
import app.tsuzuku.data.preferences.StoredTimerSession
import app.tsuzuku.data.preferences.TimerSessionStore
import app.tsuzuku.model.HabitDraft
import app.tsuzuku.model.HabitHistorySnapshot
import app.tsuzuku.model.HabitLevel
import app.tsuzuku.model.TodayHabit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.ceil

data class HabitTimerState(
    val habitId: Long? = null,
    val elapsedSeconds: Long = 0,
    val maxSeconds: Long = 0,
) {
    val isRunning: Boolean get() = habitId != null
}

class TodayViewModel(
    private val repository: HabitRepository,
    private val timerStore: TimerSessionStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
    private val onDataChanged: () -> Unit = {},
) : ViewModel() {
    val habits: StateFlow<List<TodayHabit>> = repository.observeToday().flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val history: StateFlow<HabitHistorySnapshot> = repository.observeHistory().flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitHistorySnapshot(emptyList(), emptyList()),
    )

    private val mutableTimer = MutableStateFlow(HabitTimerState())
    val timer: StateFlow<HabitTimerState> = mutableTimer.asStateFlow()
    private var timerJob: Job? = null

    init {
        timerStore.load()?.let { session ->
            if (session.localDate == today()) {
                runTimer(session)
            } else {
                timerStore.clear()
            }
        }
    }

    fun toggleHabit(id: Long) {
        viewModelScope.launch { repository.toggleHabit(id); onDataChanged() }
    }

    fun setFeeling(habitId: Long, feeling: app.tsuzuku.model.Feeling?) {
        viewModelScope.launch { repository.setFeeling(habitId, java.time.LocalDate.now(), feeling) }
    }

    fun completeTinyVersion(id: Long) {
        viewModelScope.launch { repository.completeTinyVersion(id); onDataChanged() }
    }

    fun incrementHabit(id: Long) {
        viewModelScope.launch { repository.incrementHabit(id); onDataChanged() }
    }

    fun decrementHabit(id: Long) {
        viewModelScope.launch { repository.decrementHabit(id); onDataChanged() }
    }

    fun toggleHabitOnDate(id: Long, date: LocalDate) {
        viewModelScope.launch { repository.toggleHabitOnDate(id, date); onDataChanged() }
    }

    fun incrementHabitOnDate(id: Long, date: LocalDate) {
        viewModelScope.launch { repository.incrementHabitOnDate(id, date); onDataChanged() }
    }

    fun decrementHabitOnDate(id: Long, date: LocalDate) {
        viewModelScope.launch { repository.decrementHabitOnDate(id, date); onDataChanged() }
    }

    fun skipHabit(id: Long, reason: String?) {
        viewModelScope.launch { repository.skipHabit(id, reason); onDataChanged() }
    }

    fun skipHabitOnDate(id: Long, date: LocalDate, reason: String?) {
        viewModelScope.launch { repository.skipHabitOnDate(id, date, reason); onDataChanged() }
    }

    fun addHabit(draft: HabitDraft, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            onSaved(repository.addHabit(draft))
            onDataChanged()
        }
    }

    fun setWeeklyGoal(id: Long, timesPerWeek: Int) {
        viewModelScope.launch {
            repository.setWeeklyGoal(id, timesPerWeek)
            onDataChanged()
        }
    }

    fun setHabitTarget(id: Long, target: Double) {
        viewModelScope.launch {
            repository.setHabitTarget(id, target)
            onDataChanged()
        }
    }

    fun updateHabit(id: Long, draft: HabitDraft, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.updateHabit(id, draft)
            onDataChanged()
            onSaved()
        }
    }

    fun saveHabitNote(id: Long, note: String) {
        viewModelScope.launch { repository.updateHabitNote(id, note); onDataChanged() }
    }

    fun archiveHabit(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.archiveHabit(id)
            onDataChanged()
            onDone()
        }
    }

    fun deleteHabit(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.trashHabit(id)
            onDataChanged()
            onDone()
        }
    }

    fun duplicateHabit(id: Long, onDone: (Long?) -> Unit) {
        viewModelScope.launch {
            val newId = repository.duplicateHabit(id)
            onDataChanged()
            onDone(newId)
        }
    }

    fun moveHabit(id: Long, direction: Int) {
        viewModelScope.launch { repository.moveHabit(id, direction); onDataChanged() }
    }

    fun reorderHabits(orderedIds: List<Long>) {
        viewModelScope.launch { repository.reorderHabits(orderedIds); onDataChanged() }
    }

    fun observeHabitLevels(habitId: Long): Flow<List<HabitLevel>> = repository.observeHabitLevels(habitId)

    fun saveHabitLevels(habitId: Long, levels: List<HabitLevel>) {
        viewModelScope.launch {
            repository.saveHabitLevels(habitId, levels)
            onDataChanged()
        }
    }

    fun advanceHabitLevel(habitId: Long, targetLevelNumber: Int) {
        viewModelScope.launch {
            repository.advanceHabitLevel(habitId, targetLevelNumber)
            onDataChanged()
        }
    }

    fun toggleTimer(habitId: Long) {
        viewModelScope.launch {
            val active = timerStore.load()
            if (active?.habitId == habitId) {
                finishTimer(active)
                return@launch
            }
            if (active != null) finishTimer(active)
            val item = habits.value.firstOrNull { it.habit.id == habitId } ?: return@launch
            val remainingSeconds = ceil((item.habit.target - item.progress).coerceAtLeast(0.0) * 60.0).toLong()
            if (remainingSeconds <= 0) return@launch
            val session = StoredTimerSession(
                habitId = habitId,
                startedAtMillis = now(),
                maxSeconds = remainingSeconds,
                localDate = today(),
            )
            timerStore.save(session)
            runTimer(session)
        }
    }

    private fun runTimer(session: StoredTimerSession) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = elapsedSeconds(session, now())
                mutableTimer.value = HabitTimerState(session.habitId, elapsed, session.maxSeconds)
                if (elapsed >= session.maxSeconds) {
                    finishTimer(session, cancelTicker = false)
                    break
                }
                delay(1_000)
            }
        }
    }

    private suspend fun finishTimer(session: StoredTimerSession, cancelTicker: Boolean = true) {
        if (cancelTicker) timerJob?.cancel()
        timerJob = null
        val elapsed = elapsedSeconds(session, now())
        if (elapsed > 0) repository.addProgress(session.habitId, elapsed / 60.0)
        if (elapsed > 0) onDataChanged()
        timerStore.clear()
        mutableTimer.value = HabitTimerState()
    }

    companion object {
        fun factory(repository: HabitRepository, timerStore: TimerSessionStore, onDataChanged: () -> Unit = {}): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TodayViewModel(repository, timerStore, onDataChanged = onDataChanged) as T
            }
    }
}

internal fun elapsedSeconds(session: StoredTimerSession, nowMillis: Long): Long =
    ((nowMillis - session.startedAtMillis).coerceAtLeast(0L) / 1_000L).coerceAtMost(session.maxSeconds)
