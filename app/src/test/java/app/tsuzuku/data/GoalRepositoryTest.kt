package app.tsuzuku.data

import app.tsuzuku.data.local.GoalDao
import app.tsuzuku.data.local.GoalEntity
import app.tsuzuku.model.GoalDraft
import app.tsuzuku.model.GoalStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalRepositoryTest {
    @Test
    fun draftIsTrimmedAndCompletedWhenInitialProgressReachesTarget() = runTest {
        val repository = RoomGoalRepository(FakeGoalDao(), now = { 42L })
        repository.addGoal(GoalDraft("  On kitap  ", "  Açıklama  ", 12.0, 10.0, " kitap "))

        val goal = repository.observeGoals().first().single()
        assertEquals("On kitap", goal.title)
        assertEquals("Açıklama", goal.description)
        assertEquals(10.0, goal.currentValue, 0.0)
        assertEquals(GoalStatus.Completed, goal.status)
    }

    @Test
    fun progressCompletesAndReactivatesGoal() = runTest {
        val dao = FakeGoalDao()
        val repository = RoomGoalRepository(dao, now = { 50L })
        val id = repository.addGoal(GoalDraft("Hedef", currentValue = 0.0, targetValue = 2.0, unit = "adım"))

        repository.changeProgress(id, 2.0)
        assertEquals(GoalStatus.Completed, repository.observeGoals().first().single().status)

        repository.changeProgress(id, -1.0)
        val reopened = repository.observeGoals().first().single()
        assertEquals(GoalStatus.Active, reopened.status)
        assertEquals(1.0, reopened.currentValue, 0.0)
    }
}

private class FakeGoalDao : GoalDao {
    private val state = MutableStateFlow<List<GoalEntity>>(emptyList())
    private var nextId = 1L

    override fun observeGoals(): Flow<List<GoalEntity>> = state
    override suspend fun getGoal(id: Long): GoalEntity? = state.value.firstOrNull { it.id == id }

    override suspend fun insertGoal(goal: GoalEntity): Long {
        val id = goal.id.takeIf { it > 0 } ?: nextId++
        state.value = state.value.filterNot { it.id == id } + goal.copy(id = id)
        return id
    }

    override suspend fun updateGoal(goal: GoalEntity) {
        state.value = state.value.map { if (it.id == goal.id) goal else it }
    }

    override suspend fun deleteGoal(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
}
