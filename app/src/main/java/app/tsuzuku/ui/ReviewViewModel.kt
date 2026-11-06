package app.tsuzuku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.tsuzuku.data.ReviewRepository
import app.tsuzuku.model.ActivityHeatmapDay
import app.tsuzuku.model.DailyReview
import app.tsuzuku.model.MonthlySummary
import app.tsuzuku.model.PersonalRecords
import app.tsuzuku.model.ReviewPeriodType
import app.tsuzuku.model.SkipReasonType
import app.tsuzuku.model.WeeklySummary
import app.tsuzuku.model.YearlySummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

class ReviewViewModel(
    private val repository: ReviewRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val initialWeekStart: LocalDate = today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    val dailyReviewToday: StateFlow<DailyReview?> = repository.observeDailyReview(today().toEpochDay()).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    /** Mood per day, for the mood insights card. */
    val moodsByDay: StateFlow<Map<LocalDate, Int>> = repository.observeDailyReviews()
        .map { reviews -> reviews.associate { LocalDate.ofEpochDay(it.epochDay) to it.mood } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** One-tap mood: keeps whatever else today's review already holds. */
    fun quickMood(mood: Int, existing: app.tsuzuku.model.DailyReview?) {
        viewModelScope.launch {
            repository.saveDailyReview(
                today().toEpochDay(),
                mood,
                existing?.energy ?: 3,
                existing?.productivity ?: 3,
                existing?.stress ?: 3,
                existing?.note.orEmpty(),
            )
        }
    }

    private val mutableSelectedWeekStart = MutableStateFlow(initialWeekStart)
    val selectedWeekStart: StateFlow<LocalDate> = mutableSelectedWeekStart

    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklySummary: StateFlow<WeeklySummary> = mutableSelectedWeekStart.flatMapLatest { weekStart ->
        repository.observeWeeklySummary(weekStart)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeeklySummary(
            weekStart = initialWeekStart,
            habitCompletionRate = 0f,
            prevHabitCompletionRate = null,
            bestHabit = null,
            strugglingHabit = null,
            plannedTasksCount = 0,
            completedTasksCount = 0,
            carriedTasksCount = 0,
            focusDurationMinutes = 0,
            goalProgressCount = 0,
        ),
    )

    private val mutableSelectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = mutableSelectedMonth

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlySummary: StateFlow<MonthlySummary> = mutableSelectedMonth.flatMapLatest { month ->
        repository.observeMonthlySummary(month)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MonthlySummary(
            yearMonth = YearMonth.now(),
            habitCompletionRate = 0f,
            focusDurationMinutes = 0,
            completedTasksCount = 0,
            topHabits = emptyList(),
        ),
    )

    private val mutableSelectedYear = MutableStateFlow(today().year)
    val selectedYear: StateFlow<Int> = mutableSelectedYear

    @OptIn(ExperimentalCoroutinesApi::class)
    val yearlySummary: StateFlow<YearlySummary> = mutableSelectedYear.flatMapLatest { year ->
        repository.observeYearlySummary(year)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = YearlySummary(
            year = today().year,
            totalHabitCompletions = 0,
            totalFocusMinutes = 0,
            completedGoalsCount = 0,
            completedLibraryItemsCount = 0,
            bestStreakDays = 0,
        ),
    )

    val personalRecords: StateFlow<PersonalRecords> = repository.observePersonalRecords().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PersonalRecords(
            longestHabitStreak = 0,
            longestHabitName = "—",
            mostProductiveDayName = "—",
            mostProductiveDayCount = 0,
            totalFocusHours = 0,
            totalHabitCompletions = 0,
            totalGoalsCompleted = 0,
            totalLibraryCompleted = 0,
            milestones = emptyList(),
        ),
    )

    private val mutableHeatmapHabitId = MutableStateFlow<Long?>(null)
    val selectedHeatmapHabitId: StateFlow<Long?> = mutableHeatmapHabitId

    @OptIn(ExperimentalCoroutinesApi::class)
    val heatmapDays: StateFlow<List<ActivityHeatmapDay>> = mutableHeatmapHabitId.flatMapLatest { habitId ->
        repository.observeActivityHeatmap(habitId = habitId, days = 112) // 16 weeks * 7 days
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val skipReasonStats: StateFlow<Map<SkipReasonType, Int>> = repository.observeSkipReasonStatistics().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap(),
    )

    fun setWeek(weekStart: LocalDate) {
        mutableSelectedWeekStart.value = weekStart
    }

    fun previousWeek() {
        mutableSelectedWeekStart.value = mutableSelectedWeekStart.value.minusWeeks(1)
    }

    fun nextWeek() {
        mutableSelectedWeekStart.value = mutableSelectedWeekStart.value.plusWeeks(1)
    }

    fun setMonth(month: YearMonth) {
        mutableSelectedMonth.value = month
    }

    fun previousMonth() {
        mutableSelectedMonth.value = mutableSelectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        mutableSelectedMonth.value = mutableSelectedMonth.value.plusMonths(1)
    }

    fun setYear(year: Int) {
        mutableSelectedYear.value = year
    }

    fun previousYear() {
        mutableSelectedYear.value -= 1
    }

    fun nextYear() {
        mutableSelectedYear.value += 1
    }

    fun setHeatmapHabitId(habitId: Long?) {
        mutableHeatmapHabitId.value = habitId
    }

    fun saveDailyReview(
        mood: Int,
        energy: Int,
        productivity: Int,
        stress: Int,
        note: String,
        epochDay: Long = today().toEpochDay(),
    ) {
        viewModelScope.launch {
            repository.saveDailyReview(epochDay, mood, energy, productivity, stress, note)
        }
    }

    fun saveWeeklyNote(weekStart: LocalDate, note: String) {
        viewModelScope.launch {
            repository.saveWeeklyReview(weekStart, note)
        }
    }

    fun savePeriodNote(periodType: ReviewPeriodType, periodKey: String, note: String) {
        viewModelScope.launch {
            repository.savePeriodReview(periodType, periodKey, note)
        }
    }

    fun savePeriodReview(periodType: ReviewPeriodType, periodKey: String, note: String) {
        savePeriodNote(periodType, periodKey, note)
    }

    companion object {
        fun factory(repository: ReviewRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ReviewViewModel(repository) as T
            }
    }
}
