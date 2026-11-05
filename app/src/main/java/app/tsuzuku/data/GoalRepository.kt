package app.tsuzuku.data

import app.tsuzuku.data.local.GoalDao
import app.tsuzuku.data.local.GoalEntity
import app.tsuzuku.model.Goal
import app.tsuzuku.model.GoalDraft
import app.tsuzuku.model.GoalStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface GoalRepository {
    fun observeGoals(): Flow<List<Goal>>
    suspend fun addGoal(draft: GoalDraft): Long
    suspend fun updateGoal(id: Long, draft: GoalDraft)
    suspend fun changeProgress(id: Long, delta: Double)
    suspend fun setCompleted(id: Long, completed: Boolean)
    suspend fun archiveGoal(id: Long)
    suspend fun deleteGoal(id: Long)
}

class RoomGoalRepository(
    private val dao: GoalDao,
    private val now: () -> Long = System::currentTimeMillis,
) : GoalRepository {
    override fun observeGoals(): Flow<List<Goal>> =
        dao.observeGoals().map { goals -> goals.map(GoalEntity::toDomain) }

    override suspend fun addGoal(draft: GoalDraft): Long {
        val timestamp = now()
        return dao.insertGoal(draft.toEntity(timestamp = timestamp))
    }

    override suspend fun updateGoal(id: Long, draft: GoalDraft) {
        val current = dao.getGoal(id) ?: return
        dao.updateGoal(
            draft.toEntity(
                id = id,
                createdAt = current.createdAt,
                timestamp = now(),
            ),
        )
    }

    override suspend fun changeProgress(id: Long, delta: Double) {
        val goal = dao.getGoal(id) ?: return
        val next = (goal.currentValue + delta).coerceIn(0.0, goal.targetValue)
        val status = if (next >= goal.targetValue) GoalStatus.Completed else GoalStatus.Active
        dao.updateGoal(goal.copy(currentValue = next, status = status.name, updatedAt = now()))
    }

    override suspend fun setCompleted(id: Long, completed: Boolean) {
        val goal = dao.getGoal(id) ?: return
        dao.updateGoal(
            goal.copy(
                currentValue = if (completed) goal.targetValue else 0.0,
                status = if (completed) GoalStatus.Completed.name else GoalStatus.Active.name,
                updatedAt = now(),
            ),
        )
    }

    override suspend fun archiveGoal(id: Long) {
        val goal = dao.getGoal(id) ?: return
        dao.updateGoal(goal.copy(status = GoalStatus.Archived.name, updatedAt = now()))
    }

    override suspend fun deleteGoal(id: Long) = dao.deleteGoal(id)
}

private fun GoalDraft.toEntity(
    id: Long = 0,
    createdAt: Long = 0,
    timestamp: Long,
): GoalEntity {
    val normalizedTarget = targetValue.coerceAtLeast(0.01)
    val normalizedCurrent = currentValue.coerceIn(0.0, normalizedTarget)
    return GoalEntity(
        id = id,
        title = title.trim(),
        description = description.trim(),
        currentValue = normalizedCurrent,
        targetValue = normalizedTarget,
        unit = unit.trim(),
        dueDateEpochDay = dueDateEpochDay,
        status = if (normalizedCurrent >= normalizedTarget) GoalStatus.Completed.name else GoalStatus.Active.name,
        createdAt = createdAt.takeIf { it > 0 } ?: timestamp,
        updatedAt = timestamp,
    )
}

fun GoalEntity.toDomain() = Goal(
    id = id,
    title = title,
    description = description,
    currentValue = currentValue,
    targetValue = targetValue,
    unit = unit,
    dueDateEpochDay = dueDateEpochDay,
    status = runCatching { GoalStatus.valueOf(status) }.getOrDefault(GoalStatus.Active),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

