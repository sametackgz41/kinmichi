package app.tsuzuku.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "library_contents", indices = [Index(value = ["status"]), Index(value = ["category"])])
data class LibraryContentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val status: String,
    val creator: String,
    val coverUri: String? = null,
    val currentUnits: Double,
    val totalUnits: Double,
    val unit: String,
    val generalNote: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: String = "",
    val rating: Float? = null,
    val isFavorite: Boolean = false,
    val startDateEpochDay: Long? = null,
    val finishDateEpochDay: Long? = null,
    val reviewNote: String = "",
    val rewatchDesired: Boolean = false,
)

@Entity(tableName = "library_collections")
data class LibraryCollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val createdAt: Long,
)

@Entity(
    tableName = "library_collection_items",
    foreignKeys = [
        ForeignKey(
            entity = LibraryCollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LibraryContentEntity::class,
            parentColumns = ["id"],
            childColumns = ["contentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["contentId"]),
        Index(value = ["collectionId", "contentId"], unique = true),
    ],
)
data class LibraryCollectionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: Long,
    val contentId: Long,
)

@Entity(
    tableName = "content_seasons",
    foreignKeys = [
        ForeignKey(
            entity = LibraryContentEntity::class,
            parentColumns = ["id"],
            childColumns = ["contentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["contentId"]), Index(value = ["contentId", "number"], unique = true)],
)
data class ContentSeasonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentId: Long,
    val number: Int,
    val title: String,
    val currentEpisodes: Int,
    val totalEpisodes: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "content_notes",
    foreignKeys = [
        ForeignKey(
            entity = LibraryContentEntity::class,
            parentColumns = ["id"],
            childColumns = ["contentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["contentId"])],
)
data class ContentNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentId: Long,
    val text: String,
    val createdAt: Long,
)

data class ContentWithSeasons(
    @Embedded val content: LibraryContentEntity,
    @Relation(parentColumn = "id", entityColumn = "contentId")
    val seasons: List<ContentSeasonEntity>,
)
