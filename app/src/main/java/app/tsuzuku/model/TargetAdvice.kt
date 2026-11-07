package app.tsuzuku.model

import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor

/**
 * "Start small" coaching for amount-based habits. When a target is rarely reached, suggest the
 * amount the person actually manages; once that sticks, suggest stepping back up toward the
 * original goal. Uses only the habit's own history — no AI, nothing leaves the phone.
 */
data class TargetAdvice(
    val habit: Habit,
    val kind: Kind,
    val newTarget: Double,
    /** Days the current target was reached in the window. */
    val hits: Int,
    /** Scheduled, not skipped days in the window. */
    val days: Int,
) {
    /**
     * Lower / Raise: an amount target, down to what is managed or back up toward the original.
     * Grow: an amount target that has been met nearly every day for three weeks.
     * Weekly: a daily check habit rarely done; [newTarget] is the suggested times per week.
     * FewerPerWeek / MorePerWeek: a weekly goal adjusted; [newTarget] is times per week.
     */
    enum class Kind { Lower, Raise, Grow, Weekly, FewerPerWeek, MorePerWeek }

    val changesFrequency: Boolean get() = kind == Kind.Weekly || kind == Kind.FewerPerWeek || kind == Kind.MorePerWeek
}

internal const val ADVICE_WINDOW_DAYS = 14
private const val MIN_DAYS = 10
private const val LOWER_BELOW = 0.4
private const val RAISE_FROM = 0.85

/**
 * @param originalTarget the target before Tsuzuku suggested lowering it, if it did; raising
 *   is only ever suggested back toward this value.
 */
fun targetAdvice(
    history: HabitHistorySnapshot,
    habit: Habit,
    today: LocalDate,
    originalTarget: Double? = null,
): TargetAdvice? {
    if (habit.type == HabitType.Check || habit.direction != HabitDirection.Build) return null
    if (habit.goalPeriod != GoalPeriod.Daily || habit.target <= 0.0) return null

    // The last two weeks, not counting today (still in progress) or skipped days.
    val window = (1..ADVICE_WINDOW_DAYS).map { today.minusDays(it.toLong()) }
        .filter { !it.isBefore(habit.createdOn) && habit.isScheduledOn(it) }
        .mapNotNull { date -> history.progressOn(habit, date)?.let { date to it } }
    if (window.size < MIN_DAYS) return null

    val hits = window.count { (date, _) -> history.isDone(habit, date) }
    val rate = hits.toDouble() / window.size
    val step = habit.step.takeIf { it > 0.0 } ?: 1.0

    if (rate < LOWER_BELOW) {
        // What a typical logged day looks like. Days with nothing logged say "forgot", not
        // "too hard", so they don't pull the suggestion down.
        val logged = window.map { it.second }.filter { it > 0.0 }.sorted()
        if (logged.size < 5) return null
        val typical = logged[((logged.size - 1) * 0.6).toInt()]
        val suggestion = (floor(typical / step) * step).coerceAtLeast(step)
        if (suggestion > habit.target * 0.85) return null
        return TargetAdvice(habit, TargetAdvice.Kind.Lower, suggestion, hits, window.size)
    }

    if (originalTarget != null && habit.target < originalTarget && rate >= RAISE_FROM) {
        val increment = maxOf(step, ceil(habit.target * 0.25 / step) * step)
        val suggestion = minOf(originalTarget, habit.target + increment)
        return TargetAdvice(habit, TargetAdvice.Kind.Raise, suggestion, hits, window.size)
    }
    if (originalTarget == null) growAdvice(history, habit, today)?.let { return it }
    return null
}

private const val GROW_WINDOW_DAYS = 21
private const val GROW_FROM = 0.95

/** Three weeks of hitting the target almost every day: time to raise the bar a little. */
private fun growAdvice(history: HabitHistorySnapshot, habit: Habit, today: LocalDate): TargetAdvice? {
    val window = (1..GROW_WINDOW_DAYS).map { today.minusDays(it.toLong()) }
        .filter { !it.isBefore(habit.createdOn) && habit.isScheduledOn(it) && history.progressOn(habit, it) != null }
    if (window.size < 18) return null
    val hits = window.count { history.isDone(habit, it) }
    if (hits.toDouble() / window.size < GROW_FROM) return null
    val step = habit.step.takeIf { it > 0.0 } ?: 1.0
    val increment = maxOf(step, ceil(habit.target * 0.15 / step) * step)
    return TargetAdvice(habit, TargetAdvice.Kind.Grow, habit.target + increment, hits, window.size)
}

/**
 * Frequency coaching for check habits, from the last three full weeks:
 * a daily habit done on fewer than 40% of its days becomes "X times a week", and a weekly goal
 * moves up after three weeks all met, or down when the weeks average well below it.
 */
fun frequencyAdvice(history: HabitHistorySnapshot, habit: Habit, today: LocalDate): TargetAdvice? {
    if (habit.type != HabitType.Check || habit.direction != HabitDirection.Build) return null
    val thisWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val weeks = (1..3).map { thisWeek.minusWeeks(it.toLong()) }
    if (weeks.last().isBefore(habit.createdOn.minusDays(0)) && habit.createdOn != LocalDate.MIN) return null
    val perWeek = weeks.map { start ->
        (0L until 7L).map { start.plusDays(it) }.count { history.isDone(habit, it) }
    }
    return when (habit.goalPeriod) {
        GoalPeriod.Daily -> {
            val daysPerWeek = Integer.bitCount(habit.scheduleDaysMask and 127)
            if (daysPerWeek < 5 || habit.repeatIntervalDays != 1) return null
            val due = weeks.flatMap { start -> (0L until 7L).map { start.plusDays(it) } }
                .filter { habit.isScheduledOn(it) && history.progressOn(habit, it) != null }
            if (due.size < 15) return null
            val done = perWeek.sum()
            if (done.toDouble() / due.size >= 0.4 || done < 3) return null
            val times = Math.round(done / 3.0).toInt().coerceIn(1, daysPerWeek - 1)
            TargetAdvice(habit, TargetAdvice.Kind.Weekly, times.toDouble(), done, due.size)
        }
        GoalPeriod.Weekly -> {
            val goal = habit.periodTarget
            when {
                goal < 7 && perWeek.all { it >= goal } ->
                    TargetAdvice(habit, TargetAdvice.Kind.MorePerWeek, (goal + 1).toDouble(), perWeek.sum(), goal * 3)
                goal > 1 && perWeek.average() < goal * 0.6 -> {
                    val lower = Math.round(perWeek.average()).toInt().coerceIn(1, goal - 1)
                    TargetAdvice(habit, TargetAdvice.Kind.FewerPerWeek, lower.toDouble(), perWeek.sum(), goal * 3)
                }
                else -> null
            }
        }
        GoalPeriod.Monthly -> null
    }
}
