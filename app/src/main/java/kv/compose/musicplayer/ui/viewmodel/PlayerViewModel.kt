package kv.compose.musicplayer.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
class PlayerViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val player: ExoPlayer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _trackId: String? = savedStateHandle["trackId"]

    private val trackId: Long = _trackId?.toLong() ?: -1

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
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

    fun loadTrack() {
        viewModelScope.launch {
            try {

                if (trackId == -1L) throw Exception("TrackID = -1")

                when (val res = repository.getTrack(trackId)) {
                    is Result.Error -> _uiState.value = PlayerUiState.Error(res.message)
                    Result.Loading -> {
                        _uiState.value = PlayerUiState.Loading
                    }

                    is Result.Success -> {
                        _uiState.value = PlayerUiState.Success(res.data)

                        val mediaItem = MediaItem.Builder()
                            .setUri(res.data.preview)
                            .setMediaId(res.data.id.toString())
                            .build()

                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.play()
                    }
                }

            } catch (e: Exception) {
                _uiState.value = PlayerUiState.Error(e.message ?: "Unknown error")
            }
        }
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