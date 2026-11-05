package app.tsuzuku.data.intelligence

import app.tsuzuku.model.AskTsuzukuMessage
import app.tsuzuku.model.CompactAnalyticsSummary
import app.tsuzuku.model.GoalAnalyticsItem
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitAnalyticsItem
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.Insight
import app.tsuzuku.model.InsightConfidence
import app.tsuzuku.model.InsightType
import app.tsuzuku.model.MessageSender
import app.tsuzuku.model.PlanMode
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.model.WeeklySummary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IntelligenceProviderTest {

    private val sampleContext = CompactAnalyticsSummary(
        periodDays = 30,
        totalHabitsCount = 5,
        activeHabitsSummary = listOf(
            HabitAnalyticsItem("Kitap Oku", 90, 14),
            HabitAnalyticsItem("Sabah Koşusu", 40, 2, "Zamanım yoktu"),
        ),
        overallHabitCompletionRatePercent = 78,
        bestStreakHabit = "Kitap Oku",
        strugglingHabit = "Sabah Koşusu",
        topSkipReasons = mapOf("Zamanım yoktu" to 5, "Unuttum" to 2),
        activeTasksCount = 8,
        carriedTasksCount = 4,
        chronicDeferredTasks = listOf("Vergi Raporunu Hazırla"),
        avgTaskDurationMinutes = 35,
        totalFocusHours = 18,
        activeGoalsSummary = listOf(
            GoalAnalyticsItem("Yarı Maraton Koş", 45),
        ),
        topActiveInsights = listOf("Derin Odaklanma Başarısı"),
    )

    private val provider: IntelligenceProvider = RuleBasedIntelligenceProvider()

    private fun createMockHabit(id: Long, name: String) = Habit(
        id = id,
        name = name,
        emoji = "🌱",
        colorArgb = 0xFF4CAF50,
        type = HabitType.Check,
        direction = HabitDirection.Build,
        target = 1.0,
        unit = "kez",
        step = 1.0,
        scheduleDaysMask = 127,
        sortOrder = 0,
    )

    @Test
    fun ruleBasedProviderAnswersEfficiencyQuestionsAccurately() = runBlocking {
        val result = provider.answerQuestion("En verimli olduğum saatler ve odak durumum nedir?", sampleContext)
        assertTrue(result.isSuccess)
        val message = result.getOrNull()
        assertNotNull(message)
        assertEquals(MessageSender.Assistant, message?.sender)
        assertTrue(message!!.content.contains("18 saatlik derin odaklanma"))
        assertEquals("18 saat", message.supportingMetrics["Toplam Odak"])
        assertTrue(message.followUpQuestions.isNotEmpty())
    }

    @Test
    fun ruleBasedProviderIdentifiesStrugglingHabitsAndSkipReasons() = runBlocking {
        val result = provider.answerQuestion("Hangi alışkanlığım aksıyor ve neden?", sampleContext)
        assertTrue(result.isSuccess)
        val message = result.getOrNull()
        assertNotNull(message)
        assertTrue(message!!.content.contains("Sabah Koşusu"))
        assertTrue(message.content.contains("Zamanım yoktu"))
        assertEquals("Sabah Koşusu", message.supportingMetrics["Aksayan Alışkanlık"])
        assertTrue(message.followUpQuestions.isNotEmpty())
    }

    @Test
    fun ruleBasedProviderIdentifiesChronicDeferredTasks() = runBlocking {
        val result = provider.answerQuestion("Ertelenen görevlerimde son durum ne?", sampleContext)
        assertTrue(result.isSuccess)
        val message = result.getOrNull()
        assertNotNull(message)
        assertTrue(message!!.content.contains("Vergi Raporunu Hazırla"))
        assertTrue(message.content.contains("4 kez görev devredilmiş"))
        assertTrue(message.followUpQuestions.isNotEmpty())
    }

    @Test
    fun ruleBasedProviderHandlesMultiTurnContextualQuestions() = runBlocking {
        val chatHistory = listOf(
            AskTsuzukuMessage(sender = MessageSender.User, content = "Hangi alışkanlığım aksıyor?"),
            AskTsuzukuMessage(
                sender = MessageSender.Assistant,
                content = "Son 30 günde en çok zorlandığın alışkanlık Sabah Koşusu.",
                supportingMetrics = mapOf("Aksayan Alışkanlık" to "Sabah Koşusu"),
            ),
        )

        val result = provider.answerQuestion("Bunu nasıl yapabilirim, önerin nedir?", sampleContext, chatHistory)
        assertTrue(result.isSuccess)
        val message = result.getOrNull()
        assertNotNull(message)
        assertTrue(message!!.content.contains("2 Dakika Kuralı") || message.content.contains("sürtünme"))
        assertEquals("Hedefi %50 Küçült", message.supportingMetrics["Öneri"])
    }

    @Test
    fun ruleBasedProviderGeneratesBalancedPlanMode() = runBlocking {
        val result = provider.generatePlanDraft("Yarın için dengeli bir gün tasarla", PlanMode.Balanced, sampleContext)
        assertTrue(result.isSuccess)
        val proposal = result.getOrNull()
        assertNotNull(proposal)
        assertEquals(PlanMode.Balanced, proposal!!.mode)
        assertTrue(proposal.proposedTasks.isNotEmpty())
        val deferredTask = proposal.proposedTasks.firstOrNull { it.title.contains("Vergi Raporunu Hazırla") }
        assertNotNull(deferredTask)
        assertTrue(deferredTask!!.subtasks.isNotEmpty())
    }

    @Test
    fun ruleBasedProviderGeneratesDeepWorkAndLowEnergyModes() = runBlocking {
        // DeepWork mode
        val deepWorkResult = provider.generatePlanDraft("Yarın", PlanMode.DeepWork, sampleContext)
        assertTrue(deepWorkResult.isSuccess)
        val deepWorkProposal = deepWorkResult.getOrNull()
        assertNotNull(deepWorkProposal)
        assertEquals(PlanMode.DeepWork, deepWorkProposal!!.mode)
        assertTrue(deepWorkProposal.proposedTasks.any { it.estimatedMinutes >= 60 })

        // LowEnergy mode
        val lowEnergyResult = provider.generatePlanDraft("Yarın", PlanMode.LowEnergy, sampleContext)
        assertTrue(lowEnergyResult.isSuccess)
        val lowEnergyProposal = lowEnergyResult.getOrNull()
        assertNotNull(lowEnergyProposal)
        assertEquals(PlanMode.LowEnergy, lowEnergyProposal!!.mode)
        assertTrue(lowEnergyProposal.proposedTasks.all { it.estimatedMinutes <= 20 })
    }

    @Test
    fun ruleBasedProviderGeneratesClearDeferralsAndGoalSprintModes() = runBlocking {
        // ClearDeferrals mode
        val deferralsResult = provider.generatePlanDraft("Yarın", PlanMode.ClearDeferrals, sampleContext)
        assertTrue(deferralsResult.isSuccess)
        val deferralsProposal = deferralsResult.getOrNull()
        assertNotNull(deferralsProposal)
        assertEquals(PlanMode.ClearDeferrals, deferralsProposal!!.mode)
        assertTrue(deferralsProposal.proposedTasks.any { it.title.contains("Vergi Raporunu Hazırla") })

        // GoalSprint mode
        val goalSprintResult = provider.generatePlanDraft("Yarın", PlanMode.GoalSprint, sampleContext)
        assertTrue(goalSprintResult.isSuccess)
        val goalSprintProposal = goalSprintResult.getOrNull()
        assertNotNull(goalSprintProposal)
        assertEquals(PlanMode.GoalSprint, goalSprintProposal!!.mode)
        assertTrue(goalSprintProposal.proposedTasks.any { it.title.contains("Yarı Maraton Koş") })
    }

    @Test
    fun ruleBasedProviderGeneratesWeeklyCommentary() = runBlocking {
        val highSummary = WeeklySummary(
            weekStart = LocalDate.now().minusDays(7),
            habitCompletionRate = 0.85f,
            prevHabitCompletionRate = 0.75f,
            bestHabit = TodayHabit(createMockHabit(1L, "Kitap Oku"), 1.0),
            strugglingHabit = null,
            plannedTasksCount = 10,
            completedTasksCount = 9,
            carriedTasksCount = 1,
            focusDurationMinutes = 300,
            goalProgressCount = 2,
        )

        val resultHigh = provider.generateWeeklyReviewCommentary(highSummary, sampleContext)
        assertTrue(resultHigh.isSuccess)
        val commentaryHigh = resultHigh.getOrNull()
        assertNotNull(commentaryHigh)
        assertTrue(commentaryHigh!!.title.contains("Güçlü ve Tutarlı"))
        assertTrue(commentaryHigh.summary.contains("Kitap Oku"))

        val strugglingSummary = WeeklySummary(
            weekStart = LocalDate.now().minusDays(7),
            habitCompletionRate = 0.50f,
            prevHabitCompletionRate = 0.60f,
            bestHabit = TodayHabit(createMockHabit(1L, "Kitap Oku"), 1.0),
            strugglingHabit = TodayHabit(createMockHabit(2L, "Sabah Koşusu"), 0.2),
            plannedTasksCount = 10,
            completedTasksCount = 4,
            carriedTasksCount = 5,
            focusDurationMinutes = 0,
            goalProgressCount = 0,
        )

        val resultStruggling = provider.generateWeeklyReviewCommentary(strugglingSummary, sampleContext)
        assertTrue(resultStruggling.isSuccess)
        val commentaryStruggling = resultStruggling.getOrNull()
        assertNotNull(commentaryStruggling)
        assertTrue(commentaryStruggling!!.title.contains("Sakin ve Yeniden Başlangıç"))
        assertTrue(commentaryStruggling.suggestion.contains("Sabah Koşusu"))
    }

    @Test
    fun ruleBasedProviderExplainsInsightsCalmlyWithoutJudgment() = runBlocking {
        val insight = Insight(
            id = "test_insight_1",
            type = InsightType.CarryRisk,
            title = "Erteleme Kalıbı",
            description = "Görevlerin genellikle haftanın son günlerine yığılıyor.",
            confidence = InsightConfidence.High,
            whyWeSayThis = "Son 3 haftada Cuma günleri planlanan işlerin %40'ı ertesi haftaya aktarıldı.",
        )
        val result = provider.explainInsight(insight, sampleContext)
        assertTrue(result.isSuccess)
        val explanation = result.getOrNull().orEmpty()
        assertTrue(explanation.contains("yargılamak veya mükemmeliyetçilik"))
        assertTrue(explanation.contains("Cuma günleri"))
    }

    @Test
    fun geminiCloudProviderFallsBackCleanlyWhenApiKeyIsBlank() = runBlocking {
        val cloudProvider = GeminiCloudIntelligenceProvider(
            apiKeyProvider = { "" },
            modelProvider = { "gemini-1.5-flash" },
            fallbackProvider = provider,
        )

        val result = cloudProvider.answerQuestion("Genel durumum nasıl?", sampleContext)
        assertTrue(result.isSuccess)
        val message = result.getOrNull()
        assertNotNull(message)
        assertTrue(message!!.content.contains("Tsuzuku analizi"))
    }
}
