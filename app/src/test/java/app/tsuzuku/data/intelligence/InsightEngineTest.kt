package app.tsuzuku.data.intelligence

import app.tsuzuku.data.local.DailyReviewEntity
import app.tsuzuku.data.local.FocusSessionEntity
import app.tsuzuku.data.local.GoalEntity
import app.tsuzuku.data.local.HabitEntity
import app.tsuzuku.data.local.HabitEntryEntity
import app.tsuzuku.data.local.PlanTaskEntity
import app.tsuzuku.model.InsightConfidence
import app.tsuzuku.model.InsightType
import app.tsuzuku.model.RecommendationActionType
import app.tsuzuku.model.SkipReasonType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InsightEngineTest {

    @Test
    fun returnsEmptyWhenInsufficientData() {
        val insights = InsightEngine.generateInsights(
            habits = emptyList(),
            habitEntries = emptyList(),
            tasks = emptyList(),
            goals = emptyList(),
            focusSessions = emptyList(),
            dailyReviews = emptyList(),
        )
        assertTrue(insights.isEmpty())
    }

    @Test
    fun detectsCarryRiskForRepeatedlyDeferredTask() {
        val tasks = listOf(
            PlanTaskEntity(
                id = 101L,
                title = "Finansal Tabloları Hazırla",
                status = "Active",
                carryCount = 4,
                createdAt = 1000L,
                updatedAt = 2000L,
            ),
            PlanTaskEntity(
                id = 102L,
                title = "Diş hekimini ara",
                status = "Active",
                carryCount = 1,
                createdAt = 1000L,
                updatedAt = 2000L,
            ),
        )

        val insights = InsightEngine.generateInsights(
            habits = emptyList(),
            habitEntries = emptyList(),
            tasks = tasks,
            goals = emptyList(),
            focusSessions = emptyList(),
            dailyReviews = emptyList(),
        )

        val carryInsight = insights.firstOrNull { it.type == InsightType.CarryRisk }
        assertNotNull(carryInsight)
        assertEquals(InsightConfidence.High, carryInsight?.confidence)
        assertTrue(carryInsight!!.title.contains("Finansal Tabloları"))
        assertNotNull(carryInsight.recommendation)
        assertEquals(RecommendationActionType.SplitTask, carryInsight.recommendation?.actionType)
    }

    @Test
    fun detectsTaskPlanningFallacyWhenTasksTakeLongerThanEstimated() {
        val tasks = listOf(
            PlanTaskEntity(id = 1L, title = "T1", status = "Completed", estimatedMinutes = 30, actualMinutes = 60, createdAt = 1L, updatedAt = 2L),
            PlanTaskEntity(id = 2L, title = "T2", status = "Completed", estimatedMinutes = 20, actualMinutes = 45, createdAt = 1L, updatedAt = 2L),
            PlanTaskEntity(id = 3L, title = "T3", status = "Completed", estimatedMinutes = 40, actualMinutes = 80, createdAt = 1L, updatedAt = 2L),
            PlanTaskEntity(id = 4L, title = "T4", status = "Completed", estimatedMinutes = 30, actualMinutes = 55, createdAt = 1L, updatedAt = 2L),
            PlanTaskEntity(id = 5L, title = "T5", status = "Completed", estimatedMinutes = 15, actualMinutes = 35, createdAt = 1L, updatedAt = 2L),
        )

        val insights = InsightEngine.generateInsights(
            habits = emptyList(),
            habitEntries = emptyList(),
            tasks = tasks,
            goals = emptyList(),
            focusSessions = emptyList(),
            dailyReviews = emptyList(),
        )

        val pacingInsight = insights.firstOrNull { it.type == InsightType.TaskPacing }
        assertNotNull(pacingInsight)
        assertTrue(pacingInsight!!.whyWeSayThis.isNotEmpty())
    }

    @Test
    fun detectsFocusPatternWithHighCompletionRate() {
        val sessions = (1..6).map { i ->
            FocusSessionEntity(
                id = i.toLong(),
                taskId = null,
                habitId = null,
                goalId = null,
                mode = "Pomodoro",
                startedAt = 1000L + (i * 3600_000L),
                endedAt = 1000L + (i * 3600_000L) + (30 * 60_000L),
                durationMinutes = 30,
                completed = true,
                interrupted = false,
                createdAt = 1000L,
            )
        }

        val insights = InsightEngine.generateInsights(
            habits = emptyList(),
            habitEntries = emptyList(),
            tasks = emptyList(),
            goals = emptyList(),
            focusSessions = sessions,
            dailyReviews = emptyList(),
        )

        val focusInsight = insights.firstOrNull { it.type == InsightType.FocusPattern }
        assertNotNull(focusInsight)
        assertEquals(InsightConfidence.Medium, focusInsight?.confidence)
    }

    @Test
    fun detectsWellnessCorrelationBetweenMoodAndHabits() {
        val entries = mutableListOf<HabitEntryEntity>()
        var entryId = 1L

        // Days 1..5: 3 habits completed per day
        for (day in 1..5) {
            for (habitId in 1L..3L) {
                entries += HabitEntryEntity(
                    id = entryId++,
                    habitId = habitId,
                    localDate = LocalDate.of(2026, 8, day).toString(),
                    amount = 1.0,
                    status = "COMPLETED",
                    createdAt = 1000L,
                    updatedAt = 1000L,
                )
            }
        }
        // Days 6..10: 0 habits completed

        val dailyReviews = mutableListOf<DailyReviewEntity>()
        // High habit days: energy = 5
        for (day in 1..5) {
            dailyReviews += DailyReviewEntity(
                epochDay = LocalDate.of(2026, 8, day).toEpochDay(),
                mood = 5,
                energy = 5,
                productivity = 5,
                stress = 1,
                note = "Çok enerjik gün",
                createdAt = 1000L,
                updatedAt = 1000L,
            )
        }
        // Low habit days: energy = 2
        for (day in 6..10) {
            dailyReviews += DailyReviewEntity(
                epochDay = LocalDate.of(2026, 8, day).toEpochDay(),
                mood = 2,
                energy = 2,
                productivity = 2,
                stress = 4,
                note = "Yorgun gün",
                createdAt = 1000L,
                updatedAt = 1000L,
            )
        }

        val insights = InsightEngine.generateInsights(
            habits = emptyList(),
            habitEntries = entries,
            tasks = emptyList(),
            goals = emptyList(),
            focusSessions = emptyList(),
            dailyReviews = dailyReviews,
        )

        val wellnessInsight = insights.firstOrNull { it.type == InsightType.WellnessCorrelation }
        assertNotNull(wellnessInsight)
        assertTrue(wellnessInsight!!.confidence == InsightConfidence.High)
    }

    @Test
    fun detectsHabitSkipReasonPatternsAndRecommendsAction() {
        val today = LocalDate.now()
        val habits = listOf(
            HabitEntity(
                id = 10L,
                name = "Kitap Oku",
                emoji = "📖",
                colorArgb = 0xFF4A6B5D,
                type = "Check",
                direction = "Build",
                target = 1.0,
                unit = "",
                step = 1.0,
                scheduleDaysMask = 127,
                sortOrder = 0,
                isArchived = false,
                createdAt = 1000L,
                updatedAt = 1000L,
            ),
        )

        // 3 skips with NoTime
        val entries = listOf(
            HabitEntryEntity(
                habitId = 10L,
                localDate = today.minusDays(1).toString(),
                amount = 0.0,
                status = HabitEntryEntity.STATUS_SKIPPED,
                note = SkipReasonType.format(SkipReasonType.NoTime),
                createdAt = 1000L,
                updatedAt = 1000L,
            ),
            HabitEntryEntity(
                habitId = 10L,
                localDate = today.minusDays(2).toString(),
                amount = 0.0,
                status = HabitEntryEntity.STATUS_SKIPPED,
                note = SkipReasonType.format(SkipReasonType.NoTime),
                createdAt = 1000L,
                updatedAt = 1000L,
            ),
            HabitEntryEntity(
                habitId = 10L,
                localDate = today.minusDays(3).toString(),
                amount = 0.0,
                status = HabitEntryEntity.STATUS_SKIPPED,
                note = SkipReasonType.format(SkipReasonType.NoTime),
                createdAt = 1000L,
                updatedAt = 1000L,
            ),
        )

        val insights = InsightEngine.generateInsights(
            habits = habits,
            habitEntries = entries,
            tasks = emptyList(),
            goals = emptyList(),
            focusSessions = emptyList(),
            dailyReviews = emptyList(),
        )

        val skipInsight = insights.firstOrNull { it.type == InsightType.HabitSkipReason }
        assertNotNull(skipInsight)
        assertEquals(InsightConfidence.Medium, skipInsight?.confidence)
        assertTrue(skipInsight!!.title.contains("Kitap Oku"))
        assertTrue(skipInsight.description.contains("Zamanım yoktu"))
        assertNotNull(skipInsight.recommendation)
        assertEquals(RecommendationActionType.AdjustHabitTarget, skipInsight.recommendation?.actionType)
    }
}
