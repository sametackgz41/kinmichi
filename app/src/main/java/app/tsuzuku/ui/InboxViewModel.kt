package app.tsuzuku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.tsuzuku.data.InboxRepository
import app.tsuzuku.model.InboxItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(private val inboxRepository: InboxRepository) : ViewModel() {

    val items: StateFlow<List<InboxItem>> = inboxRepository.observeOpen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun capture(text: String) {
        viewModelScope.launch {
            inboxRepository.capture(text)
        }
    }

    fun dismiss(id: Long) {
        viewModelScope.launch {
            inboxRepository.dismiss(id)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            inboxRepository.delete(id)
        }
    }

    fun markConverted(id: Long) {
        viewModelScope.launch {
            inboxRepository.markConverted(id)
        }
    }

    companion object {
        fun factory(inboxRepository: InboxRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    InboxViewModel(inboxRepository) as T
            }
    }
}
