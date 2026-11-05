package app.tsuzuku.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_sessions",
    indices = [Index("taskId"), Index("habitId"), Index("goalId"), Index("startedAt")],
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val habitId: Long? = null,
    val goalId: Long? = null,
    val mode: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationMinutes: Int = 0,
    val completed: Boolean = false,
    val interrupted: Boolean = false,
    val createdAt: Long,
)

@Entity(tableName = "inbox_items", indices = [Index("status"), Index("createdAt")])
data class InboxItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val status: String = "Open",
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "habit_goal_links",
    indices = [Index("habitId"), Index("goalId"), Index(value = ["habitId", "goalId"], unique = true)],
)
data class HabitGoalLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val goalId: Long,
    val contributionMode: String = "HabitAmount",
    val contributionValue: Double = 1.0,
)

@Entity(
    tableName = "task_goal_links",
    indices = [Index("taskId"), Index("goalId"), Index(value = ["taskId", "goalId"], unique = true)],
)
data class TaskGoalLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val goalId: Long,
    val contributionValue: Double = 0.0,
)

@Entity(
    tableName = "habit_library_links",
    indices = [Index("habitId"), Index("contentId"), Index(value = ["habitId", "contentId"], unique = true)],
)
data class HabitLibraryLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val contentId: Long,
    val contributionMultiplier: Double = 1.0,
)
