package app.tsuzuku.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FeelingsTest {
    private val today = LocalDate.of(2026, 11, 7)

    private fun habit(id: Long, name: String) = Habit(
        id = id, name = name, emoji = "✓", colorArgb = 0xFF5068A9, type = HabitType.Check,
        direction = HabitDirection.Build, target = 1.0, unit = "", step = 1.0, scheduleDaysMask = 127,
        goalPeriod = GoalPeriod.Daily, sortOrder = id.toInt(),
    )

    private fun feelings(habitId: Long, vararg values: Feeling) =
        values.mapIndexed { i, f -> HabitFeeling(habitId, today.minusDays(i.toLong()), f) }

    @Test
    fun summaryCountsAndPicksTheUsualAnswer() {
        val snapshot = HabitHistorySnapshot(
            listOf(habit(1, "Read")), emptyList(),
            feelings(1, Feeling.Good, Feeling.Good, Feeling.Hard, Feeling.Okay),
        )
        val summary = snapshot.feelingSummary(1)
        assertEquals(FeelingSummary(hard = 1, okay = 1, good = 2), summary)
        assertEquals(Feeling.Good, summary.usual)
        assertEquals(2.25, summary.average!!, 0.001)
        assertEquals(2, snapshot.feelingSummary(1, since = today.minusDays(1)).total)
        assertNull(FeelingSummary().usual)
    }

    @Test
    fun mostEnjoyedNeedsEnoughAnswersAndSortsByAverage() {
        val snapshot = HabitHistorySnapshot(
            listOf(habit(1, "Read"), habit(2, "Run"), habit(3, "Walk")), emptyList(),
            feelings(1, Feeling.Okay, Feeling.Okay, Feeling.Good) +
                feelings(2, Feeling.Good, Feeling.Good, Feeling.Good) +
                feelings(3, Feeling.Good),
        )
        assertEquals(listOf("Run", "Read"), snapshot.mostEnjoyed().map { it.first.name })
    }
}
