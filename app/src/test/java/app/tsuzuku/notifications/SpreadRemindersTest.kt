package app.tsuzuku.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class SpreadRemindersTest {
    private val window = SpreadWindow(9 * 60, 21 * 60)
    private val everyDay = 127

    @Test
    fun spreadsWhatIsLeftOverTheRestOfTheWindow() {
        // 12:00, 6 glasses to go, 9 hours left -> every 90 minutes.
        val next = nextSpreadTime(LocalDateTime.of(2026, 11, 9, 12, 0), window, 6, everyDay)
        assertEquals(LocalDateTime.of(2026, 11, 9, 13, 30), next)
    }

    @Test
    fun neverCloserThanHalfAnHour() {
        val next = nextSpreadTime(LocalDateTime.of(2026, 11, 9, 20, 0), window, 8, everyDay)
        assertEquals(LocalDateTime.of(2026, 11, 9, 20, 30), next)
    }

    @Test
    fun beforeTheWindowWaitsForItsStart() {
        val next = nextSpreadTime(LocalDateTime.of(2026, 11, 9, 7, 15), window, 8, everyDay)
        assertEquals(LocalDateTime.of(2026, 11, 9, 9, 0), next)
    }

    @Test
    fun doneOrLateMovesToTomorrow() {
        assertEquals(LocalDateTime.of(2026, 11, 10, 9, 0), nextSpreadTime(LocalDateTime.of(2026, 11, 9, 15, 0), window, 0, everyDay))
        assertEquals(LocalDateTime.of(2026, 11, 10, 9, 0), nextSpreadTime(LocalDateTime.of(2026, 11, 9, 22, 0), window, 3, everyDay))
    }

    @Test
    fun skipsDaysTheHabitIsNotScheduled() {
        // Monday-only habit, asked on Monday evening -> next Monday.
        val mondays = 1
        val next = nextSpreadTime(LocalDateTime.of(2026, 11, 9, 22, 0), window, 3, mondays)
        assertEquals(LocalDateTime.of(2026, 11, 16, 9, 0), next)
    }
}
