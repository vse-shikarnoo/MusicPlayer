package kv.compose.musicplayer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kv.compose.musicplayer.R
import kv.compose.musicplayer.ui.screens.OnlineTracksScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(painterResource(R.drawable.baseline_music_note_24), contentDescription = "Online Tracks") },
                        label = { Text("Online") },
                        selected = currentRoute == Screen.OnlineTracks.route,
                        onClick = {
                            navController.navigate(Screen.OnlineTracks.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Local Tracks") },
                        label = { Text("Local") },
                        selected = currentRoute == Screen.LocalTracks.route,
                        onClick = {
                            navController.navigate(Screen.LocalTracks.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(
                navController = navController,
                startDestination = Screen.OnlineTracks.route
            ) {
                composable(Screen.OnlineTracks.route) {
                    OnlineTracksScreen(navController)
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    data object OnlineTracks : Screen("online_tracks")
    data object LocalTracks : Screen("local_tracks")
    data object Player : Screen("player")
}