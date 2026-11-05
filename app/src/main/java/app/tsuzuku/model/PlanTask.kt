package app.tsuzuku.model

data class PlanTask(
    val id: Long,
    val title: String,
    val note: String,
    val scheduledEpochDay: Long?,
    val dueMinutes: Int?,
    val priority: Int,
    val estimatedMinutes: Int?,
    val actualMinutes: Int?,
    val recurrence: String,
    val completed: Boolean,
    val sortOrder: Int,
    val carryCount: Int,
    val checklist: List<PlanChecklistItem>,
    val startMinutes: Int?,
    val endMinutes: Int?,
    val tags: String,
)

data class PlanChecklistItem(val id: Long, val title: String, val completed: Boolean)

data class PlanTemplate(
    val id: Long,
    val title: String,
    val note: String,
    val priority: Int,
    val estimatedMinutes: Int?,
    val startMinutes: Int?,
    val endMinutes: Int?,
    val recurrence: String,
    val tags: String = "",
)

data class PlanTaskDraft(
    val title: String,
    val note: String = "",
    val scheduledEpochDay: Long? = null,
    val dueMinutes: Int? = null,
    val priority: Int = 1,
    val estimatedMinutes: Int? = null,
    val recurrence: String = "None",
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val tags: String = "",
)
