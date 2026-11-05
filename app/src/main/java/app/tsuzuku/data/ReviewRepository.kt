package app.tsuzuku.data

import app.tsuzuku.data.local.DailyReviewEntity
import app.tsuzuku.data.local.FocusDao
import app.tsuzuku.data.local.GoalDao
import app.tsuzuku.data.local.HabitDao
import app.tsuzuku.data.local.HabitEntity
import app.tsuzuku.data.local.HabitEntryEntity
import app.tsuzuku.data.local.LibraryDao
import app.tsuzuku.data.local.PeriodReviewEntity
import app.tsuzuku.data.local.PlanDao
import app.tsuzuku.data.local.ReviewDao
import app.tsuzuku.model.ActivityHeatmapDay
import app.tsuzuku.model.ContentStatus
import app.tsuzuku.model.DailyReview
import app.tsuzuku.model.GoalStatus
import app.tsuzuku.model.HabitDayStatus
import app.tsuzuku.model.MonthlySummary
import app.tsuzuku.model.PeriodReview
import app.tsuzuku.model.PersonalMilestone
import app.tsuzuku.model.PersonalRecords
import app.tsuzuku.model.ReviewPeriodType
import app.tsuzuku.model.SkipReasonType
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.model.WeeklySummary
import app.tsuzuku.model.YearlySummary
import app.tsuzuku.model.isScheduledOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

interface ReviewRepository {
    fun observeDailyReview(epochDay: Long): Flow<DailyReview?>
    fun observeDailyReviews(): Flow<List<DailyReview>>
    suspend fun saveDailyReview(epochDay: Long, mood: Int, energy: Int, productivity: Int, stress: Int, note: String)
    suspend fun deleteDailyReview(epochDay: Long)

    fun observePeriodReviews(): Flow<List<PeriodReview>>
    suspend fun savePeriodReview(periodType: ReviewPeriodType, periodKey: String, note: String)
    suspend fun saveWeeklyReview(weekStart: LocalDate, note: String)

    fun observeWeeklySummary(weekStart: LocalDate): Flow<WeeklySummary>
    fun observeMonthlySummary(yearMonth: YearMonth): Flow<MonthlySummary>
    fun observeYearlySummary(year: Int): Flow<YearlySummary>
    fun observePersonalRecords(): Flow<PersonalRecords>
    fun observeActivityHeatmap(habitId: Long? = null, days: Int = 120): Flow<List<ActivityHeatmapDay>>
    fun observeSkipReasonStatistics(): Flow<Map<SkipReasonType, Int>>
}

