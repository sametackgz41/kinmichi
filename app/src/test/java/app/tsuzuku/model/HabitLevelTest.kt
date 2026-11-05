package app.tsuzuku.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitLevelTest {
    private val date = LocalDate.of(2026, 8, 10)

    private val baseHabit = Habit(
        id = 1,
        name = "Koşu",
        emoji = "🏃",
        colorArgb = 0xFF5068A9,
        type = HabitType.Count,
        direction = HabitDirection.Build,
        target = 1.0,
        unit = "km",
        step = 0.5,
        scheduleDaysMask = 127,
        sortOrder = 0,
        currentLevel = 1,
    )

    @Test
    fun habitLevelCreationAndProperties() {
        val level1 = HabitLevel(id = 1, habitId = 1, levelNumber = 1, name = "Başlangıç", target = 1.0, rewardNote = "İlk adım!")
        val level2 = HabitLevel(id = 2, habitId = 1, levelNumber = 2, name = "Orta Seviye", target = 3.0, minimumTarget = 2.0, maximumTarget = 5.0)
        val level3 = HabitLevel(id = 3, habitId = 1, levelNumber = 3, name = "İleri Seviye", target = 5.0)

        assertEquals(1, level1.levelNumber)
        assertEquals("Başlangıç", level1.name)
        assertEquals(1.0, level1.target, 0.001)
        assertEquals("İlk adım!", level1.rewardNote)

        assertEquals(2, level2.levelNumber)
        assertEquals(3.0, level2.target, 0.001)
        assertEquals(2.0, level2.minimumTarget, 0.001)
        assertEquals(5.0, level2.maximumTarget!!, 0.001)

        assertEquals(3, level3.levelNumber)
        assertEquals(5.0, level3.target, 0.001)
    }

    @Test
    fun todayHabitWithLevelsReflectsLevelState() {
        val habitWithLevels = baseHabit.copy(currentLevel = 2)
        val todayHabit = TodayHabit(
            habit = habitWithLevels,
            progress = 3.0,
            hasLevels = true,
            currentLevelNumber = 2,
            currentLevelName = "Orta Seviye",
        )

        assertTrue(todayHabit.hasLevels)
        assertEquals(2, todayHabit.currentLevelNumber)
        assertEquals("Orta Seviye", todayHabit.currentLevelName)
    }

    @Test
    fun quitHabitUnderLimitIsCompleteAndNotExceeded() {
        val quitHabit = Habit(
            id = 2,
            name = "Sosyal Medya",
            emoji = "📱",
            colorArgb = 0xFF5068A9,
            type = HabitType.Duration,
            direction = HabitDirection.Quit,
            target = 60.0,
            unit = "dk",
            step = 5.0,
            scheduleDaysMask = 127,
            sortOrder = 1,
            maximumTarget = 60.0,
        )

        val todayHabit = TodayHabit(habit = quitHabit, progress = 30.0)
        assertTrue(todayHabit.isComplete)
        assertFalse(todayHabit.badHabitLimitExceeded)
        assertFalse(todayHabit.badHabitLimitWarning)
        assertTrue(todayHabit.isIdealRange)
    }

    @Test
    fun quitHabitApproachingLimitTriggersWarning() {
        val quitHabit = Habit(
            id = 2,
            name = "Sosyal Medya",
            emoji = "📱",
            colorArgb = 0xFF5068A9,
            type = HabitType.Duration,
            direction = HabitDirection.Quit,
            target = 60.0,
            unit = "dk",
            step = 5.0,
            scheduleDaysMask = 127,
            sortOrder = 1,
            maximumTarget = 60.0,
        )

        // 80% of 60 is 48. Progress 50 should trigger warning, but not exceeded.
        val todayHabit = TodayHabit(habit = quitHabit, progress = 50.0)
        assertTrue(todayHabit.isComplete)
        assertFalse(todayHabit.badHabitLimitExceeded)
        assertTrue(todayHabit.badHabitLimitWarning)
        assertTrue(todayHabit.isIdealRange)
    }

    @Test
    fun quitHabitExceedingLimitMarksExceededAndIncomplete() {
        val quitHabit = Habit(
            id = 2,
            name = "Sosyal Medya",
            emoji = "📱",
            colorArgb = 0xFF5068A9,
            type = HabitType.Duration,
            direction = HabitDirection.Quit,
            target = 60.0,
            unit = "dk",
            step = 5.0,
            scheduleDaysMask = 127,
            sortOrder = 1,
            maximumTarget = 60.0,
        )

        // Progress 65 exceeds 60
        val todayHabit = TodayHabit(habit = quitHabit, progress = 65.0)
        assertFalse(todayHabit.isComplete)
        assertTrue(todayHabit.badHabitLimitExceeded)
        assertFalse(todayHabit.badHabitLimitWarning)
        assertFalse(todayHabit.isIdealRange)
    }

    @Test
    fun idealRangeWithMinimumAndMaximumTargets() {
        val quitHabitWithRange = Habit(
            id = 3,
            name = "Kahve",
            emoji = "☕",
            colorArgb = 0xFF5068A9,
            type = HabitType.Count,
            direction = HabitDirection.Quit,
            target = 2.0,
            unit = "fincan",
            step = 1.0,
            scheduleDaysMask = 127,
            sortOrder = 2,
            minimumTarget = 1.0,
            maximumTarget = 3.0,
        )

        // Progress 0 is below minimum (not ideal range)
        val underMin = TodayHabit(habit = quitHabitWithRange, progress = 0.0)
        assertFalse(underMin.isIdealRange)

        // Progress 2 is between min (1.0) and max (3.0) -> ideal range
        val inRange = TodayHabit(habit = quitHabitWithRange, progress = 2.0)
        assertTrue(inRange.isIdealRange)

        // Progress 4 exceeds max (3.0) -> not ideal range
        val exceeded = TodayHabit(habit = quitHabitWithRange, progress = 4.0)
        assertFalse(exceeded.isIdealRange)
        assertTrue(exceeded.badHabitLimitExceeded)
    }

    @Test
    fun streakPreservationForQuitHabits() {
        val quitHabit = Habit(
            id = 4,
            name = "Şeker Tüketimi",
            emoji = "🍬",
            colorArgb = 0xFF5068A9,
            type = HabitType.Count,
            direction = HabitDirection.Quit,
            target = 5.0,
            unit = "adet",
            step = 1.0,
            scheduleDaysMask = 127,
            sortOrder = 3,
            maximumTarget = 5.0,
            createdOn = date,
        )

        // Day 1: 3 pieces (under limit -> completed)
        // Day 2: 4 pieces (under limit -> completed)
        val snapshotSuccess = HabitHistorySnapshot(
            habits = listOf(quitHabit),
            records = listOf(
                HabitHistoryRecord(quitHabit.id, date, 3.0, HabitDayStatus.Pending),
                HabitHistoryRecord(quitHabit.id, date.plusDays(1), 4.0, HabitDayStatus.Pending),
            ),
        )
        val statsSuccess = snapshotSuccess.statistics(days = 2, endDate = date.plusDays(1))
        assertEquals(2, statsSuccess.currentStreak)
        assertEquals(2, statsSuccess.bestStreak)
        assertEquals(2, statsSuccess.completedCount)

        // Day 1: 3 pieces (under limit)
        // Day 2: 8 pieces (exceeded limit -> fails streak)
        val snapshotBroken = HabitHistorySnapshot(
            habits = listOf(quitHabit),
            records = listOf(
                HabitHistoryRecord(quitHabit.id, date, 3.0, HabitDayStatus.Pending),
                HabitHistoryRecord(quitHabit.id, date.plusDays(1), 8.0, HabitDayStatus.Pending),
            ),
        )
        val statsBroken = snapshotBroken.statistics(days = 2, endDate = date.plusDays(1))
        assertEquals(0, statsBroken.currentStreak)
        assertEquals(1, statsBroken.bestStreak)
        assertEquals(1, statsBroken.completedCount)
    }
}
