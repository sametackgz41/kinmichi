package app.tsuzuku.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM custom_categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: CategoryEntity): Long

    @Query("DELETE FROM custom_categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM habits") suspend fun getHabits(): List<HabitEntity>
    @Query("SELECT * FROM plan_tasks") suspend fun getTasks(): List<PlanTaskEntity>
    @Query("SELECT * FROM library_contents") suspend fun getContents(): List<LibraryContentEntity>
    @Update suspend fun updateHabits(items: List<HabitEntity>)
    @Update suspend fun updateTasks(items: List<PlanTaskEntity>)
    @Update suspend fun updateContents(items: List<LibraryContentEntity>)

    @Transaction
    suspend fun deleteAndDetach(category: CategoryEntity) {
        val now = System.currentTimeMillis()
        fun without(value: String): String = value.split(',').map(String::trim)
            .filter { it.isNotBlank() && !it.equals(category.name, ignoreCase = true) }.distinct().joinToString(",")
        updateHabits(getHabits().filter { it.tags.split(',').any { tag -> tag.trim().equals(category.name, true) } }.map { it.copy(tags = without(it.tags), updatedAt = now) })
        updateTasks(getTasks().filter { it.tags.split(',').any { tag -> tag.trim().equals(category.name, true) } }.map { it.copy(tags = without(it.tags), updatedAt = now) })
        updateContents(getContents().filter { it.tags.split(',').any { tag -> tag.trim().equals(category.name, true) } }.map { it.copy(tags = without(it.tags), updatedAt = now) })
        delete(category.id)
    }
}
