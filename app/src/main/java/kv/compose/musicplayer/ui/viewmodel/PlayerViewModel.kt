package com.example.musicplayer.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kv.compose.musicplayer.data.model.Track
import kv.compose.musicplayer.data.repository.TrackListRepository
import kv.compose.musicplayer.service.MusicService
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: TrackListRepository,
    private val player: Player,
    private val application: Application
) : AndroidViewModel(application) {
    private var progressJob: Job? = null

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
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

        Log.i("PlayerViewModelLogs", "init: ${repository.getCurrentTrack()}")
        player.addListener(playerListener)
        prepareAndPlayTrack(repository.getCurrentTrack())
    }



    private fun prepareAndPlayTrack(track: Track) {

        Log.i("TAG", "prepareAndPlayTrack: $track")
        _uiState.value = PlayerUiState.Success(track)

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist.name)
            .setAlbumTitle(track.album.title)
            .setArtworkUri(if (track.album.cover.isNotEmpty()) Uri.parse(track.album.cover) else null)
            .build()



        val mediaItem = MediaItem.Builder()
            .setUri(track.preview)
            .setMediaId(track.id.toString())
            .setMediaMetadata(mediaMetadata)
            .build()

        startMusicService()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun startMusicService() {
        val intent = Intent(application, MusicService::class.java).apply {
            action = "START_SERVICE"
        }
        ContextCompat.startForegroundService(application, intent)
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
        prepareAndPlayTrack(repository.nextTrack())
    }

    fun playPreviousTrack() {
        prepareAndPlayTrack(repository.prevTrack())
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressUpdate()
        player.removeListener(playerListener)
    }
}

sealed class PlayerUiState {
    data object Loading : PlayerUiState()
    data class Success(val track: Track) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val currentPosition: Long = 0L,
    val duration: Long = 0L
)