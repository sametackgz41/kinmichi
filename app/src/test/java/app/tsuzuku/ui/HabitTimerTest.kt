package app.tsuzuku.ui

import app.tsuzuku.data.preferences.StoredTimerSession
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitTimerTest {
    private val session = StoredTimerSession(
        habitId = 4,
        startedAtMillis = 10_000,
        maxSeconds = 120,
        localDate = LocalDate.of(2026, 8, 18),
    )

    @Test
    fun elapsedTimeNeverBecomesNegative() {
        assertEquals(0, elapsedSeconds(session, 5_000))
    }

    @Test
    fun elapsedTimeIsMeasuredInSeconds() {
        assertEquals(45, elapsedSeconds(session, 55_999))
    }

    @Test
    fun elapsedTimeStopsAtTarget() {
        assertEquals(120, elapsedSeconds(session, 999_000))
    }

    @Test
    fun pausedTimeIsNotCounted() {
        // Started at 10 s, 20 s of it paused: at 70 s on the clock, 40 s counted.
        val resumed = session.copy(pausedMillis = 20_000)
        assertEquals(40, elapsedSeconds(resumed, 70_000))
    }

    @Test
    fun clockStandsStillWhilePaused() {
        val paused = session.copy(pausedAtMillis = 40_000)
        assertEquals(30, elapsedSeconds(paused, 500_000))
    }
}
