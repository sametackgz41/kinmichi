package app.tsuzuku.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StreakCardTest {
    private val today = LocalDate.of(2026, 11, 6) // a Friday

    private val habit = Habit(
        id = 1, name = "Read", emoji = "📖", colorArgb = 0xFF3E8EC2, type = HabitType.Check,
        direction = HabitDirection.Build, target = 1.0, unit = "", step = 1.0,
        scheduleDaysMask = 127, goalPeriod = GoalPeriod.Daily, sortOrder = 0,
        createdOn = today.minusDays(200),
    )

    private fun doneOn(vararg daysAgo: Long) = HabitHistorySnapshot(
        listOf(habit),
        daysAgo.map { HabitHistoryRecord(habit.id, today.minusDays(it), 1.0, HabitDayStatus.Completed) },
    )

    @Test
    fun gridEndsInTheCurrentWeekWithFutureDaysEmpty() {
        val data = streakCardData(doneOn(0, 1, 2), habit, today, DayOfWeek.MONDAY)
        assertEquals(SHARE_CARD_WEEKS, data.weeks.size)
        val lastWeek = data.weeks.last()
        // Monday..Friday are past or today, Saturday and Sunday are still ahead.
        assertEquals(HeatDay.Done, lastWeek[4])
        assertNull(lastWeek[5])
        assertNull(lastWeek[6])
    }

    @Test
    fun countsStreakAndDoneDays() {
        val data = streakCardData(doneOn(0, 1, 2, 10), habit, today, DayOfWeek.MONDAY)
        assertEquals(3, data.current)
        assertEquals(4, data.doneDays)
    }

    @Test
    fun weekStartFollowsTheSetting() {
        val data = streakCardData(doneOn(0), habit, today, DayOfWeek.SUNDAY)
        // Sunday-first: Friday is the sixth cell.
        assertEquals(HeatDay.Done, data.weeks.last()[5])
        assertNull(data.weeks.last()[6])
    }
}
