package app.tsuzuku.model

import java.time.LocalDate

data class HabitHistoryRecord(
    val habitId: Long,
    val date: LocalDate,
    val progress: Double,
    val status: HabitDayStatus,
    val note: String? = null,
)

data class CalendarDaySummary(
    val date: LocalDate,
    val scheduledCount: Int,
    val completedCount: Int,
    val progressFraction: Float,
)

data class HabitPeriodStat(
    val habit: Habit,
    val scheduledCount: Int,
    val completedCount: Int,
    val completionRate: Float,
    val currentStreak: Int,
    val bestStreak: Int,
)

data class PeriodStatistics(
    val days: Int,
    val scheduledCount: Int,
    val completedCount: Int,
    val completionRate: Float,
    val activeDays: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val habits: List<HabitPeriodStat>,
)

data class HabitHistorySnapshot(
    val habits: List<Habit>,
    val records: List<HabitHistoryRecord>,
    val feelings: List<HabitFeeling> = emptyList(),
) {
    private val recordsByDateAndHabit: Map<LocalDate, Map<Long, HabitHistoryRecord>> =
        records.groupBy(HabitHistoryRecord::date).mapValues { (_, dayRecords) ->
            dayRecords.associateBy(HabitHistoryRecord::habitId)
        }

    fun habitsForDay(date: LocalDate): List<TodayHabit> = habits
        .filter { it.isScheduledOn(date) || recordsByDateAndHabit[date]?.containsKey(it.id) == true }
        .sortedBy(Habit::sortOrder)
        .map { habit ->
            val record = recordsByDateAndHabit[date]?.get(habit.id)
            TodayHabit(
                habit = habit,
                progress = record?.progress ?: 0.0,
                status = record?.status ?: HabitDayStatus.Pending,
                skipReason = record?.note,
            )
        }

    fun daySummary(date: LocalDate): CalendarDaySummary {
        val dayHabits = habitsForDay(date).filter { it.status != HabitDayStatus.Skipped }
        val completed = dayHabits.count(TodayHabit::isComplete)
        return CalendarDaySummary(
            date = date,
            scheduledCount = dayHabits.size,
            completedCount = completed,
            progressFraction = if (dayHabits.isEmpty()) 0f else completed.toFloat() / dayHabits.size,
        )
    }

    /** Days from the first habit (or first record, if earlier) to [endDate], for the "all time" range. */
    fun allTimeDays(endDate: LocalDate = LocalDate.now()): Int {
        val firstHabit = habits.map(Habit::createdOn).filter { it != LocalDate.MIN }.minOrNull()
        val firstRecord = records.minOfOrNull(HabitHistoryRecord::date)
        val start = listOfNotNull(firstHabit, firstRecord).minOrNull() ?: endDate
        return (java.time.temporal.ChronoUnit.DAYS.between(start, endDate) + 1).toInt().coerceIn(1, 3650)
    }

    fun statistics(days: Int, endDate: LocalDate = LocalDate.now()): PeriodStatistics {
        val safeDays = days.coerceAtLeast(1)
        val startDate = endDate.minusDays((safeDays - 1).toLong())
        val dates = generateSequence(startDate) { current ->
            current.plusDays(1).takeIf { !it.isAfter(endDate) }
        }.toList()
        val summaries = dates.map(::daySummary)
        val perHabit = habits.map { habit ->
            val scheduledDates = dates.filter(habit::isScheduledOn).filterNot { date ->
                recordsByDateAndHabit[date]?.get(habit.id)?.status == HabitDayStatus.Skipped
            }
            val allScheduledDates = generateScheduledDates(habit, endDate)
            val completedDates = scheduledDates.filter { date -> isComplete(habit, date) }
            val periodResults = scheduledDates.groupBy { habit.periodKey(it) }
                .map { (_, periodDates) -> periodDates.count { isComplete(habit, it) } >= habit.periodTarget }
            val allPeriodResults = allScheduledDates.groupBy { habit.periodKey(it) }
                .map { (_, periodDates) -> periodDates.count { isComplete(habit, it) } >= habit.periodTarget }
            val scheduledCount = if (habit.goalPeriod == GoalPeriod.Daily) scheduledDates.size else periodResults.size
            val completedCount = if (habit.goalPeriod == GoalPeriod.Daily) completedDates.size else periodResults.count { it }
            val dailyStreak = if (habit.goalPeriod == GoalPeriod.Daily) streakOf(streakDays(habit, allScheduledDates, endDate)) else null
            HabitPeriodStat(
                habit = habit,
                scheduledCount = scheduledCount,
                completedCount = completedCount,
                completionRate = ratio(completedCount, scheduledCount),
                currentStreak = dailyStreak?.current ?: currentBooleanStreak(allPeriodResults),
                bestStreak = dailyStreak?.best ?: bestBooleanStreak(allPeriodResults),
            )
        }.sortedWith(compareByDescending<HabitPeriodStat> { it.completionRate }.thenBy { it.habit.sortOrder })
        val scheduled = perHabit.sumOf(HabitPeriodStat::scheduledCount)
        val completed = perHabit.sumOf(HabitPeriodStat::completedCount)
        return PeriodStatistics(
            days = safeDays,
            scheduledCount = scheduled,
            completedCount = completed,
            completionRate = ratio(completed, scheduled),
            activeDays = summaries.count { it.completedCount > 0 },
            currentStreak = perHabit.maxOfOrNull(HabitPeriodStat::currentStreak) ?: 0,
            bestStreak = perHabit.maxOfOrNull(HabitPeriodStat::bestStreak) ?: 0,
            habits = perHabit,
        )
    }

    private fun isComplete(habit: Habit, date: LocalDate): Boolean {
        val record = recordsByDateAndHabit[date]?.get(habit.id) ?: return false
        return if (habit.direction == HabitDirection.Quit) {
            record.status == HabitDayStatus.Completed || (habit.maximumTarget != null && record.progress <= habit.maximumTarget && record.status != HabitDayStatus.Failed)
        } else {
            record.status == HabitDayStatus.Completed || record.progress >= habit.targetOn(date)
        }
    }

    private fun preservesStreak(habit: Habit, date: LocalDate): Boolean {
        val record = recordsByDateAndHabit[date]?.get(habit.id) ?: return false
        return if (habit.direction == HabitDirection.Quit) {
            record.status == HabitDayStatus.Completed || (habit.maximumTarget != null && record.progress <= habit.maximumTarget && record.status != HabitDayStatus.Failed)
        } else {
            record.status == HabitDayStatus.Completed || record.progress >= habit.targetOn(date) ||
                (habit.minimumTarget > 0.0 && record.progress >= habit.minimumTarget)
        }
    }

    /**
     * True when [habit] was due yesterday and nothing kept its streak alive (no completion,
     * minimum or skip). Used for the "never miss twice" nudge.
     */
    fun missedYesterday(habit: Habit, today: LocalDate): Boolean {
        if (habit.direction == HabitDirection.Quit || habit.goalPeriod != GoalPeriod.Daily) return false
        val yesterday = today.minusDays(1)
        if (yesterday.isBefore(habit.createdOn) || !habit.isScheduledOn(yesterday)) return false
        val record = recordsByDateAndHabit[yesterday]?.get(habit.id)
        if (record?.status == HabitDayStatus.Skipped) return false
        return !preservesStreak(habit, yesterday)
    }

    /** First day each habit was ever done; a miss before that is a slow start, not a break. */
    private val firstDoneByHabit: Map<Long, LocalDate> by lazy {
        val byId = habits.associateBy(Habit::id)
        records.filter { record -> byId[record.habitId]?.let { preservesStreak(it, record.date) } == true }
            .groupBy(HabitHistoryRecord::habitId)
            .mapValues { (_, list) -> list.minOf(HabitHistoryRecord::date) }
    }

    /**
     * A kintsugi day: [habit] was done on [date] after its previous due day had been missed.
     * The break is mended in gold instead of hidden.
     */
    fun isComeback(habit: Habit, date: LocalDate): Boolean {
        if (habit.direction == HabitDirection.Quit || habit.goalPeriod != GoalPeriod.Daily) return false
        if (!preservesStreak(habit, date)) return false
        val firstDone = firstDoneByHabit[habit.id] ?: return false
        var day = date.minusDays(1)
        repeat(31) {
            if (day.isBefore(firstDone) || day.isBefore(habit.createdOn)) return false
            val skipped = recordsByDateAndHabit[day]?.get(habit.id)?.status == HabitDayStatus.Skipped
            if (habit.isScheduledOn(day) && !skipped) return !preservesStreak(habit, day)
            day = day.minusDays(1)
        }
        return false
    }

    /** Comebacks across every habit between [from] and [to], inclusive. */
    fun comebackCount(from: LocalDate, to: LocalDate): Int {
        var count = 0
        var day = from
        while (!day.isAfter(to)) {
            habits.forEach { if (isComeback(it, day)) count++ }
            day = day.plusDays(1)
        }
        return count
    }

    /** An explicit failure, as opposed to a day that was simply not logged. */
    private fun isBroken(habit: Habit, date: LocalDate): Boolean {
        val record = recordsByDateAndHabit[date]?.get(habit.id) ?: return false
        return record.status == HabitDayStatus.Failed ||
            (habit.direction == HabitDirection.Quit && habit.maximumTarget != null && record.progress > habit.maximumTarget)
    }

    private fun generateScheduledDates(habit: Habit, endDate: LocalDate): List<LocalDate> {
        val firstRecord = records.asSequence().filter { it.habitId == habit.id }.minOfOrNull(HabitHistoryRecord::date)
        val start = maxOf(habit.createdOn, firstRecord ?: habit.createdOn, endDate.minusDays(365))
        if (start.isAfter(endDate)) return emptyList()
        return generateSequence(start) { current ->
            current.plusDays(1).takeIf { !it.isAfter(endDate) }
        }.filter(habit::isScheduledOn).filterNot { date ->
            recordsByDateAndHabit[date]?.get(habit.id)?.status == HabitDayStatus.Skipped
        }.toList()
    }

    /** Logged amount on [date]; 0 when nothing was logged, null when the day was skipped. */
    fun progressOn(habit: Habit, date: LocalDate): Double? {
        val record = recordsByDateAndHabit[date]?.get(habit.id)
        return when {
            record?.status == HabitDayStatus.Skipped -> null
            record == null -> 0.0
            else -> record.progress
        }
    }

    /** Whether [habit] counts as done on [date] (target met, or within limits for a quit habit). */
    fun isDone(habit: Habit, date: LocalDate): Boolean = isComplete(habit, date)

    /** How one day looks on a habit's year heatmap. */
    fun heatDay(habit: Habit, date: LocalDate): HeatDay {
        val record = recordsByDateAndHabit[date]?.get(habit.id)
        return when {
            record?.status == HabitDayStatus.Skipped -> HeatDay.Skipped
            isComplete(habit, date) -> HeatDay.Done
            record == null && !habit.isScheduledOn(date) -> HeatDay.Off
            record == null || habit.direction == HabitDirection.Quit || habit.target <= 0.0 -> HeatDay.Partial(0f)
            else -> HeatDay.Partial((record.progress / habit.targetOn(date)).toFloat().coerceIn(0f, 0.99f))
        }
    }

    /** Streak for a daily habit as of [today], with this week's rest day applied. */
    fun streak(habit: Habit, today: LocalDate): StreakResult =
        if (habit.goalPeriod != GoalPeriod.Daily) {
            StreakResult(0, 0, restUsedThisWeek = false)
        } else {
            streakOf(streakDays(habit, generateScheduledDates(habit, today), today))
        }

    private fun streakDays(habit: Habit, scheduledDates: List<LocalDate>, today: LocalDate): List<Pair<LocalDate, StreakDay>> =
        scheduledDates.map { date ->
            date to when {
                preservesStreak(habit, date) -> StreakDay.Kept
                isBroken(habit, date) -> StreakDay.Broken
                date == today -> StreakDay.Pending
                else -> StreakDay.Missed
            }
        }

    private fun currentBooleanStreak(results: List<Boolean>): Int = results.asReversed().takeWhile { it }.size

    private fun bestBooleanStreak(results: List<Boolean>): Int {
        var best = 0
        var current = 0
        results.forEach { complete ->
            current = if (complete) current + 1 else 0
            best = maxOf(best, current)
        }
        return best
    }
}

fun Habit.isScheduledOn(date: LocalDate): Boolean {
    val start = activeStartEpochDay?.let(LocalDate::ofEpochDay) ?: createdOn
    val end = activeEndEpochDay?.let(LocalDate::ofEpochDay)
    val pausedUntil = pausedUntilEpochDay?.let(LocalDate::ofEpochDay)
    if (date.isBefore(start) || (end != null && date.isAfter(end)) || (pausedUntil != null && !date.isAfter(pausedUntil))) return false
    if (repeatIntervalDays > 1) return java.time.temporal.ChronoUnit.DAYS.between(start, date) % repeatIntervalDays == 0L
    return scheduleDaysMask and (1 shl (date.dayOfWeek.value - 1)) != 0
}

private fun ratio(completed: Int, scheduled: Int): Float =
    if (scheduled <= 0) 0f else completed.toFloat() / scheduled

private fun Habit.periodKey(date: LocalDate): String = periodBounds(date).first.toString()

sealed interface HeatDay {
    /** Not scheduled and nothing logged. */
    data object Off : HeatDay
    data object Skipped : HeatDay
    data object Done : HeatDay

    /** Scheduled; [fraction] of the target reached (0 = nothing). */
    data class Partial(val fraction: Float) : HeatDay
}
