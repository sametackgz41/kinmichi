package app.tsuzuku.data.intelligence

import app.tsuzuku.data.local.DailyReviewEntity
import app.tsuzuku.data.local.FocusSessionEntity
import app.tsuzuku.data.local.GoalEntity
import app.tsuzuku.data.local.HabitEntity
import app.tsuzuku.data.local.HabitEntryEntity
import app.tsuzuku.data.local.PlanTaskEntity
import app.tsuzuku.model.Insight
import app.tsuzuku.model.InsightConfidence
import app.tsuzuku.model.InsightPeriod
import app.tsuzuku.model.InsightType
import app.tsuzuku.model.Recommendation
import app.tsuzuku.model.RecommendationActionType
import app.tsuzuku.model.SkipReasonType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object InsightEngine {

    fun generateInsights(
        habits: List<HabitEntity>,
        habitEntries: List<HabitEntryEntity>,
        tasks: List<PlanTaskEntity>,
        goals: List<GoalEntity>,
        focusSessions: List<FocusSessionEntity>,
        dailyReviews: List<DailyReviewEntity>,
        today: LocalDate = LocalDate.now(),
        locale: Locale = Locale.forLanguageTag("tr"),
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        // 1. Chronic Deferral / Carry Risk Analysis (High Priority for Today)
        analyzeCarryRisk(tasks, insights)

        // 2. Time of Day Performance
        analyzeTimeOfDay(tasks, insights)

        // 3. Task Pacing & Planning Fallacy (Estimated vs Actual)
        analyzeTaskPacing(tasks, insights)

        // 4. Day-of-Week Habit Rhythm
        analyzeHabitPatterns(habits, habitEntries, today, locale, insights)

        // 5. Wellness Correlation (Daily Review vs Habit Completion)
        analyzeWellnessCorrelation(habitEntries, dailyReviews, insights)

        // 6. Focus Pattern Analysis
        analyzeFocusPattern(focusSessions, insights)

        // 7. Goal Velocity
        analyzeGoalVelocity(goals, today, insights)

        // 8. Habit Skip Reasons Analysis
        analyzeHabitSkipReasons(habits, habitEntries, today, insights)

        return insights.sortedWith(
            compareByDescending<Insight> { it.confidence.score }
                .thenBy { it.period == InsightPeriod.Today }
        )
    }

    private fun analyzeCarryRisk(tasks: List<PlanTaskEntity>, out: MutableList<Insight>) {
        val highCarryTasks = tasks.filter { it.status == "Active" && it.carryCount >= 3 }
            .sortedByDescending { it.carryCount }

        for (task in highCarryTasks.take(2)) {
            val carry = task.carryCount
            out += Insight(
                id = "carry_risk_${task.id}",
                type = InsightType.CarryRisk,
                title = "Sürekli Ertelenen Görev: \"${task.title}\"",
                description = "Bu görev $carry kez ertelendi. Görev üzerinizde gizli bir direnç veya yük yaratıyor olabilir.",
                confidence = InsightConfidence.High,
                whyWeSayThis = "\"${task.title}\" görevi ardı ardına $carry gün boyunca ertelenenler arasına kaydedildi.",
                supportingMetrics = mapOf("carryCount" to carry.toString(), "taskId" to task.id.toString()),
                recommendation = Recommendation(
                    id = "rec_split_${task.id}",
                    title = "Görevi Sadeleştir",
                    description = "Görevi daha küçük adımlara bölerek veya haftalık plana alarak baskıyı azaltabilirsin.",
                    actionType = RecommendationActionType.SplitTask,
                    actionPayload = task.id.toString(),
                    suggestedChangeText = "Bu görevi 2 alt adıma bölmek ister misin?",
                ),
                period = InsightPeriod.Today,
            )
        }
    }

    private fun analyzeTimeOfDay(tasks: List<PlanTaskEntity>, out: MutableList<Insight>) {
        val scheduledTasks = tasks.filter { (it.startMinutes != null || it.dueMinutes != null) }
        if (scheduledTasks.size < 6) return

        val eveningTasks = scheduledTasks.filter { (it.startMinutes ?: it.dueMinutes ?: 0) >= 1080 } // 18:00+
        val dayTasks = scheduledTasks.filter { (it.startMinutes ?: it.dueMinutes ?: 0) in 360 until 1080 } // 06:00 - 18:00

        if (eveningTasks.size >= 4 && dayTasks.size >= 4) {
            val eveningCompleted = eveningTasks.count { it.status == "Completed" }
            val dayCompleted = dayTasks.count { it.status == "Completed" }

            val eveningRate = (eveningCompleted.toFloat() / eveningTasks.size * 100).toInt()
            val dayRate = (dayCompleted.toFloat() / dayTasks.size * 100).toInt()

            if (dayRate - eveningRate >= 25) {
                out += Insight(
                    id = "time_of_day_evening_drop",
                    type = InsightType.TimeOfDay,
                    title = "Akşam Saatlerinde Tamamlama Düşüşü",
                    description = "18:00 sonrasına planlanan görevlerin tamamlama oranı %$eveningRate iken, günün diğer saatlerinde %$dayRate.",
                    confidence = if (eveningTasks.size >= 8) InsightConfidence.High else InsightConfidence.Medium,
                    whyWeSayThis = "Son dönemde akşam saatlerine planlanan ${eveningTasks.size} görevden $eveningCompleted tanesi tamamlandı (%$eveningRate). Gün içi planlanan ${dayTasks.size} görevden $dayCompleted tanesi tamamlandı (%$dayRate).",
                    supportingMetrics = mapOf(
                        "eveningRate" to "%$eveningRate",
                        "dayRate" to "%$dayRate",
                        "eveningTasks" to eveningTasks.size.toString(),
                    ),
                    recommendation = Recommendation(
                        id = "rec_schedule_earlier",
                        title = "Önemli Görevleri Erkene Al",
                        description = "Odak gerektiren önemli görevlerini zihninin daha zinde olduğu gündüz saatlerine planlamayı deneyebilirsin.",
                        actionType = RecommendationActionType.RescheduleTask,
                        suggestedChangeText = "Akşam görevlerini öğleden önceye kaydırmayı öneriyoruz.",
                    ),
                    period = InsightPeriod.Weekly,
                )
            }
        }
    }

    private fun analyzeTaskPacing(tasks: List<PlanTaskEntity>, out: MutableList<Insight>) {
        val completedWithEstimates = tasks.filter {
            it.status == "Completed" && (it.estimatedMinutes ?: 0) > 0 && (it.actualMinutes ?: 0) > 0
        }

        if (completedWithEstimates.size >= 4) {
            val totalEstimated = completedWithEstimates.sumOf { it.estimatedMinutes ?: 0 }
            val totalActual = completedWithEstimates.sumOf { it.actualMinutes ?: 0 }

            if (totalActual > totalEstimated) {
                val underestimationRatio = ((totalActual - totalEstimated).toFloat() / totalEstimated * 100).toInt()
                if (underestimationRatio >= 20) {
                    out += Insight(
                        id = "task_pacing_underestimation",
                        type = InsightType.TaskPacing,
                        title = "Planlama Yanılgısı (Planning Fallacy)",
                        description = "Görevler için ayırdığın tahmini süre, gerçek çalışma süresinden ortalama %$underestimationRatio daha kısa kalıyor.",
                        confidence = if (completedWithEstimates.size >= 8) InsightConfidence.High else InsightConfidence.Medium,
                        whyWeSayThis = "İncelenen ${completedWithEstimates.size} görevde toplam $totalEstimated dakika tahmin edilmişken, gerçekte $totalActual dakika harcandı.",
                        supportingMetrics = mapOf(
                            "underestimation" to "%$underestimationRatio",
                            "tasksCount" to completedWithEstimates.size.toString(),
                        ),
                        recommendation = Recommendation(
                            id = "rec_time_buffer",
                            title = "%25 Tampon Süre Ekle",
                            description = "Görev sürelerini tahmin ederken beklenmeyen durumlar için %20-%30 ek pay bırakmak günü sakin tutar.",
                            actionType = RecommendationActionType.RescheduleTask,
                            suggestedChangeText = "Görev tahminlerini %25 artırarak plan yapabilirsin.",
                        ),
                        period = InsightPeriod.Weekly,
                    )
                }
            }
        }
    }

    private fun analyzeHabitPatterns(
        habits: List<HabitEntity>,
        entries: List<HabitEntryEntity>,
        today: LocalDate,
        locale: Locale,
        out: MutableList<Insight>,
    ) {
        val activeHabits = habits.filter { !it.isArchived }
        if (activeHabits.isEmpty() || entries.size < 14) return

        val last30Days = (0 until 30).map { today.minusDays(it.toLong()) }
        val entriesByDate = entries.groupBy { it.localDate }

        val dayOfWeekCompletions = mutableMapOf<DayOfWeek, Pair<Int, Int>>() // DayOfWeek -> (completed, totalScheduled)

        for (date in last30Days) {
            val dayOfWeek = date.dayOfWeek
            val dayEntries = entriesByDate[date.toString()].orEmpty()
            val completedCount = dayEntries.count { it.status.equals("Completed", ignoreCase = true) || it.status.equals("COMPLETED", ignoreCase = true) || it.amount > 0 }
            val scheduledCount = activeHabits.count { h ->
                val bit = when (dayOfWeek) {
                    DayOfWeek.MONDAY -> 1
                    DayOfWeek.TUESDAY -> 2
                    DayOfWeek.WEDNESDAY -> 4
                    DayOfWeek.THURSDAY -> 8
                    DayOfWeek.FRIDAY -> 16
                    DayOfWeek.SATURDAY -> 32
                    DayOfWeek.SUNDAY -> 64
                }
                (h.scheduleDaysMask and bit) != 0
            }

            if (scheduledCount > 0) {
                val current = dayOfWeekCompletions.getOrDefault(dayOfWeek, Pair(0, 0))
                dayOfWeekCompletions[dayOfWeek] = Pair(current.first + completedCount, current.second + scheduledCount)
            }
        }

        val dayRates = dayOfWeekCompletions.filter { it.value.second >= 4 }.mapValues {
            (it.value.first.toFloat() / it.value.second * 100).toInt()
        }

        if (dayRates.size >= 4) {
            val bestDay = dayRates.maxByOrNull { it.value }
            val worstDay = dayRates.minByOrNull { it.value }

            if (bestDay != null && worstDay != null && (bestDay.value - worstDay.value >= 25)) {
                val bestName = bestDay.key.getDisplayName(TextStyle.FULL, locale)
                val worstName = worstDay.key.getDisplayName(TextStyle.FULL, locale)

                out += Insight(
                    id = "habit_pattern_dow",
                    type = InsightType.HabitPattern,
                    title = "Haftalık Alışkanlık Ritmi",
                    description = "Alışkanlıklarını en düzenli tamamladığın gün %${bestDay.value} ile $bestName; en çok zorlandığın gün ise %${worstDay.value} ile $worstName.",
                    confidence = InsightConfidence.High,
                    whyWeSayThis = "Son 30 günde $bestName günlerinde tamamlama oranı %${bestDay.value}, $worstName günlerinde ise %${worstDay.value} olarak gerçekleşti.",
                    supportingMetrics = mapOf(
                        "bestDay" to "$bestName (%${bestDay.value})",
                        "worstDay" to "$worstName (%${worstDay.value})",
                    ),
                    recommendation = Recommendation(
                        id = "rec_habit_dow",
                        title = "$worstName Gününü Hafiflet",
                        description = "$worstName günlerinde alışkanlık hedeflerini minimum seviyede tutarak istikrarı koruyabilirsin.",
                        actionType = RecommendationActionType.AdjustHabitTarget,
                        suggestedChangeText = "$worstName günlerinde hedefleri esnetmeyi düşünebilirsin.",
                    ),
                    period = InsightPeriod.Trend,
                )
            }
        }
    }

    private fun analyzeWellnessCorrelation(
        entries: List<HabitEntryEntity>,
        reviews: List<DailyReviewEntity>,
        out: MutableList<Insight>,
    ) {
        if (reviews.size < 5) return

        val entriesByEpoch = entries.groupBy {
            try { LocalDate.parse(it.localDate).toEpochDay() } catch (_: Exception) { -1L }
        }

        val highHabitReviews = mutableListOf<DailyReviewEntity>()
        val lowHabitReviews = mutableListOf<DailyReviewEntity>()

        for (review in reviews) {
            val completedHabits = entriesByEpoch[review.epochDay]?.count { it.status.equals("Completed", ignoreCase = true) || it.status.equals("COMPLETED", ignoreCase = true) || it.amount > 0 } ?: 0
            if (completedHabits >= 3) {
                highHabitReviews += review
            } else if (completedHabits <= 1) {
                lowHabitReviews += review
            }
        }

        if (highHabitReviews.size >= 3 && lowHabitReviews.size >= 3) {
            val avgHighEnergy = highHabitReviews.map { it.energy }.average()
            val avgLowEnergy = lowHabitReviews.map { it.energy }.average()
            val diff = avgHighEnergy - avgLowEnergy

            if (diff >= 0.7) {
                val formattedDiff = "%.1f".format(Locale.US, diff)
                out += Insight(
                    id = "wellness_habit_energy_correlation",
                    type = InsightType.WellnessCorrelation,
                    title = "Alışkanlıklar ve Enerji Düzeyi",
                    description = "Alışkanlıklarını tamamladığın günlerde gün sonu enerji ve zindelik seviyen ortalama $formattedDiff puan daha yüksek.",
                    confidence = if (reviews.size >= 10) InsightConfidence.High else InsightConfidence.Medium,
                    whyWeSayThis = "3+ alışkanlık tamamlanan günlerde ortalama enerji: ${"%.1f".format(Locale.US, avgHighEnergy)}/5. Aksayan günlerde ortalama enerji: ${"%.1f".format(Locale.US, avgLowEnergy)}/5 (${reviews.size} gün sonu değerlendirmesi analiz edildi).",
                    supportingMetrics = mapOf("energyDiff" to "+$formattedDiff puan"),
                    period = InsightPeriod.Monthly,
                )
            }
        }
    }

    private fun analyzeFocusPattern(sessions: List<FocusSessionEntity>, out: MutableList<Insight>) {
        val completed = sessions.filter { it.completed }
        if (completed.size < 4) return

        val totalMinutes = completed.sumOf { it.durationMinutes }
        val avgMinutes = totalMinutes / completed.size
        val topMode = completed.groupBy { it.mode }.maxByOrNull { it.value.size }?.key ?: "Pomodoro"

        out += Insight(
            id = "focus_depth_pattern",
            type = InsightType.FocusPattern,
            title = "Derin Çalışma Ritmi",
            description = "Odak oturumlarında en çok $topMode modunu tercih ediyorsun. Ortalama seans süren $avgMinutes dakika.",
            confidence = InsightConfidence.Medium,
            whyWeSayThis = "Toplam ${completed.size} tamamlanmış oturumda $totalMinutes dakika derin çalışma kaydedildi.",
            supportingMetrics = mapOf("totalMinutes" to "$totalMinutes dk", "avgDuration" to "$avgMinutes dk"),
            period = InsightPeriod.Trend,
        )
    }

    private fun analyzeGoalVelocity(goals: List<GoalEntity>, today: LocalDate, out: MutableList<Insight>) {
        val activeWithDates = goals.filter { it.status == "Active" && it.dueDateEpochDay != null && it.targetValue > 0 }
        for (goal in activeWithDates) {
            val due = LocalDate.ofEpochDay(goal.dueDateEpochDay!!)
            val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, due).toInt()
            if (daysRemaining in 1..30) {
                val remainingValue = goal.targetValue - goal.currentValue
                if (remainingValue > 0) {
                    val neededPerDay = remainingValue / daysRemaining
                    out += Insight(
                        id = "goal_velocity_${goal.id}",
                        type = InsightType.GoalVelocity,
                        title = "Hedef İlerlemesi: \"${goal.title}\"",
                        description = "Bitiş tarihine $daysRemaining gün kaldı. Hedefe ulaşmak için günlük tempo: %.1f %s.".format(neededPerDay, goal.unit),
                        confidence = InsightConfidence.High,
                        whyWeSayThis = "Hedeflenen: ${goal.targetValue} ${goal.unit}, Mevcut: ${goal.currentValue} ${goal.unit}. Kalan $remainingValue ${goal.unit} için $daysRemaining gün bulunuyor.",
                        supportingMetrics = mapOf("daysLeft" to daysRemaining.toString(), "neededPerDay" to "%.1f".format(neededPerDay)),
                        period = InsightPeriod.Weekly,
                    )
                }
            }
        }
    }

    private fun analyzeHabitSkipReasons(
        habits: List<HabitEntity>,
        habitEntries: List<HabitEntryEntity>,
        today: LocalDate,
        out: MutableList<Insight>,
    ) {
        val habitMap = habits.associateBy { it.id }
        val startDate = today.minusDays(30).toString()
        val skippedEntries = habitEntries.filter {
            it.status == HabitEntryEntity.STATUS_SKIPPED && it.localDate >= startDate
        }
        if (skippedEntries.isEmpty()) return

        val skipsByHabit = skippedEntries.groupBy { it.habitId }
        for ((habitId, entries) in skipsByHabit) {
            val habit = habitMap[habitId] ?: continue
            if (entries.size < 3) continue

            val reasons = entries.map { SkipReasonType.parse(it.note) }
            val dominantReasonGroup = reasons.groupBy { it.first }
                .maxByOrNull { it.value.size } ?: continue

            val dominantType = dominantReasonGroup.key
            val dominantCount = dominantReasonGroup.value.size
            val dominantRatio = (dominantCount.toFloat() / entries.size * 100).toInt()

            val why = "Son 30 günde \"${habit.name}\" alışkanlığı ${entries.size} kez atlandı. Atlama nedenlerinin %$dominantRatio'ini \"${dominantType.defaultLabel}\" ($dominantCount kez) oluşturuyor."

            val recommendation = when (dominantType) {
                SkipReasonType.NoTime -> Recommendation(
                    id = "rec_habit_notime_${habit.id}",
                    title = "Hedefi veya Zamanı Esnet",
                    description = "\"${habit.name}\" için zaman bulmakta zorlanıyorsun. Günlük hedefi küçültmek veya daha sakin bir vakte almak sürdürülebilirliği artırabilir.",
                    actionType = RecommendationActionType.AdjustHabitTarget,
                    actionPayload = habit.id.toString(),
                    suggestedChangeText = "Hedef miktarını azaltmayı veya hatırlatıcı vaktini değiştirmeyi öneriyoruz.",
                )
                SkipReasonType.Forgot -> Recommendation(
                    id = "rec_habit_forgot_${habit.id}",
                    title = "Hatırlatıcı veya Rutin Ekle",
                    description = "\"${habit.name}\" alışkanlığını sık sık unutuyorsun. Özel bir hatırlatıcı kurmak veya mevcut bir rutinin hemen ardına eklemek faydalı olabilir.",
                    actionType = RecommendationActionType.AdjustHabitTarget,
                    actionPayload = habit.id.toString(),
                    suggestedChangeText = "Belirli bir hatırlatıcı saati belirlemeyi öneriyoruz.",
                )
                SkipReasonType.Travel, SkipReasonType.Sick -> Recommendation(
                    id = "rec_habit_pause_${habit.id}",
                    title = "Küçük Adımlarla Yeniden Başla",
                    description = "Geçici aksaklıklar ve molalar doğaldır. Kendini suçlamadan, en küçük adımla ritmini yeniden yakalayabilirsin.",
                    actionType = RecommendationActionType.AdjustHabitTarget,
                    actionPayload = habit.id.toString(),
                    suggestedChangeText = "Ritmi yakalamak için ilk günlerde hedefi esnek tutabilirsin.",
                )
                else -> null
            }

            out += Insight(
                id = "habit_skip_${habit.id}",
                type = InsightType.HabitSkipReason,
                title = "\"${habit.name}\" Atlama Analizi",
                description = "Son 30 günde ${entries.size} kez atlandı. En belirgin sebep: ${dominantType.icon} ${dominantType.defaultLabel} ($dominantCount kez).",
                confidence = if (entries.size >= 5) InsightConfidence.High else InsightConfidence.Medium,
                whyWeSayThis = why,
                supportingMetrics = mapOf(
                    "skipCount" to entries.size.toString(),
                    "topReason" to dominantType.defaultLabel,
                    "topReasonCount" to dominantCount.toString(),
                ),
                recommendation = recommendation,
                period = InsightPeriod.Weekly,
            )
        }
    }
}
