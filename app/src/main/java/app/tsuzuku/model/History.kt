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
            val periodResults = scheduledDates.groupBy { it.periodKey(habit.goalPeriod) }
                .map { (_, periodDates) -> periodDates.count { isComplete(habit, it) } >= habit.periodTarget }
            val allPeriodResults = allScheduledDates.groupBy { it.periodKey(habit.goalPeriod) }
                .map { (_, periodDates) -> periodDates.count { isComplete(habit, it) } >= habit.periodTarget }
            val scheduledCount = if (habit.goalPeriod == GoalPeriod.Daily) scheduledDates.size else periodResults.size
            val completedCount = if (habit.goalPeriod == GoalPeriod.Daily) completedDates.size else periodResults.count { it }
            HabitPeriodStat(
                habit = habit,
                scheduledCount = scheduledCount,
                completedCount = completedCount,
                completionRate = ratio(completedCount, scheduledCount),
                currentStreak = if (habit.goalPeriod == GoalPeriod.Daily) streakOf(streakDays(habit, allScheduledDates, endDate)).current else currentBooleanStreak(allPeriodResults),
                bestStreak = if (habit.goalPeriod == GoalPeriod.Daily) streakOf(streakDays(habit, allScheduledDates, endDate)).best else bestBooleanStreak(allPeriodResults),
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
            record.status == HabitDayStatus.Completed || record.progress >= habit.target
        }
    }

    private fun preservesStreak(habit: Habit, date: LocalDate): Boolean {
        val record = recordsByDateAndHabit[date]?.get(habit.id) ?: return false
        return if (habit.direction == HabitDirection.Quit) {
            record.status == HabitDayStatus.Completed || (habit.maximumTarget != null && record.progress <= habit.maximumTarget && record.status != HabitDayStatus.Failed)
        } else {
            record.status == HabitDayStatus.Completed || record.progress >= habit.target ||
                (habit.minimumTarget > 0.0 && record.progress >= habit.minimumTarget)
        }
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

private fun LocalDate.periodKey(period: GoalPeriod): String = when (period) {
    GoalPeriod.Daily -> toString()
    GoalPeriod.Weekly -> minusDays((dayOfWeek.value - 1).toLong()).toString()
    GoalPeriod.Monthly -> "%04d-%02d".format(year, monthValue)
}
