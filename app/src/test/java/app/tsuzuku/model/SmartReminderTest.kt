package app.tsuzuku.model

import app.tsuzuku.notifications.streakAtRisk
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmartReminderTest {
    private val habit = Habit(
        id = 3, name = "Run", emoji = "🏃", colorArgb = 0xFF5068A9, type = HabitType.Check,
        direction = HabitDirection.Build, target = 1.0, unit = "", step = 1.0, scheduleDaysMask = 127,
        sortOrder = 0, createdOn = LocalDate.of(2026, 1, 1), reminderMinutes = 21 * 60,
    )

    @Test
    fun medianNeedsEnoughDays() {
        assertNull(typicalMinute(listOf(480, 490, 500, 510)))
        assertEquals(500, typicalMinute(listOf(480, 490, 500, 510, 900)))
    }

    @Test
    fun suggestsJustBeforeTheUsualTimeRoundedToFiveMinutes() {
        // Usually done around 07:42; reminder at 21:00.
        val suggestion = reminderSuggestion(habit, listOf(460, 462, 470, 455, 465))
        assertEquals(21 * 60, suggestion?.fromMinutes)
        // Median 07:42, ten minutes earlier, rounded: 07:30.
        assertEquals(7 * 60 + 30, suggestion?.toMinutes)
    }

    @Test
    fun staysQuietWhenTheReminderIsAlreadyClose() {
        val close = habit.copy(reminderMinutes = 7 * 60 + 30)
        assertNull(reminderSuggestion(close, listOf(460, 462, 470, 455, 465)))
        assertNull(reminderSuggestion(habit.copy(reminderMinutes = null), listOf(460, 462, 470, 455, 465)))
    }

    @Test
    fun streakAtRiskPicksTheLongestPendingDailyStreak() {
        val short = TodayHabit(habit.copy(id = 1), progress = 0.0, streakDays = 2)
        val long = TodayHabit(habit.copy(id = 2), progress = 0.0, streakDays = 12)
        val mid = TodayHabit(habit.copy(id = 3), progress = 0.0, streakDays = 5)
        val done = TodayHabit(habit.copy(id = 4), progress = 1.0, status = HabitDayStatus.Completed, streakDays = 30)
        assertEquals(2L, streakAtRisk(listOf(short, mid, long, done))?.habit?.id)
        assertNull(streakAtRisk(listOf(short, done)))
    }
}
