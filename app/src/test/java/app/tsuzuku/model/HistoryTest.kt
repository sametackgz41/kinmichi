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

    @Test
    fun missedYesterdayFlagsAnUnloggedDueDay() {
        val habit = sampleHabit()
        val snapshot = HabitHistorySnapshot(habits = listOf(habit), records = listOf(completed(habit.id, monday)))
        // Tuesday done? no; asking on Wednesday about Tuesday.
        assertTrue(snapshot.missedYesterday(habit, monday.plusDays(2)))
        // Asking on Tuesday about Monday, which was done.
        assertFalse(snapshot.missedYesterday(habit, monday.plusDays(1)))
    }

    @Test
    fun missedYesterdayIgnoresSkipsUnscheduledDaysAndTheFirstDay() {
        val habit = sampleHabit()
        val skipped = HabitHistoryRecord(habit.id, monday.plusDays(1), 0.0, HabitDayStatus.Skipped)
        val snapshot = HabitHistorySnapshot(habits = listOf(habit), records = listOf(skipped))
        assertFalse(snapshot.missedYesterday(habit, monday.plusDays(2)))
        assertFalse(snapshot.missedYesterday(habit, monday))
        val mondayOnly = sampleHabit(mask = 1)
        assertFalse(HabitHistorySnapshot(listOf(mondayOnly), emptyList()).missedYesterday(mondayOnly, monday.plusDays(3)))
    }

    @Test
    fun minimumAmountKeepsYesterdayFromCountingAsMissed() {
        val habit = sampleHabit().copy(type = HabitType.Count, target = 10.0, minimumTarget = 2.0)
        val small = HabitHistoryRecord(habit.id, monday.plusDays(1), 2.0, HabitDayStatus.Pending)
        val snapshot = HabitHistorySnapshot(habits = listOf(habit), records = listOf(small))
        assertFalse(snapshot.missedYesterday(habit, monday.plusDays(2)))
    }

    @Test
    fun comebackIsTheFirstDoneDayAfterABreak() {
        val habit = sampleHabit()
        // Mon done, Tue missed, Wed done (comeback), Thu done.
        val snapshot = HabitHistorySnapshot(
            habits = listOf(habit),
            records = listOf(completed(habit.id, monday), completed(habit.id, monday.plusDays(2)), completed(habit.id, monday.plusDays(3))),
        )
        assertFalse(snapshot.isComeback(habit, monday))
        assertTrue(snapshot.isComeback(habit, monday.plusDays(2)))
        assertFalse(snapshot.isComeback(habit, monday.plusDays(3)))
        assertEquals(1, snapshot.comebackCount(monday, monday.plusDays(6)))
    }

    @Test
    fun slowStartAndSkippedDaysAreNotComebacks() {
        val habit = sampleHabit()
        val skipped = HabitHistoryRecord(habit.id, monday.plusDays(3), 0.0, HabitDayStatus.Skipped)
        // Never done Mon/Tue (slow start), done Wed, skipped Thu, done Fri.
        val snapshot = HabitHistorySnapshot(
            habits = listOf(habit),
            records = listOf(completed(habit.id, monday.plusDays(2)), skipped, completed(habit.id, monday.plusDays(4))),
        )
        assertFalse(snapshot.isComeback(habit, monday.plusDays(2)))
        assertFalse(snapshot.isComeback(habit, monday.plusDays(4)))
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
