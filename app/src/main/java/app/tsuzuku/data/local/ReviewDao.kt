package app.tsuzuku.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Query("SELECT * FROM daily_reviews WHERE epochDay = :epochDay LIMIT 1")
    fun observeDailyReview(epochDay: Long): Flow<DailyReviewEntity?>

    @Query("SELECT * FROM daily_reviews ORDER BY epochDay DESC")
    fun observeDailyReviews(): Flow<List<DailyReviewEntity>>

    @Query("SELECT * FROM daily_reviews WHERE epochDay = :epochDay LIMIT 1")
    suspend fun getDailyReview(epochDay: Long): DailyReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyReview(review: DailyReviewEntity)

    @Query("DELETE FROM daily_reviews WHERE epochDay = :epochDay")
    suspend fun deleteDailyReview(epochDay: Long)

    @Query("SELECT * FROM daily_reviews ORDER BY epochDay DESC")
    suspend fun getAllDailyReviews(): List<DailyReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyReviews(reviews: List<DailyReviewEntity>)

    @Query("SELECT * FROM period_reviews ORDER BY periodKey DESC")
    fun observePeriodReviews(): Flow<List<PeriodReviewEntity>>

    @Query("SELECT * FROM period_reviews WHERE periodType = :periodType AND periodKey = :periodKey LIMIT 1")
    suspend fun getPeriodReview(periodType: String, periodKey: String): PeriodReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeriodReview(review: PeriodReviewEntity)

    @Query("DELETE FROM period_reviews WHERE periodType = :periodType AND periodKey = :periodKey")
    suspend fun deletePeriodReview(periodType: String, periodKey: String)

    @Query("SELECT * FROM period_reviews ORDER BY periodKey DESC")
    suspend fun getAllPeriodReviews(): List<PeriodReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriodReviews(reviews: List<PeriodReviewEntity>)
}
