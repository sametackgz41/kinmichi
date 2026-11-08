package app.tsuzuku.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TargetHistoryTest {
    private val oct1 = LocalDate.of(2026, 10, 1)
    private val nov8 = LocalDate.of(2026, 11, 8)
    private val habit = Habit(
        id = 1, name = "Kitap", emoji = "📖", colorArgb = 0, type = HabitType.Count, direction = HabitDirection.Build,
        target = 30.0, unit = "sayfa", step = 1.0, scheduleDaysMask = 127, sortOrder = 0,
        targetHistory = TargetChange.parse("2026-10-01:10.0|2026-11-08:20.0"),
    )

    @Test
    fun eachDayUsesTheTargetItHadThen() {
        assertEquals(10.0, habit.targetOn(oct1.minusDays(1)), 0.0)
        assertEquals(20.0, habit.targetOn(oct1), 0.0)
        assertEquals(20.0, habit.targetOn(nov8.minusDays(1)), 0.0)
        assertEquals(30.0, habit.targetOn(nov8), 0.0)
    }

    @Test
    fun aSecondChangeTheSameDayKeepsTheFirstOldValue() {
        val once = TargetChange.record("", 20.0, nov8)
        val twice = TargetChange.record(once, 25.0, nov8)
        assertEquals(listOf(TargetChange(nov8, 20.0)), TargetChange.parse(twice))
    }
}
