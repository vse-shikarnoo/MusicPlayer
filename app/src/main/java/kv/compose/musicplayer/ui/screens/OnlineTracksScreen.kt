package kv.compose.musicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kv.compose.musicplayer.R
import kv.compose.musicplayer.data.model.Album
import kv.compose.musicplayer.data.model.Artist
import kv.compose.musicplayer.data.model.Track
import kv.compose.musicplayer.ui.navigation.Screen
import kv.compose.musicplayer.ui.viewmodel.OnlineTracksUiState
import kv.compose.musicplayer.ui.viewmodel.OnlineTracksViewModel

@Preview
@Composable
private fun OnlineTracksScreenPreview() {
    OnlineTracksScreen(rememberNavController())
}

@Composable
fun OnlineTracksScreen(
    navController: NavController,
    viewModel: OnlineTracksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()) {
        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        when (val state = uiState) {
            is OnlineTracksUiState.Loading -> LoadingIndicator()
            is OnlineTracksUiState.Success -> TracksList(
                tracks = state.tracks,
                onTrackClick = { track ->
                    navController.navigate(Screen.Player.route + "/${track.id}")
                }
            )

            is OnlineTracksUiState.Error -> ErrorMessage(state.message) {
                viewModel.onSearchQueryChange(query = searchQuery)
            }
        }
    }
}


@Preview
@Composable
private fun SearchBarPreview() {
    SearchBar(
        query = "",
        onQueryChange = {

        }
    )
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search tracks...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },

        )
}


@Preview
@Composable
private fun LoadingIndicatorPreview() {
    LoadingIndicator()
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Preview
@Composable
private fun TracksListPreview() {
    TracksList(
        tracks = List(20) { testTrack }
    ) { }
}

@Composable
private fun TracksList(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(tracks) { track ->
            TrackItem(track = track, onClick = { onTrackClick(track) })
        }
    }
}

@Preview
@Composable
private fun TrackItemPreview() {
    TrackItem(
        testTrack
    ) { }
}

@Composable
private fun TrackItem(
    track: Track,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(
                    androidx.compose.ui.platform.LocalContext.current
                )
                    .data(track.album.cover)
                    .build(),
                contentDescription = "Album cover",
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.baseline_music_note_24),
                error = painterResource(R.drawable.baseline_music_note_24)
            )

            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = track.artist.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
private fun ErrorMessagePreview() {
    ErrorMessage(
        "Error"
    ) {}
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
        Button(
            modifier = Modifier.padding(top = 16.dp),
            onClick = { onRetry() }
        ) {
            Text(text = "Retry")
        }
    }
}

private val testTrack = Track(
    id = 1,
    title = "Test",
    artist = Artist(
        id = 1,
        name = "Test"
    ),
    album = Album(
        id = 1,
        title = "Test",
        cover = "Test",
        coverSmall = "Test",
        coverMedium = "Test",
        coverBig = "Test"
    ),
    preview = "Test",
    duration = 100,
)