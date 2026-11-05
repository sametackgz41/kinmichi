package app.tsuzuku.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "plan_tasks", indices = [Index("scheduledEpochDay"), Index("status")])
data class PlanTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val scheduledEpochDay: Long? = null,
    val dueMinutes: Int? = null,
    val priority: Int = 1,
    val estimatedMinutes: Int? = null,
    val actualMinutes: Int? = null,
    val recurrence: String = "None",
    val status: String = "Active",
    val sortOrder: Int = 0,
    val carryCount: Int = 0,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val tags: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "plan_checklist_items",
    foreignKeys = [ForeignKey(
        entity = PlanTaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("taskId")],
)
data class PlanChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val completed: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(tableName = "plan_templates")
data class PlanTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val priority: Int = 1,
    val estimatedMinutes: Int? = null,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val recurrence: String = "None",
    val tags: String = "",
    val createdAt: Long,
)
