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
    private val trackId: Long = _trackId?.toLong()?:-1
    private var progressJob: Job? = null
    private var tracks: List<Track> = emptyList()
    private var currentTrackIndex = -1

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
            when (state) {
                Player.STATE_READY -> {
                    _playbackState.value = _playbackState.value.copy(
                        duration = player.duration,
                        currentPosition = player.currentPosition
                    )
                    updateProgress()
                }
                Player.STATE_ENDED -> {
                    stopProgressUpdate()
                    _playbackState.value = _playbackState.value.copy(
                        progress = 1f,
                        currentPosition = player.duration
                    )
                    playNextTrack()
                }
            }
        }
    }

    init {
        player.addListener(playerListener)
        loadTracks()
    }

    private fun loadTracks() {
        viewModelScope.launch {
            when (val result = repository.getChartTracks()) {
                is kv.compose.musicplayer.domain.util.Result.Success -> {
                    tracks = result.data
                    currentTrackIndex = tracks.indexOfFirst { it.id == trackId }
                    if (currentTrackIndex != -1) {
                        _uiState.value = PlayerUiState.Success(tracks[currentTrackIndex])
                        prepareAndPlayTrack(tracks[currentTrackIndex])
                    }
                }
                is kv.compose.musicplayer.domain.util.Result.Error -> _uiState.value = PlayerUiState.Error(result.message)
                is kv.compose.musicplayer.domain.util.Result.Loading -> _uiState.value = PlayerUiState.Loading
            }
        }
    }

    private fun prepareAndPlayTrack(track: Track) {
        _uiState.value = PlayerUiState.Success(track)
        val mediaItem = MediaItem.Builder()
            .setUri(track.preview)
            .setMediaId(track.id.toString())
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
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
                progress = player.currentPosition.toFloat() / player.duration,
                currentPosition = player.currentPosition,
                duration = player.duration
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

    fun playNextTrack() {
        if (currentTrackIndex < tracks.size - 1) {
            currentTrackIndex++
            prepareAndPlayTrack(tracks[currentTrackIndex])
        }
    }

    fun playPreviousTrack() {
        if (currentTrackIndex > 0) {
            currentTrackIndex--
            prepareAndPlayTrack(tracks[currentTrackIndex])
        }
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
    val currentPosition: Long = 0L,
    val duration: Long = 0L
)