package app.tsuzuku.model

enum class InsightType(val label: String, val icon: String) {
    TimeOfDay("Zaman Kalıbı", "⏰"),
    TaskPacing("Planlama Yanılgısı", "⏱️"),
    HabitPattern("Alışkanlık Ritmi", "🌱"),
    CarryRisk("Erteleme Uyarısı", "⏳"),
    WellnessCorrelation("Ruh Hali & Enerji", "☀️"),
    FocusPattern("Derin Odak", "🎯"),
    GoalVelocity("Hedef İlerlemesi", "🏁"),
    HabitSkipReason("Atlama Analizi", "⏸️"),
}

enum class InsightConfidence(val label: String, val score: Float) {
    High("Yüksek Güven", 0.9f),
    Medium("Orta Güven", 0.65f),
    Low("Düşük Güven", 0.4f);
}

enum class InsightPeriod(val label: String) {
    Today("Bugün"),
    Weekly("Haftalık"),
    Monthly("Aylık"),
    Trend("Genel Trend"),
}

enum class RecommendationActionType {
    RescheduleTask,
    SplitTask,
    AdjustHabitTarget,
    StartFocusSession,
}

data class Recommendation(
    val id: String,
    val title: String,
    val description: String,
    val actionType: RecommendationActionType,
    val actionPayload: String? = null,
    val suggestedChangeText: String,
)

data class Insight(
    val id: String,
    val type: InsightType,
    val title: String,
    val description: String,
    val confidence: InsightConfidence,
    val whyWeSayThis: String,
    val supportingMetrics: Map<String, String> = emptyMap(),
    val recommendation: Recommendation? = null,
    val period: InsightPeriod = InsightPeriod.Today,
    val createdAt: Long = System.currentTimeMillis(),
)