class RoomReviewRepository(
    private val reviewDao: ReviewDao,
    private val habitDao: HabitDao,
    private val habitRepository: HabitRepository,
    private val planDao: PlanDao,
    private val goalDao: GoalDao,
    private val focusDao: FocusDao,
    private val libraryDao: LibraryDao,
    private val now: () -> Long = System::currentTimeMillis,
) : ReviewRepository {

    override fun observeDailyReview(epochDay: Long): Flow<DailyReview?> =
        reviewDao.observeDailyReview(epochDay).map { it?.toDomain() }

    override fun observeDailyReviews(): Flow<List<DailyReview>> =
        reviewDao.observeDailyReviews().map { list -> list.map(DailyReviewEntity::toDomain) }

    override suspend fun saveDailyReview(
        epochDay: Long,
        mood: Int,
        energy: Int,
        productivity: Int,
        stress: Int,
        note: String,
    ) {
        val timestamp = now()
        reviewDao.upsertDailyReview(
            DailyReviewEntity(
                epochDay = epochDay,
                mood = mood.coerceIn(1, 5),
                energy = energy.coerceIn(1, 5),
                productivity = productivity.coerceIn(1, 5),
                stress = stress.coerceIn(1, 5),
                note = note.trim(),
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    override suspend fun deleteDailyReview(epochDay: Long) {
        reviewDao.deleteDailyReview(epochDay)
    }

    override fun observePeriodReviews(): Flow<List<PeriodReview>> =
        reviewDao.observePeriodReviews().map { list -> list.map(PeriodReviewEntity::toDomain) }

    override suspend fun savePeriodReview(periodType: ReviewPeriodType, periodKey: String, note: String) {
        val timestamp = now()
        reviewDao.upsertPeriodReview(
            PeriodReviewEntity(
                periodType = periodType.name,
                periodKey = periodKey.trim(),
                note = note.trim(),
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    override suspend fun saveWeeklyReview(weekStart: LocalDate, note: String) {
        habitRepository.saveWeeklyReview(weekStart, note)
    }

    override fun observeWeeklySummary(weekStart: LocalDate): Flow<WeeklySummary> {
        val weekEnd = weekStart.plusDays(6)
        val prevWeekStart = weekStart.minusWeeks(1)
        val prevWeekEnd = prevWeekStart.plusDays(6)

        return combine(
            habitRepository.observeHistory(),
            planDao.observeTasks(),
            focusDao.observeSessions(),
            goalDao.observeGoals(),
            habitRepository.observeWeeklyReviews(),
        ) { history, tasks, focusSessions, goals, weeklyReviews ->
            val dates = generateSequence(weekStart) { it.plusDays(1) }.take(7).toList()
            val prevDates = generateSequence(prevWeekStart) { it.plusDays(1) }.take(7).toList()

            // Weekly habit completion rate
            val weekSummaries = dates.map(history::daySummary)
            val totalScheduled = weekSummaries.sumOf { it.scheduledCount }
            val totalCompleted = weekSummaries.sumOf { it.completedCount }
            val rate = if (totalScheduled == 0) 0f else totalCompleted.toFloat() / totalScheduled

            val prevSummaries = prevDates.map(history::daySummary)
            val prevScheduled = prevSummaries.sumOf { it.scheduledCount }
            val prevCompleted = prevSummaries.sumOf { it.completedCount }
            val prevRate = if (prevScheduled == 0) null else prevCompleted.toFloat() / prevScheduled

            // Top & struggling habits
            val habitStats = history.habits.map { habit ->
                val scheduledInWeek = dates.filter(habit::isScheduledOn)
                val completedInWeek = scheduledInWeek.count { date ->
                    val dayHabits = history.habitsForDay(date)
                    dayHabits.firstOrNull { it.habit.id == habit.id }?.isComplete == true
                }
                val hRate = if (scheduledInWeek.isEmpty()) 0f else completedInWeek.toFloat() / scheduledInWeek.size
                Triple(habit, scheduledInWeek.size, hRate)
            }.filter { it.second > 0 }

            val best = habitStats.maxByOrNull { it.third }?.first?.let { h ->
                TodayHabit(habit = h, progress = h.target, status = HabitDayStatus.Completed)
            }
            val struggling = habitStats.minByOrNull { it.third }?.first?.let { h ->
                TodayHabit(habit = h, progress = 0.0, status = HabitDayStatus.Pending)
            }

            // Tasks in week
            val weekStartEpoch = weekStart.toEpochDay()
            val weekEndEpoch = weekEnd.toEpochDay()
            val weekTasks = tasks.filter { task ->
                task.scheduledEpochDay != null && task.scheduledEpochDay in weekStartEpoch..weekEndEpoch
            }
            val plannedTasksCount = weekTasks.size
            val completedTasksCount = weekTasks.count { it.status == "Completed" }
            val carriedTasksCount = weekTasks.count { it.carryCount > 0 }

            // Focus in week
            val weekStartMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val weekEndMillis = weekEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val focusMinutes = focusSessions.filter { it.startedAt in weekStartMillis until weekEndMillis }
                .sumOf { it.durationMinutes }

            // Goal progress count
            val goalProgressCount = goals.count { it.updatedAt in weekStartMillis until weekEndMillis }

            val note = weeklyReviews.firstOrNull { it.weekStart == weekStart }?.note.orEmpty()

            WeeklySummary(
                weekStart = weekStart,
                habitCompletionRate = rate,
                prevHabitCompletionRate = prevRate,
                bestHabit = best,
                strugglingHabit = struggling,
                plannedTasksCount = plannedTasksCount,
                completedTasksCount = completedTasksCount,
                carriedTasksCount = carriedTasksCount,
                focusDurationMinutes = focusMinutes,
                goalProgressCount = goalProgressCount,
                note = note,
            )
        }
    }

    override fun observeMonthlySummary(yearMonth: YearMonth): Flow<MonthlySummary> {
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        val periodKey = "%04d-%02d".format(yearMonth.year, yearMonth.monthValue)

        return combine(
            habitRepository.observeHistory(),
            planDao.observeTasks(),
            focusDao.observeSessions(),
            reviewDao.observePeriodReviews(),
        ) { history, tasks, focusSessions, periodReviews ->
            val dates = generateSequence(startDate) { it.plusDays(1) }.take(yearMonth.lengthOfMonth()).toList()
            val summaries = dates.map(history::daySummary)
            val scheduled = summaries.sumOf { it.scheduledCount }
            val completed = summaries.sumOf { it.completedCount }
            val rate = if (scheduled == 0) 0f else completed.toFloat() / scheduled

            val startEpoch = startDate.toEpochDay()
            val endEpoch = endDate.toEpochDay()
            val completedTasks = tasks.count {
                it.scheduledEpochDay in startEpoch..endEpoch && it.status == "Completed"
            }

            val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val focusMinutes = focusSessions.filter { it.startedAt in startMillis until endMillis }
                .sumOf { it.durationMinutes }

            val stats = history.statistics(yearMonth.lengthOfMonth(), endDate)
            val note = periodReviews.firstOrNull {
                it.periodType == ReviewPeriodType.Monthly.name && it.periodKey == periodKey
            }?.note.orEmpty()

            MonthlySummary(
                yearMonth = yearMonth,
                habitCompletionRate = rate,
                focusDurationMinutes = focusMinutes,
                completedTasksCount = completedTasks,
                topHabits = stats.habits.take(5),
                note = note,
            )
        }
    }

    override fun observeYearlySummary(year: Int): Flow<YearlySummary> {
        val periodKey = year.toString()
        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year, 12, 31)

        return combine(
            habitRepository.observeHistory(),
            focusDao.observeSessions(),
            goalDao.observeGoals(),
            libraryDao.observeContents(),
            reviewDao.observePeriodReviews(),
        ) { history, focusSessions, goals, libraryContents, periodReviews ->
            val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val focusMinutes = focusSessions.filter { it.startedAt in startMillis until endMillis }
                .sumOf { it.durationMinutes }

            val completedGoals = goals.count {
                it.status == GoalStatus.Completed.name && it.updatedAt in startMillis until endMillis
            }

            val completedLibrary = libraryContents.count {
                it.content.status == ContentStatus.Completed.name && it.content.updatedAt in startMillis until endMillis
            }

            val totalCompletions = history.records.count {
                it.date.year == year && (it.status == HabitDayStatus.Completed || it.progress > 0)
            }

            val bestStreak = history.habits.maxOfOrNull { habit ->
                history.statistics(365, endDate).habits.firstOrNull { it.habit.id == habit.id }?.bestStreak ?: 0
            } ?: 0

            val note = periodReviews.firstOrNull {
                it.periodType == ReviewPeriodType.Yearly.name && it.periodKey == periodKey
            }?.note.orEmpty()

            YearlySummary(
                year = year,
                totalHabitCompletions = totalCompletions,
                totalFocusMinutes = focusMinutes,
                completedGoalsCount = completedGoals,
                completedLibraryItemsCount = completedLibrary,
                bestStreakDays = bestStreak,
                note = note,
            )
        }
    }

    override fun observePersonalRecords(): Flow<PersonalRecords> {
        return combine(
            habitRepository.observeHistory(),
            focusDao.observeSessions(),
            goalDao.observeGoals(),
            libraryDao.observeContents(),
        ) { history, focusSessions, goals, libraryContents ->
            val stats = history.statistics(365)
            val longestHabitStat = stats.habits.maxByOrNull { it.bestStreak }
            val longestStreak = longestHabitStat?.bestStreak ?: 0
            val longestHabitName = longestHabitStat?.habit?.name ?: "—"

            val totalHabitCompletions = history.records.count {
                it.status == HabitDayStatus.Completed || it.progress > 0
            }

            // Most productive day
            val completionsByDate = history.records.filter {
                it.status == HabitDayStatus.Completed || it.progress > 0
            }.groupBy { it.date }
            val bestDayEntry = completionsByDate.maxByOrNull { it.value.size }
            val bestDayName = bestDayEntry?.key?.toString() ?: "—"
            val bestDayCount = bestDayEntry?.value?.size ?: 0

            val totalFocusMinutes = focusSessions.sumOf { it.durationMinutes }
            val totalFocusHours = totalFocusMinutes / 60

            val completedGoalsCount = goals.count { it.status == GoalStatus.Completed.name }
            val completedLibraryCount = libraryContents.count { it.content.status == ContentStatus.Completed.name }

            val milestones = listOf(
                PersonalMilestone(
                    id = "streak_7",
                    title = "7 Günlük Seri",
                    description = "Bir alışkanlıkta kesintisiz 7 güne ulaş.",
                    icon = "🌱",
                    currentValue = longestStreak,
                    targetValue = 7,
                    isUnlocked = longestStreak >= 7,
                ),
                PersonalMilestone(
                    id = "streak_30",
                    title = "30 Günlük Ustalık",
                    description = "Bir alışkanlığı 30 gün boyunca aralıksız sürdür.",
                    icon = "🌿",
                    currentValue = longestStreak,
                    targetValue = 30,
                    isUnlocked = longestStreak >= 30,
                ),
                PersonalMilestone(
                    id = "streak_100",
                    title = "100 Günlük Zirve",
                    description = "100 günlük süreklilik ve disiplin.",
                    icon = "🗻",
                    currentValue = longestStreak,
                    targetValue = 100,
                    isUnlocked = longestStreak >= 100,
                ),
                PersonalMilestone(
                    id = "streak_365",
                    title = "365 Günlük Efsane",
                    description = "Tam 1 yıl boyunca kesintisiz devamlılık.",
                    icon = "👑",
                    currentValue = longestStreak,
                    targetValue = 365,
                    isUnlocked = longestStreak >= 365,
                ),
                PersonalMilestone(
                    id = "completions_100",
                    title = "100 Alışkanlık Adımı",
                    description = "Toplam 100 kez alışkanlık tamamla.",
                    icon = "⭐",
                    currentValue = totalHabitCompletions,
                    targetValue = 100,
                    isUnlocked = totalHabitCompletions >= 100,
                ),
                PersonalMilestone(
                    id = "completions_500",
                    title = "500 Alışkanlık Adımı",
                    description = "Toplam 500 kez alışkanlık tamamla.",
                    icon = "✨",
                    currentValue = totalHabitCompletions,
                    targetValue = 500,
                    isUnlocked = totalHabitCompletions >= 500,
                ),
                PersonalMilestone(
                    id = "completions_1000",
                    title = "1000 Alışkanlık Adımı",
                    description = "Toplam 1000 kez alışkanlık tamamla.",
                    icon = "🌟",
                    currentValue = totalHabitCompletions,
                    targetValue = 1000,
                    isUnlocked = totalHabitCompletions >= 1000,
                ),
                PersonalMilestone(
                    id = "focus_10h",
                    title = "10 Saat Derin Odak",
                    description = "Toplam 10 saat odaklanma seansı tamamla.",
                    icon = "⏱️",
                    currentValue = totalFocusHours,
                    targetValue = 10,
                    isUnlocked = totalFocusHours >= 10,
                ),
                PersonalMilestone(
                    id = "focus_50h",
                    title = "50 Saat Derin Odak",
                    description = "Toplam 50 saat odaklanma seansı tamamla.",
                    icon = "⌛",
                    currentValue = totalFocusHours,
                    targetValue = 50,
                    isUnlocked = totalFocusHours >= 50,
                ),
                PersonalMilestone(
                    id = "focus_100h",
                    title = "100 Saat Derin Odak",
                    description = "Toplam 100 saat odaklanma seansı tamamla.",
                    icon = "🌌",
                    currentValue = totalFocusHours,
                    targetValue = 100,
                    isUnlocked = totalFocusHours >= 100,
                ),
                PersonalMilestone(
                    id = "focus_500h",
                    title = "500 Saat Derin Odak",
                    description = "Toplam 500 saat odaklanma seansı tamamla.",
                    icon = "⚡",
                    currentValue = totalFocusHours,
                    targetValue = 500,
                    isUnlocked = totalFocusHours >= 500,
                ),
                PersonalMilestone(
                    id = "goal_1",
                    title = "İlk Hedef Zaferi",
                    description = "Uygulama içinde ilk hedefini tamamla.",
                    icon = "🎯",
                    currentValue = completedGoalsCount,
                    targetValue = 1,
                    isUnlocked = completedGoalsCount >= 1,
                ),
                PersonalMilestone(
                    id = "goal_5",
                    title = "Hedef Ustası",
                    description = "Toplam 5 büyük hedef tamamla.",
                    icon = "🏆",
                    currentValue = completedGoalsCount,
                    targetValue = 5,
                    isUnlocked = completedGoalsCount >= 5,
                ),
                PersonalMilestone(
                    id = "library_10",
                    title = "Kütüphane Kaşifi",
                    description = "10 içerik bitir (kitap, dizi, film vb.).",
                    icon = "📚",
                    currentValue = completedLibraryCount,
                    targetValue = 10,
                    isUnlocked = completedLibraryCount >= 10,
                ),
                PersonalMilestone(
                    id = "library_50",
                    title = "Bilgelik Kütüphanesi",
                    description = "50 içerik bitir (kitap, dizi, film vb.).",
                    icon = "📖",
                    currentValue = completedLibraryCount,
                    targetValue = 50,
                    isUnlocked = completedLibraryCount >= 50,
                ),
            )

            PersonalRecords(
                longestHabitStreak = longestStreak,
                longestHabitName = longestHabitName,
                mostProductiveDayName = bestDayName,
                mostProductiveDayCount = bestDayCount,
                totalFocusHours = totalFocusHours,
                totalHabitCompletions = totalHabitCompletions,
                totalGoalsCompleted = completedGoalsCount,
                totalLibraryCompleted = completedLibraryCount,
                milestones = milestones,
            )
        }
    }

    override fun observeActivityHeatmap(habitId: Long?, days: Int): Flow<List<ActivityHeatmapDay>> {
        val safeDays = days.coerceIn(14, 365)
        val today = LocalDate.now()
        val startDate = today.minusDays((safeDays - 1).toLong())
        val dates = generateSequence(startDate) { it.plusDays(1) }.take(safeDays).toList()

        return combine(
            habitRepository.observeHistory(),
            planDao.observeTasks(),
            focusDao.observeSessions(),
            reviewDao.observeDailyReviews(),
        ) { history, tasks, focusSessions, dailyReviews ->
            val reviewsByEpoch = dailyReviews.associateBy { it.epochDay }
            val focusByDate = focusSessions.groupBy { session ->
                Instant.ofEpochMilli(session.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            }
            val completedTasksByDate = tasks.filter { it.status == "Completed" && it.scheduledEpochDay != null }
                .groupBy { LocalDate.ofEpochDay(it.scheduledEpochDay!!) }

            dates.map { date ->
                val dayHabits = history.habitsForDay(date)
                val habitsCompleted = if (habitId != null) {
                    if (dayHabits.firstOrNull { it.habit.id == habitId }?.isComplete == true) 1 else 0
                } else {
                    dayHabits.count { it.isComplete }
                }

                val tasksCompleted = completedTasksByDate[date]?.size ?: 0
                val focusMinutes = focusByDate[date]?.sumOf { it.durationMinutes } ?: 0
                val reviewDone = reviewsByEpoch.containsKey(date.toEpochDay())

                val rawScore = (habitsCompleted * 2) + tasksCompleted + (focusMinutes / 25) + (if (reviewDone) 1 else 0)
                val intensity = when {
                    rawScore <= 0 -> 0
                    rawScore <= 2 -> 1
                    rawScore <= 5 -> 2
                    rawScore <= 9 -> 3
                    else -> 4
                }

                ActivityHeatmapDay(
                    date = date,
                    intensity = intensity,
                    habitsCompleted = habitsCompleted,
                    tasksCompleted = tasksCompleted,
                    focusMinutes = focusMinutes,
                    dailyReviewDone = reviewDone,
                )
            }
        }
    }

    override fun observeSkipReasonStatistics(): Flow<Map<SkipReasonType, Int>> {
        return habitDao.observeAllEntries().map { entries ->
            val skipped = entries.filter { it.status == HabitEntryEntity.STATUS_SKIPPED }
            val counts = mutableMapOf<SkipReasonType, Int>()
            SkipReasonType.entries.forEach { counts[it] = 0 }

            skipped.forEach { entry ->
                val (parsedType, _) = SkipReasonType.parse(entry.note)
                counts[parsedType] = (counts[parsedType] ?: 0) + 1
            }
            counts
        }
    }
}

private fun DailyReviewEntity.toDomain() = DailyReview(
    epochDay = epochDay,
    mood = mood,
    energy = energy,
    productivity = productivity,
    stress = stress,
    note = note,
    updatedAt = updatedAt,
)

private fun PeriodReviewEntity.toDomain() = PeriodReview(
    periodType = runCatching { ReviewPeriodType.valueOf(periodType) }.getOrDefault(ReviewPeriodType.Monthly),
    periodKey = periodKey,
    note = note,
    updatedAt = updatedAt,
)
