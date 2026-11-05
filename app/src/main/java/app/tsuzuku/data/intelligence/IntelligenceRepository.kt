package app.tsuzuku.data.intelligence

import app.tsuzuku.data.local.FocusDao
import app.tsuzuku.data.local.GoalDao
import app.tsuzuku.data.local.HabitDao
import app.tsuzuku.data.local.HabitEntryEntity
import app.tsuzuku.data.local.PlanChecklistItemEntity
import app.tsuzuku.data.local.PlanDao
import app.tsuzuku.data.local.ReviewDao
import app.tsuzuku.data.preferences.AppSettings
import app.tsuzuku.model.AskTsuzukuMessage
import app.tsuzuku.model.CompactAnalyticsSummary
import app.tsuzuku.model.GoalAnalyticsItem
import app.tsuzuku.model.HabitAnalyticsItem
import app.tsuzuku.model.Insight
import app.tsuzuku.model.InsightPeriod
import app.tsuzuku.model.PlanDraftProposal
import app.tsuzuku.model.PlanMode
import app.tsuzuku.model.Recommendation
import app.tsuzuku.model.RecommendationActionType
import app.tsuzuku.model.SkipReasonType
import app.tsuzuku.model.WeeklyAiCommentary
import app.tsuzuku.model.WeeklySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

interface IntelligenceRepository {
    fun observeInsights(): Flow<List<Insight>>
    fun observeTodayInsights(): Flow<List<Insight>>
    suspend fun applyRecommendation(recommendation: Recommendation)
    suspend fun getCompactAnalyticsSummary(settings: AppSettings): CompactAnalyticsSummary
    suspend fun answerQuestion(
        question: String,
        settings: AppSettings,
        chatHistory: List<AskTsuzukuMessage> = emptyList(),
    ): Result<AskTsuzukuMessage>
    suspend fun generatePlanDraft(
        prompt: String,
        mode: PlanMode = PlanMode.Balanced,
        settings: AppSettings,
    ): Result<PlanDraftProposal>
    suspend fun generateWeeklyReviewCommentary(
        summary: WeeklySummary,
        settings: AppSettings,
    ): Result<WeeklyAiCommentary>
    suspend fun explainInsight(insight: Insight, settings: AppSettings): Result<String>
}

