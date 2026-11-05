package app.tsuzuku.data

import app.tsuzuku.data.local.ContentNoteEntity
import app.tsuzuku.data.local.ContentSeasonEntity
import app.tsuzuku.data.local.ContentWithSeasons
import app.tsuzuku.data.local.LibraryCollectionEntity
import app.tsuzuku.data.local.LibraryCollectionItemEntity
import app.tsuzuku.data.local.LibraryContentEntity
import app.tsuzuku.data.local.LibraryDao
import app.tsuzuku.model.ContentCategory
import app.tsuzuku.model.ContentDraft
import app.tsuzuku.model.ContentNote
import app.tsuzuku.model.ContentSeason
import app.tsuzuku.model.ContentStatus
import app.tsuzuku.model.LibraryCollection
import app.tsuzuku.model.LibraryContent
import app.tsuzuku.model.LibraryDetail
import app.tsuzuku.model.LibraryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

interface LibraryRepository {
    fun observeLibrary(): Flow<List<LibraryItem>>
    fun observeDetail(id: Long): Flow<LibraryDetail?>
    suspend fun addContent(draft: ContentDraft): Long
    suspend fun updateContent(id: Long, draft: ContentDraft)
    suspend fun changeProgress(id: Long, delta: Double)
    suspend fun updateStatus(id: Long, status: ContentStatus)
    suspend fun updateGeneralNote(id: Long, note: String)
    suspend fun addShortNote(id: Long, note: String)
    suspend fun deleteShortNote(noteId: Long)
    suspend fun addSeason(contentId: Long, title: String, totalEpisodes: Int)
    suspend fun changeSeasonProgress(seasonId: Long, delta: Int)
    suspend fun deleteContent(id: Long)
    suspend fun moveContent(id: Long, direction: Int)
    fun observeCollections(): Flow<List<LibraryCollection>>
    suspend fun createCollection(name: String, emoji: String): Long
    suspend fun deleteCollection(id: Long)
    fun observeCollectionContentIds(collectionId: Long): Flow<List<Long>>
    suspend fun addContentToCollection(collectionId: Long, contentId: Long)
    suspend fun removeContentFromCollection(collectionId: Long, contentId: Long)
    fun observeCollectionIdsForContent(contentId: Long): Flow<List<Long>>
    suspend fun updateReview(id: Long, rating: Float?, isFavorite: Boolean, reviewNote: String, startDateEpochDay: Long?, finishDateEpochDay: Long?, rewatchDesired: Boolean)
}

