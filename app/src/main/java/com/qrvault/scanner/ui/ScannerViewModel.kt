package com.qrvault.scanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qrvault.scanner.data.local.ScanHistoryEntity
import com.qrvault.scanner.data.repository.ScanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModel(private val repository: ScanRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedType = MutableStateFlow<String?>(null)
    val selectedType: StateFlow<String?> = _selectedType.asStateFlow()

    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites: StateFlow<Boolean> = _onlyFavorites.asStateFlow()

    private val _activeScan = MutableStateFlow<ScanHistoryEntity?>(null)
    val activeScan: StateFlow<ScanHistoryEntity?> = _activeScan.asStateFlow()

    val history: StateFlow<List<ScanHistoryEntity>> = combine(
        _searchQuery,
        _selectedType,
        _onlyFavorites
    ) { query, type, favorites -> Triple(query, type, favorites) }
        .flatMapLatest { (query, type, favorites) ->
            repository.observeHistory(query, type, favorites)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQrDecoded(rawValue: String) {
        viewModelScope.launch {
            val saved = repository.saveDecodedQr(rawValue)
            _activeScan.value = saved
        }
    }

    fun selectScan(entity: ScanHistoryEntity?) {
        _activeScan.value = entity
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun updateTypeFilter(type: String?) {
        _selectedType.value = type
    }

    fun toggleFavoritesFilter() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun toggleFavorite(entity: ScanHistoryEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(entity)
        }
    }

    fun saveNote(id: Long, note: String) {
        viewModelScope.launch {
            repository.updateNote(id, note)
        }
    }

    fun deleteScan(entity: ScanHistoryEntity) {
        viewModelScope.launch {
            repository.deleteScan(entity)
            if (_activeScan.value?.id == entity.id) {
                _activeScan.value = null
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _activeScan.value = null
            Timber.i("User cleared scan history")
        }
    }

    class Factory(private val repository: ScanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScannerViewModel(repository) as T
        }
    }
}
