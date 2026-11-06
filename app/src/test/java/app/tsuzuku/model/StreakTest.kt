package app.tsuzuku.model

import app.tsuzuku.model.StreakDay.Kept
import app.tsuzuku.model.StreakDay.Missed
import app.tsuzuku.model.StreakDay.Pending
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakTest {
    // 2026-11-02 is a Monday.
    private val monday = LocalDate.of(2026, 11, 2)

    private fun days(vararg pattern: StreakDay, start: LocalDate = monday) =
        pattern.mapIndexed { i, day -> start.plusDays(i.toLong()) to day }

    @Test
    fun unfinishedTodayDoesNotBreakTheStreak() {
        val result = streakOf(days(Kept, Kept, Kept, Pending))
        assertEquals(3, result.current)
        assertFalse(result.restUsedThisWeek)
    }

    @Test
    fun oneMissedDayAWeekIsForgiven() {
        val result = streakOf(days(Kept, Kept, Missed, Kept, Kept))
        assertEquals(4, result.current)
        assertEquals(4, result.best)
        assertTrue(result.restUsedThisWeek)
    }

    @Test
    fun secondMissInTheSameWeekBreaksIt() {
        val result = streakOf(days(Kept, Missed, Kept, Missed, Kept, Pending))
        // Walking back from today the later miss is forgiven, the earlier one ends the streak.
        assertEquals(2, result.current)
        assertEquals(2, result.best)
    }

    @Test
    fun eachWeekGetsItsOwnRestDay() {
        // Miss on Wednesday of week one and Tuesday of week two: both forgiven.
        val pattern = arrayOf(Kept, Kept, Missed, Kept, Kept, Kept, Kept, Kept, Missed, Kept)
        val result = streakOf(days(*pattern))
        assertEquals(8, result.current)
        assertTrue(result.restUsedThisWeek)
    }

    @Test
    fun noRestDaysMeansStrictStreaks() {
        val result = streakOf(days(Kept, Kept, Missed, Kept), restPerWeek = 0)
        assertEquals(1, result.current)
        assertEquals(2, result.best)
    }

    @Test
    fun explicitFailureIsNeverForgiven() {
        val result = streakOf(days(Kept, Kept, StreakDay.Broken, Kept))
        assertEquals(1, result.current)
        assertEquals(2, result.best)
        assertFalse(result.restUsedThisWeek)
    }

    @Test
    fun onlyMissesGiveNoStreak() {
        val result = streakOf(days(Missed, Missed, Pending))
        assertEquals(0, result.current)
        assertEquals(0, result.best)
        assertFalse(result.restUsedThisWeek)
    }
}
