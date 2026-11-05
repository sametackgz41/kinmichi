package app.tsuzuku.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan_tasks ORDER BY status, scheduledEpochDay IS NULL, scheduledEpochDay, sortOrder, createdAt")
    fun observeTasks(): Flow<List<PlanTaskEntity>>

    @Query("SELECT * FROM plan_checklist_items ORDER BY taskId, sortOrder, id")
    fun observeChecklist(): Flow<List<PlanChecklistItemEntity>>
    @Query("SELECT * FROM plan_checklist_items WHERE taskId = :taskId ORDER BY sortOrder, id") suspend fun getChecklist(taskId: Long): List<PlanChecklistItemEntity>

    @Query("SELECT * FROM plan_tasks WHERE status != 'Completed' ORDER BY sortOrder, createdAt") suspend fun getActiveTasks(): List<PlanTaskEntity>
    @Query("SELECT * FROM plan_tasks WHERE id = :id LIMIT 1") suspend fun getTask(id: Long): PlanTaskEntity?

    @Query("SELECT * FROM plan_templates ORDER BY createdAt DESC") fun observeTemplates(): Flow<List<PlanTemplateEntity>>

    @Insert suspend fun insertTask(task: PlanTaskEntity): Long
    @Update suspend fun updateTask(task: PlanTaskEntity)

    @Query("UPDATE plan_tasks SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, updatedAt: Long)

    @Query("UPDATE plan_tasks SET scheduledEpochDay = :day, carryCount = carryCount + 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun moveTask(id: Long, day: Long?, updatedAt: Long)

    @Query("DELETE FROM plan_tasks WHERE id = :id") suspend fun deleteTask(id: Long)

    @Query("UPDATE plan_tasks SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int, updatedAt: Long)

    @Query("UPDATE plan_tasks SET tags = :tags, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTags(id: Long, tags: String, updatedAt: Long)

    @Query("UPDATE plan_tasks SET actualMinutes = :minutes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateActualMinutes(id: Long, minutes: Int?, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: PlanChecklistItemEntity): Long

    @Query("UPDATE plan_checklist_items SET completed = NOT completed WHERE id = :id")
    suspend fun toggleChecklistItem(id: Long)

    @Query("DELETE FROM plan_checklist_items WHERE id = :id") suspend fun deleteChecklistItem(id: Long)

    @Insert suspend fun insertTemplate(template: PlanTemplateEntity): Long
    @Query("DELETE FROM plan_templates WHERE id = :id") suspend fun deleteTemplate(id: Long)
}
