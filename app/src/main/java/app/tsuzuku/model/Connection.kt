package app.tsuzuku.model

enum class FocusMode { Pomodoro, Stopwatch, Custom }
enum class FocusTargetType { Task, Habit, Goal, Free }
enum class InboxStatus { Open, Converted, Dismissed }
enum class HabitGoalContributionMode { FixedValue, HabitAmount, FocusDuration, Manual }

data class FocusSession(
    val id: Long,
    val taskId: Long? = null,
    val habitId: Long? = null,
    val goalId: Long? = null,
    val mode: FocusMode,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationMinutes: Int = 0,
    val completed: Boolean = false,
    val interrupted: Boolean = false,
) {
    val targetType: FocusTargetType
        get() = when {
            taskId != null -> FocusTargetType.Task
            habitId != null -> FocusTargetType.Habit
            goalId != null -> FocusTargetType.Goal
            else -> FocusTargetType.Free
        }
}

data class InboxItem(val id: Long, val text: String, val status: InboxStatus, val createdAt: Long)

data class HabitGoalLink(
    val habitId: Long,
    val goalId: Long,
    val contributionMode: HabitGoalContributionMode,
    val contributionValue: Double,
)

data class TaskGoalLink(val taskId: Long, val goalId: Long, val contributionValue: Double)
data class HabitLibraryLink(val habitId: Long, val contentId: Long, val contributionMultiplier: Double)
