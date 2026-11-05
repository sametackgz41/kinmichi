package app.tsuzuku.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTest {
    private val monday = LocalDate.of(2026, 8, 10)

    @Test
    fun scheduleMaskStartsOnMonday() {
        val mondayOnly = sampleHabit(mask = 1)
        assertTrue(mondayOnly.isScheduledOn(monday))
        assertFalse(mondayOnly.isScheduledOn(monday.plusDays(1)))
    }

    @Test
    fun daySummaryCountsScheduledAndCompletedHabits() {
        val habit = sampleHabit()
        val snapshot = HabitHistorySnapshot(
            habits = listOf(habit),
            records = listOf(completed(habit.id, monday)),
        )
        val summary = snapshot.daySummary(monday)
        assertEquals(1, summary.scheduledCount)
        assertEquals(1, summary.completedCount)
        assertEquals(1f, summary.progressFraction, 0.001f)
    }

    @Test
    fun statisticsUseSelectedWindow() {
        val habit = sampleHabit()
        val snapshot = HabitHistorySnapshot(
            habits = listOf(habit),
            records = listOf(completed(habit.id, monday), completed(habit.id, monday.plusDays(1))),
        )
        val stats = snapshot.statistics(days = 3, endDate = monday.plusDays(2))
        assertEquals(3, stats.scheduledCount)
        assertEquals(2, stats.completedCount)
        assertEquals(2f / 3f, stats.completionRate, 0.001f)
        assertEquals(2, stats.activeDays)
    }

    @Test
    fun currentAndBestStreakAreCalculatedPerSchedule() {
        val habit = sampleHabit()
        val snapshot = HabitHistorySnapshot(
            habits = listOf(habit),
            records = listOf(completed(habit.id, monday), completed(habit.id, monday.plusDays(1))),
        )
        val stats = snapshot.statistics(days = 7, endDate = monday.plusDays(1))
        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.bestStreak)
    }

    @Test
    fun skippedVacationDayIsExcludedFromStatistics() {
        val habit = sampleHabit()
        val snapshot = HabitHistorySnapshot(
            habits = listOf(habit),
            records = listOf(HabitHistoryRecord(habit.id, monday, 0.0, HabitDayStatus.Skipped)),
        )
        val stats = snapshot.statistics(days = 1, endDate = monday)
        assertEquals(0, stats.scheduledCount)
    }

    private fun sampleHabit(mask: Int = 127) = Habit(
        id = 7,
        name = "Test",
        emoji = "✓",
        colorArgb = 0xFF5068A9,
        type = HabitType.Check,
        direction = HabitDirection.Build,
        target = 1.0,
        unit = "",
        step = 1.0,
        scheduleDaysMask = mask,
        sortOrder = 0,
        createdOn = monday,
    )

    private fun completed(habitId: Long, date: LocalDate) = HabitHistoryRecord(
        habitId = habitId,
        date = date,
        progress = 1.0,
        status = HabitDayStatus.Completed,
    )
}
