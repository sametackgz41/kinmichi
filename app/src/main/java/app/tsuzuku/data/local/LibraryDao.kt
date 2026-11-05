package app.tsuzuku.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Transaction
    @Query("SELECT * FROM library_contents ORDER BY updatedAt DESC, sortOrder")
    fun observeContents(): Flow<List<ContentWithSeasons>>

    @Transaction
    @Query("SELECT * FROM library_contents WHERE id = :id LIMIT 1")
    fun observeContent(id: Long): Flow<ContentWithSeasons?>

    @Query("SELECT * FROM content_notes WHERE contentId = :contentId ORDER BY createdAt DESC")
    fun observeNotes(contentId: Long): Flow<List<ContentNoteEntity>>

    @Query("SELECT * FROM library_contents WHERE id = :id LIMIT 1")
    suspend fun getContent(id: Long): LibraryContentEntity?

    @Query("SELECT * FROM library_contents ORDER BY sortOrder, createdAt")
    suspend fun getContentsForOrdering(): List<LibraryContentEntity>

    @Query("SELECT * FROM content_seasons WHERE id = :id LIMIT 1")
    suspend fun getSeason(id: Long): ContentSeasonEntity?

    @Query("SELECT * FROM content_seasons WHERE contentId = :contentId ORDER BY number")
    suspend fun getSeasons(contentId: Long): List<ContentSeasonEntity>

    @Query("SELECT COUNT(*) FROM library_contents")
    suspend fun countContents(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContent(content: LibraryContentEntity): Long

    @Update
    suspend fun updateContent(content: LibraryContentEntity)

    @Query("UPDATE library_contents SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateContentSortOrder(id: Long, sortOrder: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeason(season: ContentSeasonEntity): Long

    @Update
    suspend fun updateSeason(season: ContentSeasonEntity)

    @Insert
    suspend fun insertNote(note: ContentNoteEntity): Long

    @Query("DELETE FROM content_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("DELETE FROM library_contents WHERE id = :id")
    suspend fun deleteContent(id: Long)

    @Query("UPDATE content_seasons SET currentEpisodes = totalEpisodes, updatedAt = :updatedAt WHERE contentId = :contentId")
    suspend fun completeSeasons(contentId: Long, updatedAt: Long)

    @Query(
        "UPDATE library_contents SET " +
            "currentUnits = COALESCE((SELECT SUM(currentEpisodes) FROM content_seasons WHERE contentId = :contentId), 0), " +
            "totalUnits = COALESCE((SELECT SUM(totalEpisodes) FROM content_seasons WHERE contentId = :contentId), 0), " +
            "status = CASE " +
            "WHEN COALESCE((SELECT SUM(totalEpisodes) FROM content_seasons WHERE contentId = :contentId), 0) > 0 " +
            "AND COALESCE((SELECT SUM(currentEpisodes) FROM content_seasons WHERE contentId = :contentId), 0) >= " +
            "COALESCE((SELECT SUM(totalEpisodes) FROM content_seasons WHERE contentId = :contentId), 0) THEN 'Completed' " +
            "WHEN COALESCE((SELECT SUM(currentEpisodes) FROM content_seasons WHERE contentId = :contentId), 0) > 0 THEN 'InProgress' " +
            "ELSE status END, updatedAt = :updatedAt WHERE id = :contentId",
    )
    suspend fun refreshContentFromSeasons(contentId: Long, updatedAt: Long)

    @Transaction
    suspend fun insertSeasonAndRefresh(season: ContentSeasonEntity) {
        insertSeason(season)
        refreshContentFromSeasons(season.contentId, season.updatedAt)
    }

    @Transaction
    suspend fun updateSeasonAndRefresh(season: ContentSeasonEntity) {
        updateSeason(season)
        refreshContentFromSeasons(season.contentId, season.updatedAt)
    }

    @Transaction
    suspend fun completeSeasonsAndRefresh(contentId: Long, updatedAt: Long) {
        completeSeasons(contentId, updatedAt)
        refreshContentFromSeasons(contentId, updatedAt)
    }

    @Query("SELECT * FROM library_collections ORDER BY createdAt")
    fun observeCollections(): Flow<List<LibraryCollectionEntity>>

    @Query("SELECT * FROM library_collections ORDER BY createdAt")
    suspend fun getCollections(): List<LibraryCollectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: LibraryCollectionEntity): Long

    @Query("DELETE FROM library_collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Query("SELECT contentId FROM library_collection_items WHERE collectionId = :collectionId")
    fun observeCollectionItemContentIds(collectionId: Long): Flow<List<Long>>

    @Query("SELECT contentId FROM library_collection_items WHERE collectionId = :collectionId")
    suspend fun getCollectionItemContentIds(collectionId: Long): List<Long>

    @Query("SELECT collectionId FROM library_collection_items WHERE contentId = :contentId")
    fun observeCollectionsForContent(contentId: Long): Flow<List<Long>>

    @Query("SELECT collectionId FROM library_collection_items WHERE contentId = :contentId")
    suspend fun getCollectionsForContent(contentId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollectionItem(item: LibraryCollectionItemEntity): Long

    @Query("DELETE FROM library_collection_items WHERE collectionId = :collectionId AND contentId = :contentId")
    suspend fun deleteCollectionItem(collectionId: Long, contentId: Long)

    @Query("DELETE FROM library_collection_items WHERE contentId = :contentId")
    suspend fun deleteCollectionItemsForContent(contentId: Long)

    @Query("SELECT * FROM library_collections ORDER BY id")
    suspend fun getAllCollections(): List<LibraryCollectionEntity>

    @Query("SELECT * FROM library_collection_items ORDER BY id")
    suspend fun getAllCollectionItems(): List<LibraryCollectionItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(items: List<LibraryCollectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionItems(items: List<LibraryCollectionItemEntity>)

    @Query("UPDATE library_contents SET rating = :rating, isFavorite = :isFavorite, reviewNote = :reviewNote, startDateEpochDay = :startDateEpochDay, finishDateEpochDay = :finishDateEpochDay, rewatchDesired = :rewatchDesired, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateReview(id: Long, rating: Float?, isFavorite: Boolean, reviewNote: String, startDateEpochDay: Long?, finishDateEpochDay: Long?, rewatchDesired: Boolean, updatedAt: Long)
}
