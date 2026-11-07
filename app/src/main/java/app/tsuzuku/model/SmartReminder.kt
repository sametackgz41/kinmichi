package app.tsuzuku.model

import kotlin.math.abs

/** How many days of completions the suggestion looks back over, and how many it needs. */
const val SMART_REMINDER_WINDOW_DAYS = 28
const val SMART_REMINDER_MIN_DAYS = 5

/** A reminder that fires at a time the habit is rarely done, and a better time for it. */
data class ReminderSuggestion(val habit: Habit, val fromMinutes: Int, val toMinutes: Int)

/** Median minute of the day, or null with too few samples to trust. */
fun typicalMinute(minutes: List<Int>): Int? {
    if (minutes.size < SMART_REMINDER_MIN_DAYS) return null
    val sorted = minutes.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
}

/**
 * Suggests moving [habit]'s reminder to just before the time it is usually done, when that is at
 * least 45 minutes away from the current reminder. The new time is rounded to 5 minutes.
 */
fun reminderSuggestion(habit: Habit, completionMinutes: List<Int>): ReminderSuggestion? {
    val current = habit.reminderMinutes ?: return null
    val typical = typicalMinute(completionMinutes) ?: return null
    val target = (((typical - 10).coerceIn(0, 24 * 60 - 5) + 2) / 5) * 5
    if (abs(target - current) < 45) return null
    return ReminderSuggestion(habit, current, target)
}
