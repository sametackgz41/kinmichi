package app.tsuzuku.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "goals", indices = [Index(value = ["status"]), Index(value = ["dueDateEpochDay"])])
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val currentValue: Double,
    val targetValue: Double,
    val unit: String,
    val dueDateEpochDay: Long? = null,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)

