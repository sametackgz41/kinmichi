package app.tsuzuku.data.intelligence

import app.tsuzuku.model.AiProviderType
import app.tsuzuku.model.AskTsuzukuMessage
import app.tsuzuku.model.CompactAnalyticsSummary
import app.tsuzuku.model.Insight
import app.tsuzuku.model.MessageSender
import app.tsuzuku.model.PlanDraftProposal
import app.tsuzuku.model.PlanMode
import app.tsuzuku.model.ProposedTaskDraft
import app.tsuzuku.model.WeeklyAiCommentary
import app.tsuzuku.model.WeeklySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

interface IntelligenceProvider {
    val type: AiProviderType

    suspend fun answerQuestion(
        question: String,
        context: CompactAnalyticsSummary,
        chatHistory: List<AskTsuzukuMessage> = emptyList(),
    ): Result<AskTsuzukuMessage>

    suspend fun generatePlanDraft(
        prompt: String,
        mode: PlanMode = PlanMode.Balanced,
        context: CompactAnalyticsSummary,
    ): Result<PlanDraftProposal>

    suspend fun generateWeeklyReviewCommentary(
        summary: WeeklySummary,
        context: CompactAnalyticsSummary,
    ): Result<WeeklyAiCommentary>

    suspend fun explainInsight(
        insight: Insight,
        context: CompactAnalyticsSummary,
    ): Result<String>
}

class RuleBasedIntelligenceProvider : IntelligenceProvider {
    override val type: AiProviderType = AiProviderType.RuleBased

