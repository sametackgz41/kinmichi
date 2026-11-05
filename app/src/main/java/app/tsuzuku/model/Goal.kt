package app.tsuzuku.model

enum class GoalStatus {
    Active,
    Completed,
    Archived,
}

data class Goal(
    val id: Long,
    val title: String,
    val description: String,
    val currentValue: Double,
    val targetValue: Double,
    val unit: String,
    val dueDateEpochDay: Long?,
    val status: GoalStatus,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val progressFraction: Float
        get() = if (targetValue <= 0.0) 0f else (currentValue / targetValue).toFloat().coerceIn(0f, 1f)
}

data class GoalDraft(
    val title: String,
    val description: String = "",
    val currentValue: Double = 0.0,
    val targetValue: Double,
    val unit: String,
    val dueDateEpochDay: Long? = null,
)

