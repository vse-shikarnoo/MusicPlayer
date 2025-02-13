package kv.compose.musicplayer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kv.compose.musicplayer.data.model.Track
import kv.compose.musicplayer.domain.repository.MusicRepository
import kv.compose.musicplayer.domain.util.Result
import javax.inject.Inject

@HiltViewModel
class LocalTracksViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<LocalTracksUiState>(LocalTracksUiState.Loading)
    val uiState: StateFlow<LocalTracksUiState> = _uiState.asStateFlow()

    init {
        loadLocalTracks()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            loadLocalTracks()
        } else {
            searchLocalTracks(query)
        }
    }

    private fun loadLocalTracks() {
        viewModelScope.launch {
            try {
                repository.getLocalTracks()
                    .onStart { _uiState.value = LocalTracksUiState.Loading }
                    .catch { e -> _uiState.value = LocalTracksUiState.Error(e.message ?: "Unknown error") }
                    .collect { tracks ->
                        when (tracks) {
                            is Result.Error -> _uiState.value = LocalTracksUiState.Error(tracks.message)
                            Result.Loading -> {
                                _uiState.value = LocalTracksUiState.Loading
                            }

                            is Result.Success ->
                                _uiState.value = LocalTracksUiState.Success(tracks.data)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = LocalTracksUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun searchLocalTracks(query: String) {
        viewModelScope.launch {
            try {
                _uiState.value = LocalTracksUiState.Loading
                when (val res = repository.searchLocalTracks(query)) {
                    is Result.Error -> _uiState.value = LocalTracksUiState.Error(res.message)
                    Result.Loading -> {
                        _uiState.value = LocalTracksUiState.Loading
                    }

                    is Result.Success ->
                        _uiState.value = LocalTracksUiState.Success(res.data)
                }
            } catch (e: Exception) {
                _uiState.value = LocalTracksUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class LocalTracksUiState {
    object Loading : LocalTracksUiState()
    data class Success(val tracks: List<Track>) : LocalTracksUiState()
    data class Error(val message: String) : LocalTracksUiState()
}