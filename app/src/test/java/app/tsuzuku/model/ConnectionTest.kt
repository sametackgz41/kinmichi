package app.tsuzuku.model

import app.tsuzuku.ui.ActiveFocusState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionTest {

    @Test
    fun focusSessionTargetTypeResolvesCorrectly() {
        val taskSession = FocusSession(
            id = 1,
            taskId = 10,
            mode = FocusMode.Pomodoro,
            startedAt = 1000L,
        )
        assertEquals(FocusTargetType.Task, taskSession.targetType)

        val habitSession = FocusSession(
            id = 2,
            habitId = 20,
            mode = FocusMode.Stopwatch,
            startedAt = 1000L,
        )
        assertEquals(FocusTargetType.Habit, habitSession.targetType)

        val goalSession = FocusSession(
            id = 3,
            goalId = 30,
            mode = FocusMode.Custom,
            startedAt = 1000L,
        )
        assertEquals(FocusTargetType.Goal, goalSession.targetType)

        val freeSession = FocusSession(
            id = 4,
            mode = FocusMode.Pomodoro,
            startedAt = 1000L,
        )
        assertEquals(FocusTargetType.Free, freeSession.targetType)
    }

    @Test
    fun activeFocusStateRemainingSecondsAndProgress() {
        val state = ActiveFocusState(
            mode = FocusMode.Pomodoro,
            targetSeconds = 1500, // 25 min
            elapsedSeconds = 300,  // 5 min
            isRunning = true,
        )

        assertEquals(1200, state.remainingSeconds)
        assertEquals(0.2f, state.progressFraction, 0.001f)
    }

    @Test
    fun activeFocusStateElapsedExceedingTargetClampsGracefully() {
        val state = ActiveFocusState(
            mode = FocusMode.Pomodoro,
            targetSeconds = 1500,
            elapsedSeconds = 1600,
            isRunning = true,
        )

        assertEquals(0, state.remainingSeconds)
        assertEquals(1.0f, state.progressFraction, 0.001f)
    }

    @Test
    fun habitGoalContributionModesArePreserved() {
        val link1 = HabitGoalLink(
            habitId = 1,
            goalId = 10,
            contributionMode = HabitGoalContributionMode.HabitAmount,
            contributionValue = 1.0,
        )
        assertEquals(HabitGoalContributionMode.HabitAmount, link1.contributionMode)

        val link2 = HabitGoalLink(
            habitId = 1,
            goalId = 10,
            contributionMode = HabitGoalContributionMode.FixedValue,
            contributionValue = 5.0,
        )
        assertEquals(HabitGoalContributionMode.FixedValue, link2.contributionMode)
        assertEquals(5.0, link2.contributionValue, 0.001)
    }

    @Test
    fun taskGoalLinkContributionIsPositive() {
        val link = TaskGoalLink(taskId = 5, goalId = 2, contributionValue = 3.5)
        assertEquals(5L, link.taskId)
        assertEquals(2L, link.goalId)
        assertEquals(3.5, link.contributionValue, 0.001)
    }

    @Test
    fun habitLibraryLinkMultiplier() {
        val link = HabitLibraryLink(habitId = 4, contentId = 8, contributionMultiplier = 2.0)
        assertEquals(4L, link.habitId)
        assertEquals(8L, link.contentId)
        assertEquals(2.0, link.contributionMultiplier, 0.001)
    }

    @Test
    fun quickAddNaturalLanguageTimeExtraction() {
        val input = "Yarın 14:30 Kitap oku"
        val timeRegex = Regex("""\b([01]?[0-9]|2[0-3]):([0-5][0-9])\b""")
        val match = timeRegex.find(input)
        assertTrue(match != null)
        val (hours, minutes) = match!!.destructured
        val totalMinutes = hours.toInt() * 60 + minutes.toInt()
        assertEquals(14 * 60 + 30, totalMinutes)
    }
}
