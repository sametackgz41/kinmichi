package app.tsuzuku.model

import java.util.UUID

enum class MessageSender {
    User,
    Assistant,
}

data class AskTsuzukuMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val supportingMetrics: Map<String, String> = emptyMap(),
    val followUpQuestions: List<String> = emptyList(),
)

enum class PlanMode(val title: String, val iconEmoji: String, val description: String) {
    Balanced("Dengeli Gün", "⚖️", "Alışkanlıklar, odak ve dinlenme arasında uyumlu dağılım"),
    DeepWork("Derin Çalışma", "🚀", "Yüksek zihinsel odak gerektiren 45-60 dk'lık bloklar"),
    LowEnergy("Düşük Enerji", "🌿", "Tükenmiş hissettiğin günler için 15-20 dk'lık hafif mikro adımlar"),
    ClearDeferrals("Ertelenenleri Bitir", "🧹", "Sürekli sarkan görevleri parçalayarak yükü hafifletme"),
    GoalSprint("Hedef Odaklı", "🎯", "Uzun vadeli hedeflerine doğrudan katkı sunan görevler"),
}

data class HabitAnalyticsItem(
    val name: String,
    val completionRatePercent: Int,
    val currentStreak: Int,
    val skipReason: String? = null,
)

data class GoalAnalyticsItem(
    val title: String,
    val progressPercent: Int,
    val isOverdueOrStalled: Boolean = false,
)

data class CompactAnalyticsSummary(
    val periodDays: Int = 30,
    val totalHabitsCount: Int = 0,
    val activeHabitsSummary: List<HabitAnalyticsItem> = emptyList(),
    val overallHabitCompletionRatePercent: Int = 0,
    val bestStreakHabit: String? = null,
    val strugglingHabit: String? = null,
    val topSkipReasons: Map<String, Int> = emptyMap(),
    val activeTasksCount: Int = 0,
    val carriedTasksCount: Int = 0,
    val chronicDeferredTasks: List<String> = emptyList(),
    val avgTaskDurationMinutes: Int = 0,
    val totalFocusHours: Int = 0,
    val activeGoalsSummary: List<GoalAnalyticsItem> = emptyList(),
    val topActiveInsights: List<String> = emptyList(),
)

data class ProposedTaskDraft(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val estimatedMinutes: Int = 30,
    val scheduledEpochDay: Long? = null,
    val startMinutes: Int? = null,
    val dueMinutes: Int? = null,
    val subtasks: List<String> = emptyList(),
    val isSelected: Boolean = true,
)

data class PlanDraftProposal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val rationale: String,
    val proposedTasks: List<ProposedTaskDraft>,
    val mode: PlanMode = PlanMode.Balanced,
    val createdAt: Long = System.currentTimeMillis(),
)

data class WeeklyAiCommentary(
    val title: String,
    val summary: String,
    val suggestion: String,
    val focusHighlight: String,
    val generatedAt: Long = System.currentTimeMillis(),
)

enum class AiProviderType(val displayName: String) {
    RuleBased("Çevrimdışı Kural Motoru (Yerel)"),
    GeminiCloud("Google Gemini (Bulut)"),
}
