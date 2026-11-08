package app.tsuzuku.model

import java.time.LocalDate

enum class HabitType {
    Check,
    Count,
    Duration,
}

enum class HabitDirection {
    Build,
    Quit,
}

enum class GoalPeriod {
    Daily,
    Weekly,
    Monthly,
}

enum class HealthMetric {
    None,
    Steps,
    Distance,
    Calories,
    ExerciseDuration,
    SleepDuration,
}

enum class HabitDayStatus {
    Pending,
    Completed,
    Failed,
    Skipped,
}

data class Habit(
    val id: Long,
    val name: String,
    val emoji: String,
    val colorArgb: Long,
    val type: HabitType,
    val direction: HabitDirection,
    val target: Double,
    val unit: String,
    val step: Double,
    val scheduleDaysMask: Int,
    val goalPeriod: GoalPeriod = GoalPeriod.Daily,
    val periodTarget: Int = 1,
    val routineName: String = "",
    val prerequisiteHabitId: Long? = null,
    val healthMetric: HealthMetric = HealthMetric.None,
    val sortOrder: Int,
    val reminderMinutes: Int? = null,
    val reminderRepeatMinutes: Int? = null,
    val generalNote: String = "",
    val createdOn: LocalDate = LocalDate.MIN,
    val minimumTarget: Double = 0.0,
    val maximumTarget: Double? = null,
    val difficulty: Int = 2,
    val contextLabel: String = "",
    val intention: String = "",
    val activeStartEpochDay: Long? = null,
    val activeEndEpochDay: Long? = null,
    val repeatIntervalDays: Int = 1,
    val pausedUntilEpochDay: Long? = null,
    val tags: String = "",
    val currentLevel: Int = 1,
    /** What still counts on a bad day, e.g. "1 page"; keeps the streak alive. */
    val tinyVersion: String = "",
)

data class HabitLevel(
    val id: Long = 0,
    val habitId: Long,
    val levelNumber: Int,
    val name: String,
    val target: Double,
    val minimumTarget: Double = 0.0,
    val maximumTarget: Double? = null,
    val rewardNote: String = "",
    val createdAt: Long = 0L,
)

data class TodayHabit(
    val habit: Habit,
    val progress: Double,
    val status: HabitDayStatus = HabitDayStatus.Pending,
    val streakDays: Int = 0,
    val restUsedThisWeek: Boolean = false,
    val periodProgress: Int = 0,
    val isBlocked: Boolean = false,
    val blockedByName: String? = null,
    val skipReason: String? = null,
    val hasLevels: Boolean = false,
    val currentLevelNumber: Int = 1,
    val currentLevelName: String? = null,
) {
    val isMinimumComplete: Boolean
        get() = habit.minimumTarget > 0.0 && progress >= habit.minimumTarget
    val isComplete: Boolean
        get() = if (habit.direction == HabitDirection.Quit) {
            status == HabitDayStatus.Completed || (habit.maximumTarget != null && progress <= habit.maximumTarget && status != HabitDayStatus.Failed)
        } else {
            status == HabitDayStatus.Completed || progress >= habit.target ||
                (habit.goalPeriod != GoalPeriod.Daily && periodProgress >= habit.periodTarget)
        }

    val progressFraction: Float
        get() = if (habit.target <= 0.0) 0f else (progress / habit.target).coerceIn(0.0, 1.0).toFloat()

    val masteryStage: HabitMasteryStage
        get() = HabitMasteryStage.fromDays(streakDays)

    val badHabitLimitExceeded: Boolean
        get() = habit.direction == HabitDirection.Quit && habit.maximumTarget != null && progress > habit.maximumTarget

    val badHabitLimitWarning: Boolean
        get() = habit.direction == HabitDirection.Quit && habit.maximumTarget != null && progress >= (habit.maximumTarget * 0.8) && !badHabitLimitExceeded

    val isIdealRange: Boolean
        get() = if (habit.direction == HabitDirection.Quit) {
            val max = habit.maximumTarget ?: habit.target
            progress <= max && (habit.minimumTarget <= 0.0 || progress >= habit.minimumTarget)
        } else {
            progress >= habit.target
        }
}


enum class HabitMasteryStage(val icon: String, val minDays: Int, val maxDays: Int) {
    Seed("🌱", 1, 7),
    Sprout("🌿", 8, 30),
    Habit("🌳", 31, 90),
    Mastery("🗻", 91, Int.MAX_VALUE);

    companion object {
        fun fromDays(days: Int): HabitMasteryStage = when {
            days <= 7 -> Seed
            days <= 30 -> Sprout
            days <= 90 -> Habit
            else -> Mastery
        }
    }
}

data class HabitDraft(
    val name: String,
    val emoji: String,
    val colorArgb: Long,
    val type: HabitType,
    val direction: HabitDirection,
    val target: Double,
    val unit: String,
    val step: Double,
    val scheduleDaysMask: Int,
    val goalPeriod: GoalPeriod = GoalPeriod.Daily,
    val periodTarget: Int = 1,
    val routineName: String = "",
    val prerequisiteHabitId: Long? = null,
    val healthMetric: HealthMetric = HealthMetric.None,
    val reminderMinutes: Int? = null,
    val reminderRepeatMinutes: Int? = null,
    val generalNote: String = "",
    val minimumTarget: Double = 0.0,
    val maximumTarget: Double? = null,
    val difficulty: Int = 2,
    val contextLabel: String = "",
    val intention: String = "",
    val activeStartEpochDay: Long? = null,
    val activeEndEpochDay: Long? = null,
    val repeatIntervalDays: Int = 1,
    val pausedUntilEpochDay: Long? = null,
    val tags: String = "",
    val currentLevel: Int = 1,
    /** What still counts on a bad day, e.g. "1 page"; keeps the streak alive. */
    val tinyVersion: String = "",
    /** Ring like an alarm clock instead of a quiet notification (kept per device). */
    val alarm: Boolean = false,
    /** Name of an app.tsuzuku.notifications.AlarmMission. */
    val alarmMission: String = "None",
    val alarmMarkDone: Boolean = false,
)

data class WeeklyReview(
    val weekStart: LocalDate,
    val note: String,
    val updatedAt: Long,
)
