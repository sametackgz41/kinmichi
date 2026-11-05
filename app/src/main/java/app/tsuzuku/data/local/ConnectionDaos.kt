package app.tsuzuku.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    suspend fun getSession(id: Long): FocusSessionEntity?

    @Insert
    suspend fun insert(item: FocusSessionEntity): Long

    @Query("UPDATE focus_sessions SET endedAt = :endedAt, durationMinutes = :durationMinutes, completed = :completed, interrupted = :interrupted WHERE id = :id")
    suspend fun finish(id: Long, endedAt: Long, durationMinutes: Int, completed: Boolean, interrupted: Boolean)
}

@Dao
interface InboxDao {
    @Query("SELECT * FROM inbox_items WHERE status = 'Open' ORDER BY createdAt DESC")
    fun observeOpen(): Flow<List<InboxItemEntity>>

    @Insert
    suspend fun insert(item: InboxItemEntity): Long

    @Query("UPDATE inbox_items SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, updatedAt: Long)

    @Query("DELETE FROM inbox_items WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ConnectionDao {
    @Query("SELECT * FROM habit_goal_links") fun observeHabitGoalLinks(): Flow<List<HabitGoalLinkEntity>>
    @Query("SELECT * FROM task_goal_links") fun observeTaskGoalLinks(): Flow<List<TaskGoalLinkEntity>>
    @Query("SELECT * FROM habit_library_links") fun observeHabitLibraryLinks(): Flow<List<HabitLibraryLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertHabitGoal(item: HabitGoalLinkEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTaskGoal(item: TaskGoalLinkEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertHabitLibrary(item: HabitLibraryLinkEntity): Long

    @Query("DELETE FROM habit_goal_links WHERE habitId = :habitId AND goalId = :goalId") suspend fun deleteHabitGoal(habitId: Long, goalId: Long)
    @Query("DELETE FROM task_goal_links WHERE taskId = :taskId AND goalId = :goalId") suspend fun deleteTaskGoal(taskId: Long, goalId: Long)
    @Query("DELETE FROM habit_library_links WHERE habitId = :habitId AND contentId = :contentId") suspend fun deleteHabitLibrary(habitId: Long, contentId: Long)
}
