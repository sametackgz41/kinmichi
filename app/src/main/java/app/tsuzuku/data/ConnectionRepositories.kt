package app.tsuzuku.data

import app.tsuzuku.data.local.ConnectionDao
import app.tsuzuku.data.local.FocusDao
import app.tsuzuku.data.local.FocusSessionEntity
import app.tsuzuku.data.local.HabitGoalLinkEntity
import app.tsuzuku.data.local.HabitLibraryLinkEntity
import app.tsuzuku.data.local.InboxDao
import app.tsuzuku.data.local.InboxItemEntity
import app.tsuzuku.data.local.TaskGoalLinkEntity
import app.tsuzuku.model.FocusMode
import app.tsuzuku.model.FocusSession
import app.tsuzuku.model.HabitGoalContributionMode
import app.tsuzuku.model.HabitGoalLink
import app.tsuzuku.model.HabitLibraryLink
import app.tsuzuku.model.InboxItem
import app.tsuzuku.model.InboxStatus
import app.tsuzuku.model.TaskGoalLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FocusRepository(private val dao: FocusDao, private val now: () -> Long = System::currentTimeMillis) {
    fun observeSessions(): Flow<List<FocusSession>> = dao.observeSessions().map { it.map(FocusSessionEntity::toDomain) }

    suspend fun start(mode: FocusMode, taskId: Long? = null, habitId: Long? = null, goalId: Long? = null): Long {
        require(listOf(taskId, habitId, goalId).count { it != null } <= 1) { "A focus session can have one target" }
        val timestamp = now()
        return dao.insert(FocusSessionEntity(taskId = taskId, habitId = habitId, goalId = goalId, mode = mode.name, startedAt = timestamp, createdAt = timestamp))
    }

    suspend fun finish(id: Long, completed: Boolean, interrupted: Boolean = !completed) {
        val session = dao.getSession(id) ?: return
        val endedAt = now().coerceAtLeast(session.startedAt)
        val minutes = ((endedAt - session.startedAt) / 60_000L).toInt().coerceAtLeast(0)
        dao.finish(id, endedAt, minutes, completed, interrupted)
    }
}

class InboxRepository(private val dao: InboxDao, private val now: () -> Long = System::currentTimeMillis) {
    fun observeOpen(): Flow<List<InboxItem>> = dao.observeOpen().map { items -> items.map(InboxItemEntity::toDomain) }
    suspend fun capture(text: String): Long {
        val normalized = text.trim()
        if (normalized.isBlank()) return 0
        val timestamp = now()
        return dao.insert(InboxItemEntity(text = normalized, createdAt = timestamp, updatedAt = timestamp))
    }
    suspend fun markConverted(id: Long) = dao.setStatus(id, InboxStatus.Converted.name, now())
    suspend fun dismiss(id: Long) = dao.setStatus(id, InboxStatus.Dismissed.name, now())
    suspend fun delete(id: Long) = dao.delete(id)
}

class ConnectionRepository(private val dao: ConnectionDao) {
    fun observeHabitGoalLinks(): Flow<List<HabitGoalLink>> = dao.observeHabitGoalLinks().map { links -> links.map(HabitGoalLinkEntity::toDomain) }
    fun observeTaskGoalLinks(): Flow<List<TaskGoalLink>> = dao.observeTaskGoalLinks().map { links -> links.map(TaskGoalLinkEntity::toDomain) }
    fun observeHabitLibraryLinks(): Flow<List<HabitLibraryLink>> = dao.observeHabitLibraryLinks().map { links -> links.map(HabitLibraryLinkEntity::toDomain) }
    suspend fun linkHabitGoal(link: HabitGoalLink) = dao.insertHabitGoal(HabitGoalLinkEntity(habitId = link.habitId, goalId = link.goalId, contributionMode = link.contributionMode.name, contributionValue = link.contributionValue.coerceAtLeast(0.0)))
    suspend fun unlinkHabitGoal(habitId: Long, goalId: Long) = dao.deleteHabitGoal(habitId, goalId)
    suspend fun linkTaskGoal(link: TaskGoalLink) = dao.insertTaskGoal(TaskGoalLinkEntity(taskId = link.taskId, goalId = link.goalId, contributionValue = link.contributionValue.coerceAtLeast(0.0)))
    suspend fun unlinkTaskGoal(taskId: Long, goalId: Long) = dao.deleteTaskGoal(taskId, goalId)
    suspend fun linkHabitLibrary(link: HabitLibraryLink) = dao.insertHabitLibrary(HabitLibraryLinkEntity(habitId = link.habitId, contentId = link.contentId, contributionMultiplier = link.contributionMultiplier.coerceAtLeast(0.0)))
    suspend fun unlinkHabitLibrary(habitId: Long, contentId: Long) = dao.deleteHabitLibrary(habitId, contentId)
}

private fun FocusSessionEntity.toDomain() = FocusSession(id, taskId, habitId, goalId, runCatching { FocusMode.valueOf(mode) }.getOrDefault(FocusMode.Stopwatch), startedAt, endedAt, durationMinutes, completed, interrupted)
private fun InboxItemEntity.toDomain() = InboxItem(id, text, runCatching { InboxStatus.valueOf(status) }.getOrDefault(InboxStatus.Open), createdAt)
private fun HabitGoalLinkEntity.toDomain() = HabitGoalLink(habitId, goalId, runCatching { HabitGoalContributionMode.valueOf(contributionMode) }.getOrDefault(HabitGoalContributionMode.HabitAmount), contributionValue)
private fun TaskGoalLinkEntity.toDomain() = TaskGoalLink(taskId, goalId, contributionValue)
private fun HabitLibraryLinkEntity.toDomain() = HabitLibraryLink(habitId, contentId, contributionMultiplier)
