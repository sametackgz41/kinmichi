package app.tsuzuku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.tsuzuku.data.LibraryRepository
import app.tsuzuku.model.ContentDraft
import app.tsuzuku.model.ContentStatus
import app.tsuzuku.model.LibraryDetail
import app.tsuzuku.model.LibraryItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository,
) : ViewModel() {
    val items: StateFlow<List<LibraryItem>> = repository.observeLibrary().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun detail(id: Long): Flow<LibraryDetail?> = repository.observeDetail(id)

    fun saveContent(id: Long?, draft: ContentDraft, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            if (id == null) {
                onSaved(repository.addContent(draft))
            } else {
                repository.updateContent(id, draft)
                onSaved(id)
            }
        }
    }

    fun changeProgress(id: Long, delta: Double) {
        viewModelScope.launch { repository.changeProgress(id, delta) }
    }

    fun updateStatus(id: Long, status: ContentStatus) {
        viewModelScope.launch { repository.updateStatus(id, status) }
    }

    fun saveGeneralNote(id: Long, note: String) {
        viewModelScope.launch { repository.updateGeneralNote(id, note) }
    }

    fun addShortNote(id: Long, note: String) {
        viewModelScope.launch { repository.addShortNote(id, note) }
    }

    fun deleteShortNote(noteId: Long) {
        viewModelScope.launch { repository.deleteShortNote(noteId) }
    }

    fun addSeason(contentId: Long, title: String, totalEpisodes: Int) {
        viewModelScope.launch { repository.addSeason(contentId, title, totalEpisodes) }
    }

    fun changeSeasonProgress(seasonId: Long, delta: Int) {
        viewModelScope.launch { repository.changeSeasonProgress(seasonId, delta) }
    }

    fun deleteContent(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteContent(id)
            onDone()
        }
    }

    val collections: StateFlow<List<app.tsuzuku.model.LibraryCollection>> = repository.observeCollections().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val mutableSelectedCollectionId = MutableStateFlow<Long?>(null)
    val selectedCollectionId: StateFlow<Long?> = mutableSelectedCollectionId

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedCollectionContentIds: StateFlow<List<Long>> = mutableSelectedCollectionId.flatMapLatest { colId ->
        if (colId == null) flowOf(emptyList()) else repository.observeCollectionContentIds(colId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectCollection(collectionId: Long?) {
        mutableSelectedCollectionId.value = collectionId
    }

    fun createCollection(name: String, emoji: String) {
        viewModelScope.launch { repository.createCollection(name, emoji) }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch {
            if (mutableSelectedCollectionId.value == id) mutableSelectedCollectionId.value = null
            repository.deleteCollection(id)
        }
    }

    fun addContentToCollection(collectionId: Long, contentId: Long) {
        viewModelScope.launch { repository.addContentToCollection(collectionId, contentId) }
    }

    fun removeContentFromCollection(collectionId: Long, contentId: Long) {
        viewModelScope.launch { repository.removeContentFromCollection(collectionId, contentId) }
    }

    fun updateReview(
        id: Long,
        rating: Float?,
        isFavorite: Boolean,
        reviewNote: String,
        startDate: Long?,
        finishDate: Long?,
        rewatch: Boolean,
    ) {
        viewModelScope.launch {
            repository.updateReview(id, rating, isFavorite, reviewNote, startDate, finishDate, rewatch)
        }
    }

    fun moveContent(id: Long, direction: Int) {
        viewModelScope.launch { repository.moveContent(id, direction) }
    }

    companion object {
        fun factory(repository: LibraryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LibraryViewModel(repository) as T
            }
    }
}