class RoomIntelligenceRepository(
    private val habitDao: HabitDao,
    private val planDao: PlanDao,
    private val goalDao: GoalDao,
    private val focusDao: FocusDao,
    private val reviewDao: ReviewDao,
    private val today: () -> LocalDate = LocalDate::now,
) : IntelligenceRepository {

    private val ruleBasedProvider = RuleBasedIntelligenceProvider()

    private fun getProvider(settings: AppSettings): IntelligenceProvider {
        return if (settings.cloudAiEnabled && settings.geminiApiKey.isNotBlank()) {
            GeminiCloudIntelligenceProvider(
                apiKeyProvider = { settings.geminiApiKey },
                modelProvider = { settings.cloudAiModel },
                fallbackProvider = ruleBasedProvider,
            )
        } else {
            ruleBasedProvider
        }
    }

    override fun observeInsights(): Flow<List<Insight>> {
        val f1 = combine(
            habitDao.observeActiveHabits(),
            habitDao.observeAllEntries(),
            planDao.observeTasks(),
            ::Triple,
        )
        val f2 = combine(
            goalDao.observeGoals(),
            focusDao.observeSessions(),
            reviewDao.observeDailyReviews(),
            ::Triple,
        )
        return combine(f1, f2) { (habits, entries, tasks), (goals, sessions, reviews) ->
            InsightEngine.generateInsights(
                habits = habits,
                habitEntries = entries,
                tasks = tasks,
                goals = goals,
                focusSessions = sessions,
                dailyReviews = reviews,
                today = today(),
            )
        }
    }

    override fun observeTodayInsights(): Flow<List<Insight>> {
        return observeInsights().map { insights ->
            insights.filter { it.period == InsightPeriod.Today }
                .take(3)
        }
    }

    override suspend fun applyRecommendation(recommendation: Recommendation) {
        val now = today()
        when (recommendation.actionType) {
            RecommendationActionType.RescheduleTask -> {
                val taskId = recommendation.actionPayload?.toLongOrNull() ?: return
                val task = planDao.getTask(taskId) ?: return
                planDao.updateTask(task.copy(scheduledEpochDay = now.toEpochDay(), updatedAt = System.currentTimeMillis()))
            }
            RecommendationActionType.SplitTask -> {
                val taskId = recommendation.actionPayload?.toLongOrNull() ?: return
                val defaults = listOf("1. Hazırlık ve Taslak", "2. Ana Görevi Gerçekleştir", "3. Son Kontrol")
                defaults.forEachIndexed { index, title ->
                    planDao.insertChecklistItem(
                        PlanChecklistItemEntity(
                            taskId = taskId,
                            title = title,
                            completed = false,
                            sortOrder = index,
                        )
                    )
                }
            }
            RecommendationActionType.AdjustHabitTarget -> {
                val habitId = recommendation.actionPayload?.toLongOrNull() ?: return
                val habit = habitDao.getHabit(habitId) ?: return
                if (habit.target > 1) {
                    val newTarget = (habit.target * 0.75).coerceAtLeast(1.0)
                    habitDao.insertHabit(habit.copy(target = newTarget, updatedAt = System.currentTimeMillis()))
                }
            }
            RecommendationActionType.StartFocusSession -> {
                // Editorial action, no DB write needed
            }
        }
    }

    override suspend fun getCompactAnalyticsSummary(settings: AppSettings): CompactAnalyticsSummary {
        val now = today()
        val thirtyDaysAgo = now.minusDays(30)
        val startDate = thirtyDaysAgo.toString()
        val habits = habitDao.observeActiveHabits().first()
        val entries = habitDao.observeAllEntries().first().filter { it.localDate >= startDate }
        val activeTasks = planDao.observeTasks().first()
        val goals = goalDao.observeGoals().first()
        val sessions = focusDao.observeSessions().first().filter { it.startedAt >= thirtyDaysAgo.toEpochDay() * 86400000L }

        val habitAnalytics = habits.map { habit ->
            val habitEntries = entries.filter { it.habitId == habit.id }
            val completedDays = habitEntries.count { it.status == HabitEntryEntity.STATUS_COMPLETED }
            val rate = (completedDays * 100 / 30).coerceIn(0, 100)

            var streak = 0
            var checkDate = now
            while (true) {
                val dateStr = checkDate.toString()
                val e = entries.firstOrNull { it.habitId == habit.id && it.localDate == dateStr }
                if (e != null && e.status == HabitEntryEntity.STATUS_COMPLETED) {
                    streak++
                    checkDate = checkDate.minusDays(1)
                } else break
            }

            val skippedList = habitEntries.filter { it.status == HabitEntryEntity.STATUS_SKIPPED }
            val dominantSkip = skippedList.map { SkipReasonType.parse(it.note).first }
                .groupBy { it }
                .maxByOrNull { it.value.size }?.key?.defaultLabel

            val displayName = if (settings.aiShareHabitTitles) habit.name else "Alışkanlık ${habit.id.toString().take(4)}"
            HabitAnalyticsItem(
                name = displayName,
                completionRatePercent = rate,
                currentStreak = streak,
                skipReason = dominantSkip,
            )
        }

        val overallRate = if (habitAnalytics.isNotEmpty()) habitAnalytics.map { it.completionRatePercent }.average().toInt() else 0
        val bestHabit = habitAnalytics.maxByOrNull { it.currentStreak }?.name
        val strugglingHabit = habitAnalytics.filter { it.completionRatePercent < 50 }.minByOrNull { it.completionRatePercent }?.name

        val skippedEntries = entries.filter { it.status == HabitEntryEntity.STATUS_SKIPPED }
        val skipReasons = skippedEntries.map { SkipReasonType.parse(it.note).first.defaultLabel }
            .groupBy { it }
            .mapValues { it.value.size }

        val carriedTasks = activeTasks.filter { it.carryCount > 0 }
        val chronicDeferred = activeTasks.filter { it.carryCount >= 3 }
            .map { if (settings.aiShareTaskTitles) it.title else "Görev ${it.id.toString().take(4)}" }

        val totalFocusMinutes = sessions.sumOf { it.durationMinutes }
        val totalFocusHours = totalFocusMinutes / 60

        val goalAnalytics = goals.map { goal ->
            val displayName = if (settings.aiShareTaskTitles) goal.title else "Hedef ${goal.id.toString().take(4)}"
            val progress = if (goal.targetValue > 0) ((goal.currentValue / goal.targetValue) * 100).toInt().coerceIn(0, 100) else 0
            GoalAnalyticsItem(
                title = displayName,
                progressPercent = progress,
                isOverdueOrStalled = goal.dueDateEpochDay != null && goal.dueDateEpochDay < now.toEpochDay() && progress < 100,
            )
        }

        val activeInsights = observeTodayInsights().first().map { it.title }

        return CompactAnalyticsSummary(
            periodDays = 30,
            totalHabitsCount = habits.size,
            activeHabitsSummary = habitAnalytics,
            overallHabitCompletionRatePercent = overallRate,
            bestStreakHabit = bestHabit,
            strugglingHabit = strugglingHabit,
            topSkipReasons = skipReasons,
            activeTasksCount = activeTasks.size,
            carriedTasksCount = carriedTasks.size,
            chronicDeferredTasks = chronicDeferred,
            totalFocusHours = totalFocusHours,
            activeGoalsSummary = goalAnalytics,
            topActiveInsights = activeInsights,
        )
    }

    override suspend fun answerQuestion(
        question: String,
        settings: AppSettings,
        chatHistory: List<AskTsuzukuMessage>,
    ): Result<AskTsuzukuMessage> {
        val summary = getCompactAnalyticsSummary(settings)
        return getProvider(settings).answerQuestion(question, summary, chatHistory)
    }

    override suspend fun generatePlanDraft(
        prompt: String,
        mode: PlanMode,
        settings: AppSettings,
    ): Result<PlanDraftProposal> {
        val summary = getCompactAnalyticsSummary(settings)
        return getProvider(settings).generatePlanDraft(prompt, mode, summary)
    }

    override suspend fun generateWeeklyReviewCommentary(
        summary: WeeklySummary,
        settings: AppSettings,
    ): Result<WeeklyAiCommentary> {
        val analyticsSummary = getCompactAnalyticsSummary(settings)
        return getProvider(settings).generateWeeklyReviewCommentary(summary, analyticsSummary)
    }

    override suspend fun explainInsight(insight: Insight, settings: AppSettings): Result<String> {
        val summary = getCompactAnalyticsSummary(settings)
        return getProvider(settings).explainInsight(insight, summary)
    }
}
