package app.tsuzuku.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Everything the shareable streak card shows, computed from the habit's own history. */
data class StreakCardData(
    val habit: Habit,
    val current: Int,
    val best: Int,
    /** Completed days inside [weeks]. */
    val doneDays: Int,
    /** Completion rate over the last 30 scheduled days, 0..1. */
    val rate30: Float,
    /** Calendar weeks, oldest first; each has 7 days starting at the week start. Null = future. */
    val weeks: List<List<HeatDay?>>,
)

const val SHARE_CARD_WEEKS = 20

fun streakCardData(
    history: HabitHistorySnapshot,
    habit: Habit,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    weekCount: Int = SHARE_CARD_WEEKS,
): StreakCardData {
    val streak = history.streak(habit, today)
    val gridStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).minusWeeks((weekCount - 1).toLong())
    val weeks = (0 until weekCount).map { week ->
        (0 until 7).map { day ->
            val date = gridStart.plusDays((week * 7 + day).toLong())
            if (date.isAfter(today)) null else history.heatDay(habit, date)
        }
    }
    val rate = history.statistics(30, today).habits.firstOrNull { it.habit.id == habit.id }?.completionRate ?: 0f
    return StreakCardData(
        habit = habit,
        current = streak.current,
        best = streak.best,
        doneDays = weeks.sumOf { week -> week.count { it == HeatDay.Done } },
        rate30 = rate,
        weeks = weeks,
    )
}