    override suspend fun answerQuestion(
        question: String,
        context: CompactAnalyticsSummary,
        chatHistory: List<AskTsuzukuMessage>,
    ): Result<AskTsuzukuMessage> = withContext(Dispatchers.Default) {
        val q = question.lowercase()
        val metrics = mutableMapOf<String, String>()
        metrics["Genel Başarı"] = "%${context.overallHabitCompletionRatePercent}"
        metrics["Toplam Odak"] = "${context.totalFocusHours} saat"
        metrics["Ertelenen Görevler"] = "${context.carriedTasksCount}"

        var followUps = listOf<String>()
        val lastAssistantMsg = chatHistory.lastOrNull { it.sender == MessageSender.Assistant }?.content?.lowercase().orEmpty()

        val responseContent = when {
            // Multi-turn contextual follow-ups ("nasıl yapabilirim", "önerin ne", "çözüm", vb.)
            (q.contains("nasıl") || q.contains("öner") || q.contains("çözüm") || q.contains("adım") || q.contains("detay") || q.contains("ne yapmalı")) && lastAssistantMsg.isNotBlank() -> {
                when {
                    lastAssistantMsg.contains("aksayan") || lastAssistantMsg.contains("zorlandığın") -> {
                        metrics["Öneri"] = "Hedefi %50 Küçült"
                        followUps = listOf("Bunun için hafif bir plan hazırla", "Alışkanlığı sabah saatine taşı", "Haftalık özetimi göster")
                        "Bu alışkanlıkta sürtünmeyi en aza indirmek için **2 Dakika Kuralı**'nı uygulamayı dene. Başlangıç eşiğini o kadar küçült ki zihnin erteleme gerekçesi bulamasın. Örneğin 30 dakika yerine 'sadece 5 dakika başla' prensibiyle ritmi yeniden kazanabilirsin."
                    }
                    lastAssistantMsg.contains("odak") || lastAssistantMsg.contains("verim") -> {
                        metrics["Öneri"] = "Pomodoro Blokları"
                        followUps = listOf("Derin çalışma günü planla", "Günün hangi saatinde daha verimliyim?", "Mola sıklığı ne olmalı?")
                        "Odak verimini katlamak için **25+5 dakikalık Pomodoro döngüsü** ve dikkat dağıtıcıları fiziksel olarak uzaklaştırmak en sakin çözümdür. Günün ilk derin çalışma seansını sabah 09:00 - 11:00 arasına yerleştirmek zihinsel yorgunluğu önler."
                    }
                    lastAssistantMsg.contains("ertele") || lastAssistantMsg.contains("kronik") -> {
                        metrics["Öneri"] = "Görevi 3 Mikro Adıma Böl"
                        followUps = listOf("Ertelenenleri temizleme planı hazırla", "Görevi küçült", "Görevi iptal etmeli miyim?")
                        "Erteleme genellikle tembellik değil, görevin belirsizliğinden kaynaklanır. Görevi hemen bitirmek zorunda hissetme; ilk mikro adım olarak sadece 'taslak bir başlık at' veya 'gerekli dosyayı aç'. Harekete geçtikten sonra ivme kendiliğinden gelecektir."
                    }
                    else -> {
                        followUps = listOf("En verimli olduğum saatler?", "Hangi alışkanlığım aksıyor?", "Dengeli bir gün tasarla")
                        "Küçük, sürekli ve sakin adımlarla ilerlemek Tsuzuku'nun temel felsefesidir. Hedeflerini zorlaştırmak yerine günlük ritmini korumaya odaklan."
                    }
                }
            }

            // Efficiency & Focus
            q.contains("verim") || q.contains("saat") || q.contains("odak") || q.contains("zaman") -> {
                metrics["Odak Saati"] = "${context.totalFocusHours} saat"
                followUps = listOf("Odak süremi nasıl artırabilirim?", "Derin çalışma günü planla", "Günün hangi saatinde daha verimliyim?")
                if (context.totalFocusHours > 0) {
                    "Kayıtlarına göre son 30 günde toplam **${context.totalFocusHours} saatlik derin odaklanma** seansı tamamlamışsın. Odak seanslarının ardından alışkanlık tamamlama oranın belirgin şekilde yükseliyor. Günün ilk yarısındaki bloklarda zihinsel direncin daha düşük olduğu görülüyor."
                } else {
                    "Henüz kayıtlı bir odak seansın bulunmuyor. Odak Modu'nu kullanarak 25 dakikalık bloklar denemek, günün en verimli saatlerini keşfetmene yardımcı olabilir."
                }
            }

            // Struggling habits
            q.contains("aks") || q.contains("zorlan") || q.contains("alışkan") || q.contains("habit") || q.contains("atla") -> {
                val struggling = context.strugglingHabit
                val skipReason = context.topSkipReasons.maxByOrNull { it.value }
                followUps = listOf("Bu alışkanlığı nasıl kolaylaştırabilirim?", "Hedefi küçültmeli miyim?", "Haftalık özetimi göster")
                if (struggling != null) {
                    metrics["Aksayan Alışkanlık"] = struggling
                    if (skipReason != null) {
                        metrics["Baskın Atlama Sebebi"] = "${skipReason.key} (${skipReason.value} kez)"
                        "Son 30 günde en çok zorlandığın alışkanlık **'$struggling'**. Kaydedilen atlama sebeplerinde en sık **'${skipReason.key}'** (${skipReason.value} kez) öne çıkıyor. Bu alışkanlığın günlük hedefini biraz küçülterek sürdürülebilirliği yeniden yakalayabilirsin."
                    } else {
                        "Son 30 günde en çok aksayan alışkanlığın **'$struggling'**. Ritmi korumak için hedef miktarını geçici olarak %50 azaltmayı veya belirli bir hatırlatıcı eklemeyi düşünebilirsin."
                    }
                } else {
                    "Harika bir denge! Son 30 günde kritik düzeyde aksayan bir alışkanlığın bulunmuyor. Genel tamamlama oranın **%${context.overallHabitCompletionRatePercent}** seviyesinde."
                }
            }

            // Chronic deferral & tasks
            q.contains("ertele") || q.contains("carry") || q.contains("görev") || q.contains("task") || q.contains("devret") -> {
                followUps = listOf("Bu görevi 3 alt adıma böl", "Ertelenenleri temizleme planı hazırla", "Görevi iptal etmek doğru mu?")
                if (context.chronicDeferredTasks.isNotEmpty()) {
                    val taskNames = context.chronicDeferredTasks.joinToString(", ") { "'$it'" }
                    metrics["Kronik Ertelenenler"] = taskNames
                    "Son günlerde özellikle **$taskNames** görevlerinde erteleme eğilimi görülüyor. Toplam **${context.carriedTasksCount} kez görev devredilmiş**. Bir görevi ertelemek genellikle tembellikten değil, görevin belirsizliğinden kaynaklanır. Görevi 5 dakikalık 2-3 mikro adıma bölmeyi deneyebilirsin."
                } else if (context.carriedTasksCount > 0) {
                    "Son dönemde **${context.carriedTasksCount} görev devredilmiş** ancak kronikleşen bir erteleme kalıbı yok. Akışın genel olarak sağlıklı."
                } else {
                    "Tebrikler! Planlanan görevlerini zamanında tamamlama oranın oldukça yüksek, bekleyen veya ertelenen kronik görev bulunmuyor."
                }
            }

            // Goals
            q.contains("hedef") || q.contains("goal") -> {
                followUps = listOf("Hedefim için sprint planı oluştur", "Bu hedefe bağlı bir alışkanlık öner", "Gelişim durumum nasıl?")
                if (context.activeGoalsSummary.isNotEmpty()) {
                    val summaryList = context.activeGoalsSummary.joinToString("\n• ") {
                        "**${it.title}**: %${it.progressPercent} tamamlandı"
                    }
                    metrics["Aktif Hedefler"] = "${context.activeGoalsSummary.size} adet"
                    "Aktif hedeflerindeki ilerleme durumu:\n• $summaryList\n\nHedeflerine bağlı alışkanlık ve görevleri düzenli sürdürmek hedefe ulaşma hızını önemli ölçüde artırıyor."
                } else {
                    "Şu anda takip edilen aktif bir hedef bulunmuyor. Hedefler sekmesinden uzun vadeli bir amaç tanımlayabilirsin."
                }
            }

            // Summary / Overview
            q.contains("gelişim") || q.contains("nasıl") || q.contains("durum") || q.contains("özet") || q.contains("genel") || q.contains("ilerleme") -> {
                val best = context.bestStreakHabit ?: "—"
                metrics["En İyi Seri"] = best
                followUps = listOf("En verimli olduğum saatler?", "Hangi alışkanlığım aksıyor?", "Dengeli bir gün tasarla")
                "Son 30 günlük Tsuzuku analizi:\n• Genel alışkanlık tamamlama oranın: **%${context.overallHabitCompletionRatePercent}**\n• En istikrarlı alışkanlığın: **$best**\n• Tamamlanan odaklanma süresi: **${context.totalFocusHours} saat**\n• Aktif görev erteleme sayısı: **${context.carriedTasksCount}**\n\nRitmini sakin bir kararlılıkla sürdürüyorsun."
            }

            else -> {
                followUps = listOf("En verimli olduğum saatler?", "Hangi alışkanlığım aksıyor?", "Ertelenen görevlerimde durum ne?")
                "Tsuzuku analitiğine göre son 30 günde **%${context.overallHabitCompletionRatePercent}** genel tamamlama oranına ve **${context.totalFocusHours} saatlik** odaklanma süresine sahipsin. 'En verimli saatlerim', 'Hangi alışkanlığım aksıyor?' veya 'Ertelenen görevlerim' gibi sorular sorabilirsin."
            }
        }

        Result.success(
            AskTsuzukuMessage(
                sender = MessageSender.Assistant,
                content = responseContent,
                supportingMetrics = metrics,
                followUpQuestions = followUps,
            )
        )
    }

