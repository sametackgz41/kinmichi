package app.tsuzuku.data

import app.tsuzuku.data.local.GoalDao
import app.tsuzuku.data.local.GoalEntity
import app.tsuzuku.data.local.PlanDao
import app.tsuzuku.data.local.ProjectDao
import app.tsuzuku.data.local.ProjectEntity
import app.tsuzuku.data.local.ProjectGoalLinkEntity
import app.tsuzuku.data.local.ProjectMilestoneEntity
import app.tsuzuku.data.local.ProjectTaskLinkEntity
import app.tsuzuku.model.Goal
import app.tsuzuku.model.PlanTask
import app.tsuzuku.model.Project
import app.tsuzuku.model.ProjectDetail
import app.tsuzuku.model.ProjectDraft
import app.tsuzuku.model.ProjectMilestone
import app.tsuzuku.model.ProjectStatus
import app.tsuzuku.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>
    fun observeProjectDetail(projectId: Long): Flow<ProjectDetail?>
    suspend fun addProject(draft: ProjectDraft): Long
    suspend fun updateProject(id: Long, draft: ProjectDraft)
    suspend fun setProjectStatus(id: Long, status: ProjectStatus)
    suspend fun deleteProject(id: Long)

    suspend fun addMilestone(projectId: Long, title: String, dueDate: LocalDate? = null): Long
    suspend fun toggleMilestone(id: Long, completed: Boolean)
    suspend fun deleteMilestone(id: Long)

    suspend fun linkTask(projectId: Long, taskId: Long)
    suspend fun unlinkTask(projectId: Long, taskId: Long)
    suspend fun linkGoal(projectId: Long, goalId: Long)
    suspend fun unlinkGoal(projectId: Long, goalId: Long)

    fun observeTaskProjectIds(taskId: Long): Flow<List<Long>>
    fun observeGoalProjectIds(goalId: Long): Flow<List<Long>>
}

class RoomProjectRepository(
    private val projectDao: ProjectDao,
    private val planDao: PlanDao,
    private val goalDao: GoalDao,
    private val now: () -> Long = System::currentTimeMillis,
) : ProjectRepository {

    override fun observeProjects(): Flow<List<Project>> =
        projectDao.observeProjects().map { list -> list.map { it.toDomain() } }

    override fun observeProjectDetail(projectId: Long): Flow<ProjectDetail?> {
        val f1 = combine(
            projectDao.observeProject(projectId),
            projectDao.observeMilestones(projectId),
            projectDao.observeTaskLinks(projectId),
            ::Triple,
        )
        val f2 = combine(
            projectDao.observeGoalLinks(projectId),
            planDao.observeTasks(),
            goalDao.observeGoals(),
            ::Triple,
        )
        return combine(f1, f2) { (entity, milestones, taskLinks), (goalLinks, allTasks, allGoals) ->
            if (entity == null) return@combine null

            val linkedTaskIds = taskLinks.map { it.taskId }.toSet()
            val linkedTasks = allTasks
                .filter { it.id in linkedTaskIds }
                .map { task ->
                    PlanTask(
                        id = task.id,
                        title = task.title,
                        note = task.note,
                        scheduledEpochDay = task.scheduledEpochDay,
                        dueMinutes = task.dueMinutes,
                        priority = task.priority,
                        estimatedMinutes = task.estimatedMinutes,
                        actualMinutes = task.actualMinutes,
                        recurrence = task.recurrence,
                        completed = task.status == "Completed",
                        sortOrder = task.sortOrder,
                        carryCount = task.carryCount,
                        checklist = emptyList(),
                        startMinutes = task.startMinutes,
                        endMinutes = task.endMinutes,
                        tags = task.tags,
                    )
                }

            val linkedGoalIds = goalLinks.map { it.goalId }.toSet()
            val linkedGoals = allGoals
                .filter { it.id in linkedGoalIds }
                .map { it.toDomain() }

            ProjectDetail(
                project = entity.toDomain(),
                milestones = milestones.map { it.toDomain() },
                linkedTasks = linkedTasks,
                linkedGoals = linkedGoals,
            )
        }
    }

    override suspend fun addProject(draft: ProjectDraft): Long {
        val timestamp = now()
        return projectDao.insertProject(
            ProjectEntity(
                title = draft.title.trim(),
                description = draft.description.trim(),
                colorArgb = draft.colorArgb,
                emoji = draft.emoji.ifBlank { "📁" },
                status = ProjectStatus.Active.name,
                targetDateEpochDay = draft.targetDate?.toEpochDay(),
                sortOrder = 0,
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    override suspend fun updateProject(id: Long, draft: ProjectDraft) {
        val existing = projectDao.getProject(id) ?: return
        projectDao.updateProject(
            existing.copy(
                title = draft.title.trim(),
                description = draft.description.trim(),
                colorArgb = draft.colorArgb,
                emoji = draft.emoji.ifBlank { "📁" },
                targetDateEpochDay = draft.targetDate?.toEpochDay(),
                updatedAt = now(),
            ),
        )
    }

    override suspend fun setProjectStatus(id: Long, status: ProjectStatus) {
        val existing = projectDao.getProject(id) ?: return
        projectDao.updateProject(
            existing.copy(
                status = status.name,
                updatedAt = now(),
            ),
        )
    }

    override suspend fun deleteProject(id: Long) {
        projectDao.deleteProject(id)
    }

    override suspend fun addMilestone(projectId: Long, title: String, dueDate: LocalDate?): Long {
        val existingCount = projectDao.getMilestones(projectId).size
        return projectDao.insertMilestone(
            ProjectMilestoneEntity(
                projectId = projectId,
                title = title.trim(),
                sortOrder = existingCount,
                completed = false,
                dueDateEpochDay = dueDate?.toEpochDay(),
                createdAt = now(),
            ),
        )
    }

    override suspend fun toggleMilestone(id: Long, completed: Boolean) {
        projectDao.setMilestoneCompleted(id, completed)
    }

    override suspend fun deleteMilestone(id: Long) {
        projectDao.deleteMilestone(id)
    }

    override suspend fun linkTask(projectId: Long, taskId: Long) {
        projectDao.insertTaskLink(ProjectTaskLinkEntity(projectId = projectId, taskId = taskId))
    }

    override suspend fun unlinkTask(projectId: Long, taskId: Long) {
        projectDao.deleteTaskLink(projectId, taskId)
    }

    override suspend fun linkGoal(projectId: Long, goalId: Long) {
        projectDao.insertGoalLink(ProjectGoalLinkEntity(projectId = projectId, goalId = goalId))
    }

    override suspend fun unlinkGoal(projectId: Long, goalId: Long) {
        projectDao.deleteGoalLink(projectId, goalId)
    }

    override fun observeTaskProjectIds(taskId: Long): Flow<List<Long>> =
        projectDao.observeAllTaskLinks().map { links -> links.filter { it.taskId == taskId }.map { it.projectId } }

    override fun observeGoalProjectIds(goalId: Long): Flow<List<Long>> =
        projectDao.observeAllGoalLinks().map { links -> links.filter { it.goalId == goalId }.map { it.projectId } }
}
