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
    enum class Kind { Lower, Raise }
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
    return null
}
