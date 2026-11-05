package app.tsuzuku.model

enum class ContentCategory {
    Book,
    Novel,
    Manga,
    Movie,
    Series,
    Anime,
    Custom,
}

enum class ContentStatus {
    Planned,
    InProgress,
    Completed,
    Paused,
    Dropped,
}

data class LibraryContent(
    val id: Long,
    val title: String,
    val category: ContentCategory,
    val status: ContentStatus,
    val creator: String,
    val coverUri: String?,
    val currentUnits: Double,
    val totalUnits: Double,
    val unit: String,
    val generalNote: String,
    val sortOrder: Int,
    val tags: String = "",
    val rating: Float? = null,
    val isFavorite: Boolean = false,
    val startDateEpochDay: Long? = null,
    val finishDateEpochDay: Long? = null,
    val reviewNote: String = "",
    val rewatchDesired: Boolean = false,
) {
    val progressFraction: Float
        get() = if (totalUnits <= 0.0) 0f else (currentUnits / totalUnits).coerceIn(0.0, 1.0).toFloat()

    val usesSeasons: Boolean
        get() = category == ContentCategory.Series || category == ContentCategory.Anime
}

data class LibraryCollection(
    val id: Long,
    val name: String,
    val emoji: String,
    val createdAt: Long,
    val itemCount: Int = 0,
)

data class ContentSeason(
    val id: Long,
    val contentId: Long,
    val number: Int,
    val title: String,
    val currentEpisodes: Int,
    val totalEpisodes: Int,
) {
    val progressFraction: Float
        get() = if (totalEpisodes <= 0) 0f else (currentEpisodes.toFloat() / totalEpisodes).coerceIn(0f, 1f)
}

data class ContentNote(
    val id: Long,
    val contentId: Long,
    val text: String,
    val createdAt: Long,
)

data class LibraryItem(
    val content: LibraryContent,
    val seasons: List<ContentSeason> = emptyList(),
)

data class LibraryDetail(
    val item: LibraryItem,
    val notes: List<ContentNote>,
    val collectionIds: List<Long> = emptyList(),
)

data class ContentDraft(
    val title: String,
    val category: ContentCategory,
    val status: ContentStatus,
    val creator: String,
    val coverUri: String? = null,
    val currentUnits: Double,
    val totalUnits: Double,
    val unit: String,
    val generalNote: String,
    val firstSeasonTotal: Int = 0,
    val tags: String = "",
    val rating: Float? = null,
    val isFavorite: Boolean = false,
    val startDateEpochDay: Long? = null,
    val finishDateEpochDay: Long? = null,
    val reviewNote: String = "",
    val rewatchDesired: Boolean = false,
)
