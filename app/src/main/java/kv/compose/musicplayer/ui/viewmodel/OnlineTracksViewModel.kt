package kv.compose.musicplayer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kv.compose.musicplayer.data.model.Track
import kv.compose.musicplayer.domain.repository.MusicRepository
import kv.compose.musicplayer.domain.util.Result
import javax.inject.Inject

@HiltViewModel
class OnlineTracksViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnlineTracksUiState>(OnlineTracksUiState.Loading)
    val uiState: StateFlow<OnlineTracksUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadChartTracks()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            loadChartTracks()
        } else {
            searchTracks(query)
        }
    }

    private fun loadChartTracks() {
        viewModelScope.launch {
            try {
                val tracks = repository.getChartTracks() as Result.Success
                _uiState.value = OnlineTracksUiState.Success(tracks.data)
            } catch (e: Exception) {
                _uiState.value = OnlineTracksUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun searchTracks(query: String) {
        viewModelScope.launch {
            try {
                _uiState.value = OnlineTracksUiState.Loading
                val tracks = repository.searchTracks(query) as Result.Success
                _uiState.value = OnlineTracksUiState.Success(tracks.data)
            } catch (e: Exception) {
                _uiState.value = OnlineTracksUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class OnlineTracksUiState {
    data object Loading : OnlineTracksUiState()
    data class Success(val tracks: List<Track>) : OnlineTracksUiState()
    data class Error(val message: String) : OnlineTracksUiState()
}