    override suspend fun generatePlanDraft(
        prompt: String,
        mode: PlanMode,
        context: CompactAnalyticsSummary,
    ): Result<PlanDraftProposal> = withContext(Dispatchers.Default) {
        val todayEpoch = LocalDate.now().toEpochDay()
        val isTomorrow = prompt.lowercase().contains("yarın") || prompt.lowercase().contains("tomorrow")
        val targetEpoch = if (isTomorrow) todayEpoch + 1 else todayEpoch
        val proposedTasks = mutableListOf<ProposedTaskDraft>()

        val title: String
        val rationale: String

        when (mode) {
            PlanMode.Balanced -> {
                title = "Dengeli Günlük Plan"
                rationale = "Alışkanlıklar, odaklanma ve dinlenme arasında dengeli bir akış oluşturuldu."

                if (context.chronicDeferredTasks.isNotEmpty()) {
                    val deferred = context.chronicDeferredTasks.first()
                    proposedTasks.add(
                        ProposedTaskDraft(
                            title = "$deferred (Öncelikli Adım)",
                            estimatedMinutes = 25,
                            scheduledEpochDay = targetEpoch,
                            startMinutes = 570, // 09:30
                            dueMinutes = 600,
                            subtasks = listOf("Hazırlık ve ilk taslak", "Gözden geçirme"),
                            isSelected = true,
                        )
                    )
                }

                proposedTasks.add(
                    ProposedTaskDraft(
                        title = "Derin Odaklanma Seansı (Önemli Çalışma)",
                        estimatedMinutes = 45,
                        scheduledEpochDay = targetEpoch,
                        startMinutes = 630, // 10:30
                        dueMinutes = 690,
                        subtasks = listOf("Bildirimleri kapat", "Tek bir konuya odaklan"),
                        isSelected = true,
                    )
                )

                proposedTasks.add(
                    ProposedTaskDraft(
                        title = "Gün Sonu Değerlendirmesi ve Notlar",
                        estimatedMinutes = 15,
                        scheduledEpochDay = targetEpoch,
                        startMinutes = 1140, // 19:00
                        dueMinutes = 1170,
                        subtasks = listOf("Günün mod ve enerjisini kaydet", "Yarının önceliğini belirle"),
                        isSelected = true,
                    )
                )
            }

            PlanMode.DeepWork -> {
                title = "Derin Odaklanma Günü (Deep Work)"
                rationale = "Yüksek zihinsel enerji gerektiren zorlu çalışmalar için 2 derin odak bloğu ve sakin bir kapanış kurgulandı."

                proposedTasks.add(
                    ProposedTaskDraft(
                        title = "🚀 1. Derin Odak Bloğu (Kritik Çıktı)",
                        estimatedMinutes = 60,
                        scheduledEpochDay = targetEpoch,
                        startMinutes = 570, // 09:30
                        dueMinutes = 630,
                        subtasks = listOf("Tüm bildirimleri sessize al", "En zorlu bölüme başla", "İlk somut çıktıyı üret"),
                        isSelected = true,
                    )
                )

                proposedTasks.add(
                    ProposedTaskDraft(
                        title = "🎯 2. Derin Odak Bloğu (Geliştirme & Test)",
                        estimatedMinutes = 60,
                        scheduledEpochDay = targetEpoch,
                        startMinutes = 840, // 14:00
                        dueMinutes = 900,
                        subtasks = listOf("Blok hedefini gözden geçir", "Uygulama ve doğrulama", "Kazanımları not al"),
                        isSelected = true,
                    )
                )

                proposedTasks.add(
                    ProposedTaskDraft(
                        title = "🌿 Zihinsel Boşaltma ve Kapanış",
                        estimatedMinutes = 15,
                        scheduledEpochDay = targetEpoch,
                        startMinutes = 1050, // 17:30
                        dueMinutes = 1080,
                        subtasks = listOf("Ekranları kapat", "Kısa bir yürüyüş / esneme"),
                        isSelected = true,
                    )
                )
            }

            PlanMode.LowEnergy -> {
                title = "Düşük Enerji ve Toparlanma Planı"
                rationale = "Tükenmiş hissettiğin anlar için zihinsel direnç oluşturmayan 15-20 dakikalık hafif mikro adımlar seçildi."

                proposedTasks.add(
                    ProposedTaskDraft(
                        title = "🌿 10 Dakikalık Hafif Başlangıç",
                        estimatedMinutes = 15,
                        scheduledEpochDay = targetEpoch,
                        startMinutes = 600, // 10:00
                        dueMinutes = 620,
                        subtasks = listOf("Sadece çalışma alanını hazırla", "1 küçük detayı tamamla"),
                        isSelected = true,
                    )
                )

                proposedTasks.add(
                    ProposedTaskDraft(
                        title = "☕ Dinlenme ve Nefes Molası",
                        estimatedMinutes = 15,
                        scheduledEpochDay = targetEpoch,
                        startMinutes = 840, // 14:00
                        dueMinutes = 860,
                        subtasks = listOf("Su iç ve gözlerini dinlendir", "Kısa bir mola ver"),
                        isSelected = true,
                    )
                )

                proposedTasks.add(
                    ProposedTaskDraft(
                        title = "📝 Günlük Sakin Kapanış",
                        estimatedMinutes = 15,
                        scheduledEpochDay = targetEpoch,
                        startMinutes = 990, // 16:30
                        dueMinutes = 1010,
                        subtasks = listOf("Gereksiz görevleri yarına ertele", "Bugün için kendine teşekkür et"),
                        isSelected = true,
                    )
                )
            }

            PlanMode.ClearDeferrals -> {
                title = "Ertelenenleri Temizleme Planı"
                rationale = "Sürekli sarkan işlerin yarattığı zihinsel ağırlığı hafifletmek için mikro adımlara bölme odaklı kurgulandı."

                if (context.chronicDeferredTasks.isNotEmpty()) {
                    context.chronicDeferredTasks.take(2).forEachIndexed { idx, taskName ->
                        proposedTasks.add(
                            ProposedTaskDraft(
                                title = "$taskName (Mikro Adım)",
                                estimatedMinutes = 25,
                                scheduledEpochDay = targetEpoch,
                                startMinutes = 570 + (idx * 60),
                                dueMinutes = 600 + (idx * 60),
                                subtasks = listOf("İlk taslağı oluştur", "Engelleri ve soruları çıkar", "Kısa bir son kontrol yap"),
                                isSelected = true,
                            )
                        )
                    }
                } else {
                    proposedTasks.add(
                        ProposedTaskDraft(
                            title = "Eski Notları ve Gelen Kutusunu Temizle",
                            estimatedMinutes = 20,
                            scheduledEpochDay = targetEpoch,
                            startMinutes = 600,
                            dueMinutes = 630,
                            subtasks = listOf("Inbox öğelerini gözden geçir", "Gereksizleri sil"),
                            isSelected = true,
                        )
                    )
                }

                proposedTasks.add(
                    ProposedTaskDraft(
                        title = "🧹 Kalan Görevleri Sadeleştirme",
                        estimatedMinutes = 20,
                        scheduledEpochDay = targetEpoch,
                        startMinutes = 840,
                        dueMinutes = 870,
                        subtasks = listOf("Artık gerekmeyen 1 görevi iptal et", "Öncelikleri belirle"),
                        isSelected = true,
                    )
                )
            }

            PlanMode.GoalSprint -> {
                title = "Hedef Odaklı Sprint Planı"
                rationale = "Günün enerjisi doğrudan uzun vadeli hedeflerine ivme kazandıracak somut ilerlemelere ayrıldı."

                val primaryGoal = context.activeGoalsSummary.firstOrNull()
                if (primaryGoal != null) {
                    proposedTasks.add(
                        ProposedTaskDraft(
                            title = "🎯 ${primaryGoal.title} (Kritik Atılım)",
                            estimatedMinutes = 45,
                            scheduledEpochDay = targetEpoch,
                            startMinutes = 600,
                            dueMinutes = 660,
                            subtasks = listOf("Bu hafta için kritik kilometre taşını aç", "Ana taslağı veya uygulamayı yap", "İlerlemeyi kaydet"),
                            isSelected = true,
                        )
                    )
                } else {
                    proposedTasks.add(
                        ProposedTaskDraft(
                            title = "🎯 Çeyrek Hedeflerini Belirleme Seansı",
                            estimatedMinutes = 30,
                            scheduledEpochDay = targetEpoch,
                            startMinutes = 600,
                            dueMinutes = 640,
                            subtasks = listOf("Uzun vadeli 2 ana öncelik belirle", "Hedefler sekmesine ekle"),
                            isSelected = true,
                        )
                    )
                }

                proposedTasks.add(
                    ProposedTaskDraft(
                        title = "📈 İlerleme ve Veri Güncellemesi",
                        estimatedMinutes = 15,
                        scheduledEpochDay = targetEpoch,
                        startMinutes = 1020,
                        dueMinutes = 1040,
                        subtasks = listOf("Hedefe bağlı alışkanlığı tamamla", "Metrikleri güncelle"),
                        isSelected = true,
                    )
                )
            }
        }

        Result.success(
            PlanDraftProposal(
                title = title,
                rationale = rationale,
                proposedTasks = proposedTasks,
                mode = mode,
            )
        )
    }

