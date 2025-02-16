package kv.compose.musicplayer.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kv.compose.musicplayer.ui.common.ErrorMessage
import kv.compose.musicplayer.ui.common.LoadingIndicator
import kv.compose.musicplayer.ui.common.SearchBar
import kv.compose.musicplayer.ui.common.TracksList
import kv.compose.musicplayer.ui.viewmodel.OnlineTracksUiState
import kv.compose.musicplayer.ui.viewmodel.OnlineTracksViewModel

@Preview
@Composable
private fun OnlineTracksScreenPreview() {
    OnlineTracksScreen(navController = rememberNavController(), expandPlayer = {})
}

@Composable
fun OnlineTracksScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    expandPlayer: ()->Unit,
    viewModel: OnlineTracksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(modifier = modifier) {
        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 16.dp)
        )

        when (val state = uiState) {
            is OnlineTracksUiState.Loading -> LoadingIndicator()
            is OnlineTracksUiState.Success -> TracksList(
                tracks = state.tracks,
                onTrackClick = { track ->
                    viewModel.setCurrentTrack(track.id)
                    Log.d("OnlineTracksScreenLogs", "OnlineTracksScreen: ${track.id}")
                    expandPlayer()
                }
            )

            is OnlineTracksUiState.Error -> ErrorMessage(state.message){
                viewModel.onSearchQueryChange(searchQuery)
            }
        }
    }
}
