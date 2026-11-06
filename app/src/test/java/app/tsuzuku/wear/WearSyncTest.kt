package app.tsuzuku.wear

import app.tsuzuku.model.GoalPeriod
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitDayStatus
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.TodayHabit
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WearSyncTest {
    private fun habit(id: Long, type: HabitType, target: Double, unit: String = "") = Habit(
        id = id, name = "H$id", emoji = "✨", colorArgb = 0xFF3E8EC2, type = type,
        direction = HabitDirection.Build, target = target, unit = unit, step = 1.0,
        scheduleDaysMask = 127, goalPeriod = GoalPeriod.Daily, sortOrder = id.toInt(),
    )

    @Test
    fun encodesProgressAndSkipsSkippedHabits() {
        val date = LocalDate.of(2026, 11, 6)
        val json = JSONObject(
            todayJson(
                listOf(
                    TodayHabit(habit(1, HabitType.Check, 1.0), progress = 0.0, status = HabitDayStatus.Completed),
                    TodayHabit(habit(2, HabitType.Count, 8.0, "glasses"), progress = 3.0),
                    TodayHabit(habit(3, HabitType.Count, 2.0), progress = 0.0, status = HabitDayStatus.Skipped),
                ),
                date,
            ),
        )
        assertEquals("2026-11-06", json.getString("date"))
        val habits = json.getJSONArray("habits")
        assertEquals(2, habits.length())
        assertEquals(true, habits.getJSONObject(0).getBoolean("done"))
        assertEquals("", habits.getJSONObject(0).getString("detail"))
        assertEquals("3/8 glasses", habits.getJSONObject(1).getString("detail"))
        assertEquals(0.375, habits.getJSONObject(1).getDouble("fraction"), 1e-9)
    }
}
