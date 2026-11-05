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
}
