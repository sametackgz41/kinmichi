package app.tsuzuku.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorArgb: Long,
    val type: String,
    val direction: String,
    val target: Double,
    val unit: String,
    val step: Double,
    val scheduleDaysMask: Int,
    val sortOrder: Int,
    val reminderMinutes: Int? = null,
    val reminderRepeatMinutes: Int? = null,
    val generalNote: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val goalPeriod: String = "Daily",
    val periodTarget: Int = 1,
    val routineName: String = "",
    val prerequisiteHabitId: Long? = null,
    val healthMetric: String = "None",
    val minimumTarget: Double = 0.0,
    val maximumTarget: Double? = null,
    val difficulty: Int = 2,
    val contextLabel: String = "",
    val intention: String = "",
    val activeStartEpochDay: Long? = null,
    val activeEndEpochDay: Long? = null,
    val repeatIntervalDays: Int = 1,
    val pausedUntilEpochDay: Long? = null,
    val tags: String = "",
    val currentLevel: Int = 1,
    val tinyVersion: String = "",
    /** Set when the habit is in the trash; it is deleted for good 30 days later. */
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "habit_entries",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["localDate"]),
        Index(value = ["habitId", "localDate"]),
    ],
)
data class HabitEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val localDate: String,
    val amount: Double,
    val status: String = STATUS_PROGRESS,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        const val STATUS_PROGRESS = "PROGRESS"
        const val STATUS_HEALTH = "HEALTH"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_SKIPPED = "SKIPPED"

        /** Note on a completion that was the habit's bad-day (tiny) version. */
        const val NOTE_TINY = "TINY"
    }
}

@Entity(tableName = "weekly_reviews")
data class WeeklyReviewEntity(
    @PrimaryKey val weekStartEpochDay: Long,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "habit_levels",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["habitId", "levelNumber"], unique = true),
    ],
)
data class HabitLevelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val levelNumber: Int,
    val name: String,
    val target: Double,
    val minimumTarget: Double = 0.0,
    val maximumTarget: Double? = null,
    val rewardNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** How doing a habit felt on a given day: 1 hard, 2 okay, 3 good. One per habit per day. */
@Entity(
    tableName = "habit_feelings",
    primaryKeys = ["habitId", "localDate"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["habitId"])],
)
data class HabitFeelingEntity(
    val habitId: Long,
    val localDate: String,
    val feeling: Int,
    val updatedAt: Long,
)
