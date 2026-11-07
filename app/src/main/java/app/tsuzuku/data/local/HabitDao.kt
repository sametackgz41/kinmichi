package app.tsuzuku.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit_feelings")
    fun observeFeelings(): Flow<List<HabitFeelingEntity>>

    @androidx.room.Upsert
    suspend fun upsertFeeling(item: HabitFeelingEntity)

    @Query("DELETE FROM habit_feelings WHERE habitId = :habitId AND localDate = :localDate")
    suspend fun deleteFeeling(habitId: Long, localDate: String)

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY sortOrder, createdAt")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY sortOrder, createdAt")
    suspend fun getActiveHabits(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabit(id: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE isArchived = 0 AND reminderMinutes IS NOT NULL")
    suspend fun getHabitsWithReminders(): List<HabitEntity>

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun countHabits(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Query("UPDATE habits SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int, updatedAt: Long)

    @Query("UPDATE habits SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveHabit(id: Long, updatedAt: Long)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Query("SELECT * FROM habit_entries WHERE localDate = :localDate ORDER BY createdAt")
    fun observeEntries(localDate: String): Flow<List<HabitEntryEntity>>

    @Query("SELECT * FROM habit_entries ORDER BY localDate, createdAt")
    fun observeAllEntries(): Flow<List<HabitEntryEntity>>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND localDate = :localDate ORDER BY createdAt")
    suspend fun getEntries(habitId: Long, localDate: String): List<HabitEntryEntity>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND localDate BETWEEN :startDate AND :endDate ORDER BY localDate, createdAt")
    suspend fun getEntriesBetween(habitId: Long, startDate: String, endDate: String): List<HabitEntryEntity>

    @Insert
    suspend fun insertEntry(entry: HabitEntryEntity): Long

    @Insert
    suspend fun insertEntries(entries: List<HabitEntryEntity>)

    @Query("DELETE FROM habit_entries WHERE habitId = :habitId AND localDate = :localDate")
    suspend fun clearEntries(habitId: Long, localDate: String)

    /** Latest logging time per habit and day since [fromDate]; roughly when the habit got done. */
    @Query("SELECT habitId, localDate, MAX(updatedAt) AS at FROM habit_entries WHERE localDate >= :fromDate AND status IN ('COMPLETED', 'PROGRESS') GROUP BY habitId, localDate")
    suspend fun completionTimes(fromDate: String): List<CompletionTime>

    @Query("UPDATE habits SET reminderMinutes = :minutes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateReminderMinutes(id: Long, minutes: Int, updatedAt: Long)

    @Query("DELETE FROM habit_entries WHERE habitId = :habitId AND localDate = :localDate AND status = 'HEALTH'")
    suspend fun clearHealthEntries(habitId: Long, localDate: String)

    @Query("DELETE FROM habit_entries WHERE localDate BETWEEN :startDate AND :endDate AND status = 'SKIPPED' AND note = 'VACATION'")
    suspend fun clearSkippedEntries(startDate: String, endDate: String)

    @Query("SELECT * FROM weekly_reviews ORDER BY weekStartEpochDay DESC")
    fun observeWeeklyReviews(): Flow<List<WeeklyReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklyReview(review: WeeklyReviewEntity)

    @Query(
        "DELETE FROM habit_entries WHERE id = (" +
            "SELECT id FROM habit_entries " +
            "WHERE habitId = :habitId AND localDate = :localDate AND status = 'PROGRESS' " +
            "ORDER BY createdAt DESC LIMIT 1)",
    )
    suspend fun deleteLatestProgress(habitId: Long, localDate: String)

    @Transaction
    suspend fun replaceDayWithEntry(
        habitId: Long,
        localDate: String,
        entry: HabitEntryEntity,
    ) {
        clearEntries(habitId, localDate)
        insertEntry(entry)
    }

    @Query("SELECT * FROM habit_levels WHERE habitId = :habitId ORDER BY levelNumber ASC")
    fun observeHabitLevels(habitId: Long): Flow<List<HabitLevelEntity>>

    @Query("SELECT * FROM habit_levels WHERE habitId = :habitId ORDER BY levelNumber ASC")
    suspend fun getHabitLevels(habitId: Long): List<HabitLevelEntity>

    @Query("SELECT * FROM habit_levels ORDER BY habitId, levelNumber ASC")
    suspend fun getAllHabitLevels(): List<HabitLevelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLevel(level: HabitLevelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLevels(levels: List<HabitLevelEntity>)

    @Query("DELETE FROM habit_levels WHERE id = :id")
    suspend fun deleteHabitLevel(id: Long)

    @Query("DELETE FROM habit_levels WHERE habitId = :habitId")
    suspend fun clearHabitLevels(habitId: Long)

    @Query("UPDATE habits SET currentLevel = :levelNumber, target = :newTarget, minimumTarget = :newMinTarget, maximumTarget = :newMaxTarget, updatedAt = :updatedAt WHERE id = :habitId")
    suspend fun advanceHabitLevel(
        habitId: Long,
        levelNumber: Int,
        newTarget: Double,
        newMinTarget: Double,
        newMaxTarget: Double?,
        updatedAt: Long,
    )
}

/** One row of [HabitDao.completionTimes]. */
data class CompletionTime(val habitId: Long, val localDate: String, val at: Long)
