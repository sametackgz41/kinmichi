package app.tsuzuku.notifications

import app.tsuzuku.model.GoalPeriod
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitDayStatus
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.HabitHistoryRecord
import app.tsuzuku.model.HabitHistorySnapshot
import app.tsuzuku.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class WeeklySummaryTest {
    private val zone = ZoneId.of("Europe/Istanbul")

    @Test
    fun summaryRunsOnSundayEvening() {
        // Wednesday -> the coming Sunday
        val wednesday = ZonedDateTime.of(2026, 11, 4, 10, 0, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 11, 8, 19, 0, 0, 0, zone), nextWeeklySummaryTime(wednesday))
        // Sunday before 19:00 -> same day
        val sundayMorning = ZonedDateTime.of(2026, 11, 8, 9, 0, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 11, 8, 19, 0, 0, 0, zone), nextWeeklySummaryTime(sundayMorning))
        // Sunday after 19:00 -> next week
        val sundayNight = ZonedDateTime.of(2026, 11, 8, 20, 0, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 11, 15, 19, 0, 0, 0, zone), nextWeeklySummaryTime(sundayNight))
    }

    @Test
    fun recapCountsTheLastSevenDays() {
        val today = LocalDate.of(2026, 11, 8)
        val read = habit(1, "Read")
        val walk = habit(2, "Walk")
        val records = (0L..6L).flatMap { back ->
            val date = today.minusDays(back)
            listOfNotNull(
                HabitHistoryRecord(1, date, 1.0, HabitDayStatus.Completed),
                if (back < 3) HabitHistoryRecord(2, date, 1.0, HabitDayStatus.Completed) else null,
            )
        }
        val recap = weekRecap(HabitHistorySnapshot(listOf(read, walk), records), today)
        assertEquals(14, recap.scheduled)
        assertEquals(10, recap.completed)
        assertEquals(71, recap.percent)
        assertEquals("Read", recap.bestHabit)
    }

    @Test
    fun emptyWeekHasNoBestHabit() {
        val recap = weekRecap(HabitHistorySnapshot(emptyList(), emptyList()), LocalDate.of(2026, 11, 8))
        assertEquals(0, recap.percent)
        assertNull(recap.bestHabit)
    }

    private fun habit(id: Long, name: String) = Habit(
        id = id, name = name, emoji = "✓", colorArgb = 0xFF405F90, type = HabitType.Check,
        direction = HabitDirection.Build, target = 1.0, unit = "", step = 1.0, scheduleDaysMask = 127,
        goalPeriod = GoalPeriod.Daily, sortOrder = id.toInt(), createdOn = LocalDate.of(2026, 1, 1),
    )
}
