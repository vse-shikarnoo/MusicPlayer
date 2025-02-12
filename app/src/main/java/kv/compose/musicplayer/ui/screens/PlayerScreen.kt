package kv.compose.musicplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import kv.compose.musicplayer.R
import kv.compose.musicplayer.data.model.Track
import kv.compose.musicplayer.ui.viewmodel.PlayerUiState
import kv.compose.musicplayer.ui.viewmodel.PlayerViewModel
import kotlin.time.Duration.Companion.milliseconds

@Preview
@Composable
fun PlayerScreenPreview() {
    PlayerScreen(
        rememberNavController()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    navController: NavController,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is PlayerUiState.Loading -> LoadingIndicator()
                is PlayerUiState.Success -> PlayerContent(
                    track = state.track,
                    isPlaying = playbackState.isPlaying,
                    progress = playbackState.progress,
                    onPlayPauseClick = viewModel::onPlayPauseClick,
                    onSeekTo = viewModel::onSeekTo
                )

                is PlayerUiState.Error -> ErrorMessage(state.message) {

                }
            }
        }
    }
}

@Composable
private fun PlayerContent(
    track: Track,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Album Art
        AsyncImage(
            model = track.album.coverBig,
            contentDescription = "Album cover",
            modifier = Modifier
                .size(300.dp)
                .padding(bottom = 32.dp),
            contentScale = ContentScale.Crop
        )

        // Track Info
        Text(
            text = track.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = track.artist.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Progress
        Slider(
            value = progress,
            onValueChange = onSeekTo,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        // Time indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration((progress * track.duration * 1000).toLong()),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatDuration(track.duration * 1000L),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Playback Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Previous track */ }) {
                Icon(
                    painterResource(R.drawable.ic_skip_previous),
                    contentDescription = "Previous track",
                    modifier = Modifier.size(48.dp)
                )
            }
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    if (isPlaying) painterResource(R.drawable.ic_pause) else painterResource(R.drawable.ic_play),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(64.dp)
                )
            }
            IconButton(onClick = { /* Next track */ }) {
                Icon(
                    painterResource(R.drawable.ic_skip_next),
                    contentDescription = "Next track",
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun formatDuration(millis: Long): String {
    val duration = millis.milliseconds
    val minutes = duration.inWholeMinutes
    val seconds = duration.inWholeSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}