package app.tsuzuku.ui

import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitDayStatus
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.model.GoalPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayViewModelTest {
    private val habit = Habit(
        id = 1,
        name = "Su iç",
        emoji = "💧",
        colorArgb = 0xFF5068A9,
        type = HabitType.Count,
        direction = HabitDirection.Build,
        target = 8.0,
        unit = "bardak",
        step = 1.0,
        scheduleDaysMask = 127,
        sortOrder = 0,
    )

    @Test
    fun progressBelowTarget_isNotComplete() {
        assertFalse(TodayHabit(habit = habit, progress = 7.0).isComplete)
    }

    @Test
    fun progressAtTarget_isComplete() {
        assertTrue(TodayHabit(habit = habit, progress = 8.0).isComplete)
    }

    @Test
    fun progressFraction_isCappedAtOne() {
        assertEquals(1f, TodayHabit(habit = habit, progress = 12.0).progressFraction)
    }

    @Test
    fun explicitCompletedStatus_completesCheckHabit() {
        val checkHabit = habit.copy(type = HabitType.Check, target = 1.0)
        assertTrue(
            TodayHabit(
                habit = checkHabit,
                progress = 0.0,
                status = HabitDayStatus.Completed,
            ).isComplete,
        )
    }

    @Test
    fun weeklyGoalCompletesWhenPeriodTargetIsReached() {
        val weekly = habit.copy(goalPeriod = GoalPeriod.Weekly, periodTarget = 3)
        assertFalse(TodayHabit(habit = weekly, progress = 0.0, periodProgress = 2).isComplete)
        assertTrue(TodayHabit(habit = weekly, progress = 0.0, periodProgress = 3).isComplete)
    }
}
