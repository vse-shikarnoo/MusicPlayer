package kv.compose.musicplayer.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kv.compose.musicplayer.R
import kv.compose.musicplayer.ui.screens.LocalTracksScreen
import kv.compose.musicplayer.ui.screens.OnlineTracksScreen
import kv.compose.musicplayer.ui.screens.PlayerScreen


@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.OnlineTracks.route
    ) {
        composable(Screen.OnlineTracks.route) {
            OnlineTracksScreen(navController)
        }
        composable(Screen.LocalTracks.route) {
            LocalTracksScreen(navController)
        }
        composable(Screen.Player.route + "/{trackId}") {
            PlayerScreen(navController)
        }
    }
}

val bottomBarRoutes = setOf(
    Screen.OnlineTracks,
    Screen.LocalTracks
)

sealed class Screen(
    val route: String,
    val label: String = "",
    @DrawableRes val icon: Int = R.drawable.baseline_music_note_24
) {
    data object OnlineTracks : Screen("online_tracks", "Online")
    data object LocalTracks : Screen("local_tracks", "Local", R.drawable.baseline_audio_file_24)
    data object Player : Screen("player")
}