    override suspend fun generateWeeklyReviewCommentary(
        summary: WeeklySummary,
        context: CompactAnalyticsSummary,
    ): Result<WeeklyAiCommentary> = withContext(Dispatchers.Default) {
        val ratePercent = (summary.habitCompletionRate * 100).toInt()
        val bestHabitName = summary.bestHabit?.habit?.name ?: context.bestStreakHabit ?: "Genel Alışkanlıklar"
        val focusHours = summary.focusDurationMinutes / 60

        val (title, summaryText, suggestionText) = if (summary.habitCompletionRate >= 0.70f) {
            Triple(
                "🌱 Güçlü ve Tutarlı Bir Hafta",
                "Bu hafta %$ratePercent alışkanlık tamamlama oranıyla harika bir denge yakaladın. Özellikle '$bestHabitName' alışkanlığındaki istikrarın haftanın ritmini belirledi.",
                if (summary.carriedTasksCount > 2) {
                    "Planlanan görevlerde ${summary.carriedTasksCount} devir gerçekleşmiş. Gelecek hafta görevleri daha küçük parçalar halinde planlamak ertelemeleri azaltabilir."
                } else {
                    "Ritminde sakin ve güven veren bir süreklilik var. Bu tempoyu zorlamadan korumak en sürdürülebilir yoldur."
                }
            )
        } else {
            val strugglingName = summary.strugglingHabit?.habit?.name
            Triple(
                "🌿 Sakin ve Yeniden Başlangıç",
                "Bu hafta %$ratePercent tamamlama oranı gerçekleşti. Dalgalanmalar sürecin doğal bir parçasıdır; Kaizen felsefesinde önemli olan kusursuzluk değil, ritmi yeniden yakalayabilmektir.",
                if (strugglingName != null) {
                    "'$strugglingName' alışkanlığında zorlandığın görülüyor. Gelecek hafta günlük hedefi %50 küçülterek başlamayı deneyebilirsin."
                } else {
                    "Gelecek hafta kendine sadece 1 ana öncelik belirleyip diğer beklentileri hafifletmek iyi bir nefes aldıracaktır."
                }
            )
        }

        val focusHighlight = if (focusHours > 0) {
            "Hafta boyunca $focusHours saatlik derin odaklanma seansı tamamlandı."
        } else {
            "Hafta boyunca derin odak seansı kaydedilmedi; 25 dakikalık kısa bloklar denemek zihinsel berraklık sağlayabilir."
        }

        Result.success(
            WeeklyAiCommentary(
                title = title,
                summary = summaryText,
                suggestion = suggestionText,
                focusHighlight = focusHighlight,
            )
        )
    }

