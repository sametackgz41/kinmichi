package app.tsuzuku.model

import java.time.LocalDate
import kotlin.math.abs

/** Average mood (1–5) on days a habit was done versus scheduled days it wasn't. */
data class MoodInsight(
    val habit: Habit,
    val withHabit: Double,
    val withoutHabit: Double,
    val doneDays: Int,
    val missedDays: Int,
) {
    val difference: Double get() = withHabit - withoutHabit
}

data class MoodInsights(
    /** Days with a mood logged in the window. */
    val loggedDays: Int,
    val insights: List<MoodInsight>,
) {
    /** Logged days still needed before insights are worth showing. */
    val daysUntilReady: Int get() = (MOOD_MIN_DAYS - loggedDays).coerceAtLeast(0)
}

const val MOOD_MIN_DAYS = 10
private const val MIN_EACH_SIDE = 4
private const val MIN_DIFFERENCE = 0.4

/**
 * Finds habits whose days line up with a noticeably better (or worse) mood, over the last
 * [windowDays]. Correlation only: the card wording says "on days you…", never "because".
 */
fun moodInsights(
    history: HabitHistorySnapshot,
    moods: Map<LocalDate, Int>,
    today: LocalDate,
    windowDays: Int = 90,
    limit: Int = 3,
): MoodInsights {
    val from = today.minusDays(windowDays.toLong())
    val window = moods.filterKeys { !it.isBefore(from) && !it.isAfter(today) }
    val insights = if (window.size < MOOD_MIN_DAYS) emptyList() else history.habits.mapNotNull { habit ->
        val done = mutableListOf<Int>()
        val missed = mutableListOf<Int>()
        window.forEach { (date, mood) ->
            if (date.isBefore(habit.createdOn) || !habit.isScheduledOn(date)) return@forEach
            if (history.progressOn(habit, date) == null) return@forEach // skipped
            if (history.isDone(habit, date)) done += mood else if (date != today) missed += mood
        }
        if (done.size < MIN_EACH_SIDE || missed.size < MIN_EACH_SIDE) return@mapNotNull null
        MoodInsight(habit, done.average(), missed.average(), done.size, missed.size)
            .takeIf { abs(it.difference) >= MIN_DIFFERENCE }
    }.sortedByDescending { abs(it.difference) }.take(limit)
    return MoodInsights(window.size, insights)
}
