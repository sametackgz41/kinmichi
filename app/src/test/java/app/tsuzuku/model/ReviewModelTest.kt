package app.tsuzuku.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReviewModelTest {

    @Test
    fun habitMasteryStagesMapCorrectly() {
        assertEquals(HabitMasteryStage.Seed, HabitMasteryStage.fromDays(0))
        assertEquals(HabitMasteryStage.Seed, HabitMasteryStage.fromDays(7))
        assertEquals(HabitMasteryStage.Sprout, HabitMasteryStage.fromDays(8))
        assertEquals(HabitMasteryStage.Sprout, HabitMasteryStage.fromDays(30))
        assertEquals(HabitMasteryStage.Habit, HabitMasteryStage.fromDays(31))
        assertEquals(HabitMasteryStage.Habit, HabitMasteryStage.fromDays(90))
        assertEquals(HabitMasteryStage.Mastery, HabitMasteryStage.fromDays(91))
        assertEquals(HabitMasteryStage.Mastery, HabitMasteryStage.fromDays(365))
    }

    @Test
    fun skipReasonParsingAndFormatting() {
        val sickFormatted = SkipReasonType.format(SkipReasonType.Sick)
        assertEquals("SKIP_REASON:Hastaydım", sickFormatted)
        val (parsedSick, label) = SkipReasonType.parse(sickFormatted)
        assertEquals(SkipReasonType.Sick, parsedSick)
        assertEquals("Hastaydım", label)

        val vacationLegacy = SkipReasonType.parse("VACATION")
        assertEquals(SkipReasonType.Travel, vacationLegacy.first)

        val customOther = SkipReasonType.format(SkipReasonType.Other, "Toplantı uzadı")
        assertEquals("SKIP_REASON:Toplantı uzadı", customOther)
        val (parsedOther, customLabel) = SkipReasonType.parse(customOther)
        assertEquals(SkipReasonType.Other, parsedOther)
        assertEquals("Toplantı uzadı", customLabel)
    }

    @Test
    fun weeklySummaryChangeRateCalculation() {
        val summary = WeeklySummary(
            weekStart = LocalDate.of(2026, 9, 1),
            habitCompletionRate = 0.85f,
            prevHabitCompletionRate = 0.70f,
            bestHabit = null,
            strugglingHabit = null,
            plannedTasksCount = 10,
            completedTasksCount = 8,
            carriedTasksCount = 2,
            focusDurationMinutes = 120,
            goalProgressCount = 3,
        )
        assertEquals(15, summary.changeRatePercentage)
    }

    @Test
    fun personalMilestoneProgressCalculation() {
        val milestone = PersonalMilestone(
            id = "streak_30",
            title = "30 Gün Serisi",
            description = "30 gün kesintisiz devam et",
            icon = "🔥",
            currentValue = 15,
            targetValue = 30,
            isUnlocked = false,
        )
        assertEquals(0.5f, milestone.progressFraction, 0.001f)
        assertFalse(milestone.isUnlocked)

        val completedMilestone = milestone.copy(currentValue = 30, isUnlocked = true)
        assertEquals(1.0f, completedMilestone.progressFraction, 0.001f)
        assertTrue(completedMilestone.isUnlocked)
    }

    @Test
    fun activityHeatmapDayActivityUnits() {
        val heatmapDay = ActivityHeatmapDay(
            date = LocalDate.of(2026, 9, 1),
            intensity = 3,
            habitsCompleted = 3,
            tasksCompleted = 4,
            focusMinutes = 50,
            dailyReviewDone = true,
        )
        // (3 * 2) + 4 + (50 / 25) + 1 = 6 + 4 + 2 + 1 = 13 units
        assertEquals(13, heatmapDay.totalActivityUnits)
    }

    @Test
    fun extendedMilestonesProgressCappedAtOne() {
        val milestone365 = PersonalMilestone(
            id = "streak_365",
            title = "365 Günlük Efsane",
            description = "Tam 1 yıl boyunca kesintisiz devamlılık.",
            icon = "👑",
            currentValue = 400,
            targetValue = 365,
            isUnlocked = true,
        )
        assertEquals(1.0f, milestone365.progressFraction, 0.001f)
        assertTrue(milestone365.isUnlocked)

        val milestone1000 = PersonalMilestone(
            id = "completions_1000",
            title = "1000 Alışkanlık Adımı",
            description = "Toplam 1000 kez alışkanlık tamamla.",
            icon = "🌟",
            currentValue = 250,
            targetValue = 1000,
            isUnlocked = false,
        )
        assertEquals(0.25f, milestone1000.progressFraction, 0.001f)
        assertFalse(milestone1000.isUnlocked)
    }
}
