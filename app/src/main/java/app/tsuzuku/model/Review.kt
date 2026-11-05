package app.tsuzuku.model

import java.time.LocalDate
import java.time.YearMonth

enum class ReviewPeriodType {
    Monthly,
    Yearly,
}

data class DailyReview(
    val epochDay: Long,
    val mood: Int, // 1=Zor, 2=Yorgun, 3=Nötr, 4=İyi, 5=Harika
    val energy: Int, // 1 to 5
    val productivity: Int, // 1 to 5
    val stress: Int, // 1 to 5
    val note: String,
    val updatedAt: Long,
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
}

data class PeriodReview(
    val periodType: ReviewPeriodType,
    val periodKey: String,
    val note: String,
    val updatedAt: Long,
)

data class WeeklySummary(
    val weekStart: LocalDate,
    val habitCompletionRate: Float,
    val prevHabitCompletionRate: Float?,
    val bestHabit: TodayHabit?,
    val strugglingHabit: TodayHabit?,
    val plannedTasksCount: Int,
    val completedTasksCount: Int,
    val carriedTasksCount: Int,
    val focusDurationMinutes: Int,
    val goalProgressCount: Int,
    val note: String = "",
) {
    val changeRatePercentage: Int?
        get() = prevHabitCompletionRate?.let { prev ->
            ((habitCompletionRate - prev) * 100).toInt()
        }
}

data class MonthlySummary(
    val yearMonth: YearMonth,
    val habitCompletionRate: Float,
    val focusDurationMinutes: Int,
    val completedTasksCount: Int,
    val topHabits: List<HabitPeriodStat>,
    val note: String = "",
)

data class YearlySummary(
    val year: Int,
    val totalHabitCompletions: Int,
    val totalFocusMinutes: Int,
    val completedGoalsCount: Int,
    val completedLibraryItemsCount: Int,
    val bestStreakDays: Int,
    val note: String = "",
)

data class PersonalMilestone(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val currentValue: Int,
    val targetValue: Int,
    val isUnlocked: Boolean,
) {
    val progressFraction: Float
        get() = if (targetValue <= 0) 0f else (currentValue.toFloat() / targetValue).coerceIn(0f, 1f)
}

data class PersonalRecords(
    val longestHabitStreak: Int,
    val longestHabitName: String,
    val mostProductiveDayName: String,
    val mostProductiveDayCount: Int,
    val totalFocusHours: Int,
    val totalHabitCompletions: Int,
    val totalGoalsCompleted: Int,
    val totalLibraryCompleted: Int,
    val milestones: List<PersonalMilestone>,
)

data class ActivityHeatmapDay(
    val date: LocalDate,
    val intensity: Int, // 0 to 4
    val habitsCompleted: Int,
    val tasksCompleted: Int,
    val focusMinutes: Int,
    val dailyReviewDone: Boolean,
) {
    val totalActivityUnits: Int
        get() = (habitsCompleted * 2) + tasksCompleted + (focusMinutes / 25) + (if (dailyReviewDone) 1 else 0)
}

enum class SkipReasonType(val defaultLabel: String, val icon: String) {
    Forgot("Unuttum", "🧠"),
    NoTime("Zamanım yoktu", "⏳"),
    Travel("Seyahatteydim", "✈️"),
    Sick("Hastaydım", "🤒"),
    Intentional("Bilerek atladım", "🎯"),
    Other("Diğer", "📝");

    companion object {
        fun parse(note: String?): Pair<SkipReasonType, String> {
            if (note.isNullOrBlank()) return Pair(Other, "")
            if (note.equals("VACATION", ignoreCase = true)) return Pair(Travel, "Tatil")
            val clean = if (note.startsWith("SKIP_REASON:")) note.removePrefix("SKIP_REASON:") else note
            val match = entries.firstOrNull {
                it.name.equals(clean, ignoreCase = true) || it.defaultLabel.equals(clean, ignoreCase = true)
            }
            return if (match != null) {
                Pair(match, match.defaultLabel)
            } else {
                Pair(Other, clean)
            }
        }

        fun format(type: SkipReasonType, customText: String = ""): String {
            val text = customText.trim()
            return if (text.isNotBlank() && type == Other) {
                "SKIP_REASON:$text"
            } else {
                "SKIP_REASON:${type.defaultLabel}"
            }
        }
    }
}
