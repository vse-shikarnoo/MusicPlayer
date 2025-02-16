package kv.compose.musicplayer.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kv.compose.musicplayer.R
import kv.compose.musicplayer.data.model.Track
import kotlin.time.Duration.Companion.milliseconds

@Preview
@Composable
fun PlayerScreenPreview() {
    PlayerScreen(
    )
}


@Composable
fun PlayerScreen(
) {

}


@Composable
fun PlayerContent(
    track: Track,
    isPlaying: Boolean,
    progress: Float,
    currentPosition: Long,
    duration: Long,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekTo: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Album Art
        AsyncImage(
            model = track.album.cover,
            contentDescription = "Album cover",
            modifier = Modifier
                .size(300.dp)
                .padding(bottom = 32.dp),
            contentScale = ContentScale.Crop
        )

        // Track Info
        Text(
            text = track.title,
            style = MaterialTheme.typography.h2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp),
            maxLines = 1
        )
        Text(
            text = track.artist.name,
            style = MaterialTheme.typography.h4,
            color = MaterialTheme.colors.onSurface,
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
                text = formatDuration(currentPosition),
                style = MaterialTheme.typography.body1
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.body1
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
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier.size(48.dp)
            ) {
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
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(48.dp)
            ) {
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