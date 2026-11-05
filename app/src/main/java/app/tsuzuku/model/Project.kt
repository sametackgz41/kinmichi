package app.tsuzuku.model

import app.tsuzuku.data.local.ProjectEntity
import app.tsuzuku.data.local.ProjectMilestoneEntity
import java.time.LocalDate

enum class ProjectStatus {
    Active,
    Completed,
    Archived;

    companion object {
        fun fromDb(value: String): ProjectStatus = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Active
    }
}

data class Project(
    val id: Long,
    val title: String,
    val description: String,
    val colorArgb: Long,
    val emoji: String,
    val status: ProjectStatus,
    val targetDate: LocalDate?,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val milestonesTotal: Int = 0,
    val milestonesCompleted: Int = 0,
    val tasksTotal: Int = 0,
    val tasksCompleted: Int = 0,
) {
    val isCompleted: Boolean get() = status == ProjectStatus.Completed

    val progressFraction: Float
        get() {
            if (milestonesTotal > 0) {
                return (milestonesCompleted.toFloat() / milestonesTotal).coerceIn(0f, 1f)
            }
            if (tasksTotal > 0) {
                return (tasksCompleted.toFloat() / tasksTotal).coerceIn(0f, 1f)
            }
            return if (isCompleted) 1f else 0f
        }
}

data class ProjectMilestone(
    val id: Long,
    val projectId: Long,
    val title: String,
    val sortOrder: Int,
    val completed: Boolean,
    val dueDate: LocalDate?,
)

data class ProjectDraft(
    val title: String,
    val description: String = "",
    val colorArgb: Long = 0xFF5068A9,
    val emoji: String = "📁",
    val targetDate: LocalDate? = null,
)

data class ProjectDetail(
    val project: Project,
    val milestones: List<ProjectMilestone>,
    val linkedTasks: List<PlanTask>,
    val linkedGoals: List<Goal>,
) {
    val totalItemsCount: Int get() = milestones.size + linkedTasks.size
    val completedItemsCount: Int get() = milestones.count { it.completed } + linkedTasks.count { it.completed }

    val progressFraction: Float
        get() {
            val total = totalItemsCount
            return if (total == 0) {
                if (project.isCompleted) 1f else 0f
            } else {
                (completedItemsCount.toFloat() / total).coerceIn(0f, 1f)
            }
        }
}

fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    title = title,
    description = description,
    colorArgb = colorArgb,
    emoji = emoji,
    status = ProjectStatus.fromDb(status),
    targetDate = targetDateEpochDay?.let(LocalDate::ofEpochDay),
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ProjectMilestoneEntity.toDomain(): ProjectMilestone = ProjectMilestone(
    id = id,
    projectId = projectId,
    title = title,
    sortOrder = sortOrder,
    completed = completed,
    dueDate = dueDateEpochDay?.let(LocalDate::ofEpochDay),
)