    override suspend fun explainInsight(
        insight: Insight,
        context: CompactAnalyticsSummary,
    ): Result<String> = withContext(Dispatchers.Default) {
        val explanation = buildString {
            append("Bu içgörü, son 30 günde kaydettiğin verilerin sakin bir analiziyle oluşturuldu. ")
            append("Tsuzuku'nun amacı seni yargılamak veya mükemmeliyetçilik baskısı kurmak değil; ")
            appendLine("farkında olmadan oluşan kalıpları şeffafça görmeni sağlamaktır.")
            appendLine()
            append("Tespit: ").appendLine(insight.description)
            append("Dayanak: ").appendLine(insight.whyWeSayThis)
            appendLine()
            append("Küçük bir adım: Bu durumu düzeltmek için büyük kararlar almak yerine, yarın için sadece 5 dakikalık bir mikro düzenleme yapmayı deneyebilirsin.")
        }
        Result.success(explanation)
    }
}

class GeminiCloudIntelligenceProvider(
    private val apiKeyProvider: () -> String,
    private val modelProvider: () -> String = { "gemini-1.5-flash" },
    private val fallbackProvider: IntelligenceProvider = RuleBasedIntelligenceProvider(),
) : IntelligenceProvider {
    override val type: AiProviderType = AiProviderType.GeminiCloud

    override suspend fun answerQuestion(
        question: String,
        context: CompactAnalyticsSummary,
        chatHistory: List<AskTsuzukuMessage>,
    ): Result<AskTsuzukuMessage> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank()) {
            return@withContext fallbackProvider.answerQuestion(question, context, chatHistory)
        }

        try {
            val promptText = buildString {
                append("Sen Tsuzuku adında sakin, sade ve yargılamayan bir alışkanlık asistanısın. ")
                append("Kullanıcının 30 günlük verilerine göre sorusunu nazikçe, sade ve şeffaf bir dille yanıtla. ")
                appendLine("Veride olmayan sayıları uydurma.")
                appendLine()
                appendLine("Kullanıcı Verileri:")
                append("- Alışkanlık Tamamlama Oranı: %").appendLine(context.overallHabitCompletionRatePercent)
                append("- En Başarılı Seri: ").appendLine(context.bestStreakHabit ?: "Yok")
                append("- Zorlanan Alışkanlık: ").appendLine(context.strugglingHabit ?: "Yok")
                append("- Toplam Odak Saati: ").appendLine(context.totalFocusHours)
                append("- Ertelenen Görev Sayısı: ").appendLine(context.carriedTasksCount)
                if (context.chronicDeferredTasks.isNotEmpty()) {
                    append("- Kronik Ertelenenler: ").appendLine(context.chronicDeferredTasks.joinToString(", "))
                }
                appendLine()
                append("Kullanıcının Sorusu: ").append(question)
            }

            val response = callGemini(apiKey, modelProvider(), promptText)
            if (response != null) {
                Result.success(
                    AskTsuzukuMessage(
                        sender = MessageSender.Assistant,
                        content = response,
                        supportingMetrics = mapOf(
                            "Sağlayıcı" to "Gemini (${modelProvider()})",
                            "Genel Oran" to "%${context.overallHabitCompletionRatePercent}",
                            "Odak" to "${context.totalFocusHours}s",
                        ),
                        followUpQuestions = listOf(
                            "Bunun için nasıl bir plan önerirsin?",
                            "Aksayan alışkanlığımı nasıl toparlarım?",
                            "Haftalık özetimi göster",
                        ),
                    )
                )
            } else {
                fallbackProvider.answerQuestion(question, context, chatHistory)
            }
        } catch (e: Exception) {
            fallbackProvider.answerQuestion(question, context, chatHistory)
        }
    }

    override suspend fun generatePlanDraft(
        prompt: String,
        mode: PlanMode,
        context: CompactAnalyticsSummary,
    ): Result<PlanDraftProposal> {
        return fallbackProvider.generatePlanDraft(prompt, mode, context)
    }

    override suspend fun generateWeeklyReviewCommentary(
        summary: WeeklySummary,
        context: CompactAnalyticsSummary,
    ): Result<WeeklyAiCommentary> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank()) {
            return@withContext fallbackProvider.generateWeeklyReviewCommentary(summary, context)
        }

        try {
            val promptText = "Sen bilge ve sakin Tsuzuku rehberisin. Kullanıcının bu haftaki %${(summary.habitCompletionRate * 100).toInt()} tamamlama oranını, ${summary.focusDurationMinutes / 60} saatlik odağını ve ${summary.carriedTasksCount} ertelenen görevini sakin, yapıcı ve şefkatli bir dille 2-3 cümleyle değerlendir."
            val response = callGemini(apiKey, modelProvider(), promptText)
            if (response != null) {
                Result.success(
                    WeeklyAiCommentary(
                        title = "🌱 Tsuzuku AI Koç Değerlendirmesi",
                        summary = response,
                        suggestion = "Gelecek hafta için sadece 1 ana odağa odaklanmak ritmini güçlendirecektir.",
                        focusHighlight = "${summary.focusDurationMinutes / 60} saat odaklanma seansı kaydedildi.",
                    )
                )
            } else {
                fallbackProvider.generateWeeklyReviewCommentary(summary, context)
            }
        } catch (e: Exception) {
            fallbackProvider.generateWeeklyReviewCommentary(summary, context)
        }
    }

    override suspend fun explainInsight(
        insight: Insight,
        context: CompactAnalyticsSummary,
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank()) {
            return@withContext fallbackProvider.explainInsight(insight, context)
        }

        try {
            val promptText = "Sen sakin ve sade Tsuzuku asistanısın. Şu içgörüyü kullanıcıya şeffaf, motive edici ve sakin bir dille açıkla: Başlık: ${insight.title}. Açıklama: ${insight.description}. Neden: ${insight.whyWeSayThis}."
            val response = callGemini(apiKey, modelProvider(), promptText)
            if (response != null) Result.success(response) else fallbackProvider.explainInsight(insight, context)
        } catch (e: Exception) {
            fallbackProvider.explainInsight(insight, context)
        }
    }

    private fun callGemini(apiKey: String, model: String, textPrompt: String): String? {
        val selectedModel = model.trim().ifBlank { "gemini-1.5-flash" }
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 8000
        conn.readTimeout = 12000

        val requestJson = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", textPrompt)
                        })
                    })
                })
            }
            put("contents", contents)
        }

        OutputStreamWriter(conn.outputStream).use { it.write(requestJson.toString()) }

        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) return null

        val responseBody = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null
        return parts.getJSONObject(0).optString("text").trim()
    }
}
