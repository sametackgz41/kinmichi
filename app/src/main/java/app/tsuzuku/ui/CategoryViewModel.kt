package app.tsuzuku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.tsuzuku.data.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(private val repository: CategoryRepository) : ViewModel() {
    val categories = repository.observeAll().flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun create(name: String, emoji: String, color: Long) = viewModelScope.launch { repository.create(name, emoji, color) }
    fun delete(item: app.tsuzuku.data.local.CategoryEntity) = viewModelScope.launch { repository.delete(item) }

    companion object {
        fun factory(repository: CategoryRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CategoryViewModel(repository) as T
        }
    }
}
