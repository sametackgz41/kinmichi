package app.tsuzuku.notifications

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitReminderSchedulerTest {
    private val zone = ZoneId.of("Europe/Istanbul")

    @Test
    fun dailyReminderUsesTodayWhenTimeIsAhead() {
        val now = ZonedDateTime.of(2026, 8, 17, 8, 59, 30, 0, zone)

        val next = nextReminderTime(127, 9 * 60, now)

        assertEquals(ZonedDateTime.of(2026, 8, 17, 9, 0, 0, 0, zone), next)
    }

    @Test
    fun elapsedReminderMovesToNextScheduledDay() {
        val now = ZonedDateTime.of(2026, 8, 17, 9, 0, 0, 0, zone)

        val next = nextReminderTime(127, 9 * 60, now)

        assertEquals(ZonedDateTime.of(2026, 8, 18, 9, 0, 0, 0, zone), next)
    }

    @Test
    fun weekdayMaskSkipsUnselectedDays() {
        val monday = ZonedDateTime.of(2026, 8, 17, 10, 0, 0, 0, zone)
        val wednesdayOnly = 1 shl 2

        val next = nextReminderTime(wednesdayOnly, 7 * 60 + 30, monday)

        assertEquals(ZonedDateTime.of(2026, 8, 19, 7, 30, 0, 0, zone), next)
    }
}
