package kv.compose.musicplayer.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kv.compose.musicplayer.data.model.Track
import kv.compose.musicplayer.domain.repository.MusicRepository
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val player: Player,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _trackId: String? = savedStateHandle["trackId"]
    private val trackId: Long = checkNotNull(_trackId?.toLong())
    private var progressJob: Job? = null

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
            if (isPlaying) {
                startProgressUpdate()
            } else {
                stopProgressUpdate()
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                _playbackState.value = _playbackState.value.copy(
                    duration = player.duration
                )
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updateProgress()
        }
    }

    init {
        player.addListener(playerListener)
        loadTrack()
    }

    private fun loadTrack() {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            when (val result = repository.getTrack(trackId)) {
                is kv.compose.musicplayer.domain.util.Result.Success -> {
                    _uiState.value = PlayerUiState.Success(result.data)

                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(result.data.preview))
                        .setMediaId(result.data.id.toString())
                        .build()

                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
                is kv.compose.musicplayer.domain.util.Result.Error -> _uiState.value = PlayerUiState.Error(result.message)
                is kv.compose.musicplayer.domain.util.Result.Loading -> _uiState.value = PlayerUiState.Loading
            }
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                updateProgress()
                delay(16) // Update approximately 60 times per second
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateProgress() {
        if (player.duration > 0) {
            _playbackState.value = _playbackState.value.copy(
                progress = player.currentPosition.toFloat() / player.duration
            )
        }
    }

    fun onPlayPauseClick() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun onSeekTo(position: Float) {
        player.seekTo((position * player.duration).toLong())
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressUpdate()
        player.removeListener(playerListener)
    }
}

sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Success(val track: Track) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val duration: Long = 0L
)