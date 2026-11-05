package app.tsuzuku.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "daily_reviews")
data class DailyReviewEntity(
    @PrimaryKey val epochDay: Long,
    val mood: Int, // 1 to 5: 1=Zor, 2=Yorgun, 3=Nötr, 4=İyi, 5=Harika
    val energy: Int, // 1 to 5
    val productivity: Int, // 1 to 5
    val stress: Int, // 1 to 5
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "period_reviews",
    primaryKeys = ["periodType", "periodKey"],
    indices = [Index(value = ["periodType", "periodKey"])],
)
data class PeriodReviewEntity(
    val periodType: String, // "MONTHLY", "YEARLY"
    val periodKey: String, // "2026-09", "2026"
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)
