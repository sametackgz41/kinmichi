package app.tsuzuku.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [Index("status"), Index("targetDateEpochDay")],
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val colorArgb: Long = 0xFF5068A9,
    val emoji: String = "📁",
    val status: String = "Active", // "Active", "Completed", "Archived"
    val targetDateEpochDay: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "project_milestones",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class ProjectMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val sortOrder: Int = 0,
    val completed: Boolean = false,
    val dueDateEpochDay: Long? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "project_task_links",
    indices = [Index("projectId"), Index("taskId"), Index(value = ["projectId", "taskId"], unique = true)],
)
data class ProjectTaskLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val taskId: Long,
)

@Entity(
    tableName = "project_goal_links",
    indices = [Index("projectId"), Index("goalId"), Index(value = ["projectId", "goalId"], unique = true)],
)
data class ProjectGoalLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val goalId: Long,
)