class RoomLibraryRepository(
    private val dao: LibraryDao,
    private val now: () -> Long = System::currentTimeMillis,
) : LibraryRepository {
    override fun observeCollectionIdsForContent(contentId: Long): Flow<List<Long>> =
        dao.observeCollectionsForContent(contentId)

    override fun observeLibrary(): Flow<List<LibraryItem>> =
        dao.observeContents().map { contents -> contents.map(ContentWithSeasons::toDomain) }

    override fun observeDetail(id: Long): Flow<LibraryDetail?> = combine(
        dao.observeContent(id),
        dao.observeNotes(id),
        dao.observeCollectionsForContent(id),
    ) { content, notes, collectionIds ->
        content?.let {
            LibraryDetail(
                item = it.toDomain(),
                notes = notes.map(ContentNoteEntity::toDomain),
                collectionIds = collectionIds,
            )
        }
    }

    override suspend fun addContent(draft: ContentDraft): Long {
        val timestamp = now()
        val contentId = dao.insertContent(
            draft.toEntity(
                sortOrder = dao.countContents(),
                timestamp = timestamp,
            ),
        )
        if (draft.category.usesSeasons() && draft.firstSeasonTotal > 0) {
            dao.insertSeasonAndRefresh(
                ContentSeasonEntity(
                    contentId = contentId,
                    number = 1,
                    title = "",
                    currentEpisodes = draft.currentUnits.toInt().coerceIn(0, draft.firstSeasonTotal),
                    totalEpisodes = draft.firstSeasonTotal,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                ),
            )
        }
        return contentId
    }

    override suspend fun updateContent(id: Long, draft: ContentDraft) {
        val current = dao.getContent(id) ?: return
        val timestamp = now()
        dao.updateContent(
            draft.toEntity(
                id = id,
                sortOrder = current.sortOrder,
                createdAt = current.createdAt,
                timestamp = timestamp,
            ).let { updated ->
                if (draft.category.usesSeasons()) {
                    updated.copy(currentUnits = current.currentUnits, totalUnits = current.totalUnits)
                } else {
                    updated
                }
            },
        )
    }

    override suspend fun changeProgress(id: Long, delta: Double) {
        val content = dao.getContent(id) ?: return
        val next = (content.currentUnits + delta).coerceIn(0.0, content.totalUnits.coerceAtLeast(0.0))
        val status = when {
            content.totalUnits > 0.0 && next >= content.totalUnits -> ContentStatus.Completed
            next > 0.0 -> ContentStatus.InProgress
            else -> ContentStatus.Planned
        }
        dao.updateContent(content.copy(currentUnits = next, status = status.name, updatedAt = now()))
    }

    override suspend fun updateStatus(id: Long, status: ContentStatus) {
        val content = dao.getContent(id) ?: return
        if (
            status == ContentStatus.Completed &&
            (content.category == ContentCategory.Series.name || content.category == ContentCategory.Anime.name)
        ) {
            dao.completeSeasonsAndRefresh(id, now())
            return
        }
        val progress = if (status == ContentStatus.Completed && content.totalUnits > 0) {
            content.totalUnits
        } else {
            content.currentUnits
        }
        dao.updateContent(content.copy(status = status.name, currentUnits = progress, updatedAt = now()))
    }

    override suspend fun updateGeneralNote(id: Long, note: String) {
        val content = dao.getContent(id) ?: return
        dao.updateContent(content.copy(generalNote = note.trim(), updatedAt = now()))
    }

    override suspend fun addShortNote(id: Long, note: String) {
        if (note.isBlank()) return
        val timestamp = now()
        dao.insertNote(ContentNoteEntity(contentId = id, text = note.trim(), createdAt = timestamp))
        dao.getContent(id)?.let { dao.updateContent(it.copy(updatedAt = timestamp)) }
    }

    override suspend fun deleteShortNote(noteId: Long) = dao.deleteNote(noteId)

    override suspend fun addSeason(contentId: Long, title: String, totalEpisodes: Int) {
        if (totalEpisodes <= 0) return
        val detail = dao.getSeasons(contentId)
        val timestamp = now()
        dao.insertSeasonAndRefresh(
            ContentSeasonEntity(
                contentId = contentId,
                number = (detail.maxOfOrNull(ContentSeasonEntity::number) ?: 0) + 1,
                title = title.trim(),
                currentEpisodes = 0,
                totalEpisodes = totalEpisodes,
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    override suspend fun changeSeasonProgress(seasonId: Long, delta: Int) {
        val season = dao.getSeason(seasonId) ?: return
        val next = (season.currentEpisodes + delta).coerceIn(0, season.totalEpisodes)
        dao.updateSeasonAndRefresh(season.copy(currentEpisodes = next, updatedAt = now()))
    }

    override suspend fun deleteContent(id: Long) = dao.deleteContent(id)

    override suspend fun moveContent(id: Long, direction: Int) {
        val contents = dao.getContentsForOrdering()
        val index = contents.indexOfFirst { it.id == id }
        if (index < 0) return
        val target = (index + direction).coerceIn(contents.indices)
        if (target == index) return
        val reordered = contents.toMutableList().apply { add(target, removeAt(index)) }
        reordered.forEachIndexed { order, content -> dao.updateContentSortOrder(content.id, order) }
    }

    override fun observeCollections(): Flow<List<LibraryCollection>> =
        dao.observeCollections().map { collections ->
            collections.map { col ->
                val count = dao.getCollectionItemContentIds(col.id).size
                LibraryCollection(col.id, col.name, col.emoji, col.createdAt, count)
            }
        }

    override suspend fun createCollection(name: String, emoji: String): Long =
        dao.insertCollection(LibraryCollectionEntity(name = name.trim(), emoji = emoji.ifBlank { "📁" }, createdAt = now()))

    override suspend fun deleteCollection(id: Long) = dao.deleteCollection(id)

    override fun observeCollectionContentIds(collectionId: Long): Flow<List<Long>> =
        dao.observeCollectionItemContentIds(collectionId)

    override suspend fun addContentToCollection(collectionId: Long, contentId: Long) {
        dao.insertCollectionItem(LibraryCollectionItemEntity(collectionId = collectionId, contentId = contentId))
    }

    override suspend fun removeContentFromCollection(collectionId: Long, contentId: Long) {
        dao.deleteCollectionItem(collectionId, contentId)
    }

    override suspend fun updateReview(
        id: Long,
        rating: Float?,
        isFavorite: Boolean,
        reviewNote: String,
        startDateEpochDay: Long?,
        finishDateEpochDay: Long?,
        rewatchDesired: Boolean,
    ) {
        dao.updateReview(
            id = id,
            rating = rating,
            isFavorite = isFavorite,
            reviewNote = reviewNote.trim(),
            startDateEpochDay = startDateEpochDay,
            finishDateEpochDay = finishDateEpochDay,
            rewatchDesired = rewatchDesired,
            updatedAt = now(),
        )
    }
}

private fun ContentDraft.toEntity(
    id: Long = 0,
    sortOrder: Int,
    createdAt: Long = 0,
    timestamp: Long,
): LibraryContentEntity {
    val normalizedTotal = if (category.usesSeasons()) firstSeasonTotal.toDouble() else totalUnits.coerceAtLeast(0.0)
    val enteredCurrent = currentUnits.coerceIn(0.0, normalizedTotal)
    val normalizedCurrent = if (status == ContentStatus.Completed && normalizedTotal > 0.0) {
        normalizedTotal
    } else {
        enteredCurrent
    }
    val normalizedStatus = when {
        normalizedTotal > 0.0 && normalizedCurrent >= normalizedTotal -> ContentStatus.Completed
        normalizedCurrent > 0.0 && status == ContentStatus.Planned -> ContentStatus.InProgress
        else -> status
    }
    return LibraryContentEntity(
        id = id,
        title = title.trim(),
        category = category.name,
        status = normalizedStatus.name,
        creator = creator.trim(),
        coverUri = coverUri?.trim()?.takeIf(String::isNotBlank),
        currentUnits = normalizedCurrent,
        totalUnits = normalizedTotal,
        unit = unit.trim(),
        generalNote = generalNote.trim(),
        sortOrder = sortOrder,
        createdAt = createdAt.takeIf { it > 0 } ?: timestamp,
        updatedAt = timestamp,
        tags = tags.split(',', ';', '#').map(String::trim).filter(String::isNotBlank).distinct().joinToString(","),
        rating = rating,
        isFavorite = isFavorite,
        startDateEpochDay = startDateEpochDay,
        finishDateEpochDay = finishDateEpochDay,
        reviewNote = reviewNote.trim(),
        rewatchDesired = rewatchDesired,
    )
}

private fun ContentWithSeasons.toDomain() = LibraryItem(
    content = content.toDomain(),
    seasons = seasons.sortedBy(ContentSeasonEntity::number).map(ContentSeasonEntity::toDomain),
)

private fun LibraryContentEntity.toDomain() = LibraryContent(
    id = id,
    title = title,
    category = enumValueOrDefault(category, ContentCategory.Custom),
    status = enumValueOrDefault(status, ContentStatus.Planned),
    creator = creator,
    coverUri = coverUri,
    currentUnits = currentUnits,
    totalUnits = totalUnits,
    unit = unit,
    generalNote = generalNote,
    sortOrder = sortOrder,
    tags = tags,
    rating = rating,
    isFavorite = isFavorite,
    startDateEpochDay = startDateEpochDay,
    finishDateEpochDay = finishDateEpochDay,
    reviewNote = reviewNote,
    rewatchDesired = rewatchDesired,
)

private fun ContentSeasonEntity.toDomain() = ContentSeason(
    id = id,
    contentId = contentId,
    number = number,
    title = title,
    currentEpisodes = currentEpisodes,
    totalEpisodes = totalEpisodes,
)

private fun ContentNoteEntity.toDomain() = ContentNote(
    id = id,
    contentId = contentId,
    text = text,
    createdAt = createdAt,
)

private fun ContentCategory.usesSeasons(): Boolean = this == ContentCategory.Series || this == ContentCategory.Anime

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)
