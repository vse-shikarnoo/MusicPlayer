package kv.compose.musicplayer.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kv.compose.musicplayer.data.model.Track

@Composable
fun TracksList(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit
) {
    if (tracks.isEmpty()) {
        EmptyState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(tracks) { track ->
                TrackItem(track = track, onClick = { onTrackClick(track) })
            }
        }
    }
}