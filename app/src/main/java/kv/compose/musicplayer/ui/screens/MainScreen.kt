package kv.compose.musicplayer.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kv.compose.musicplayer.ui.navigation.AppNavigation
import kv.compose.musicplayer.ui.navigation.BottomBar
import kv.compose.musicplayer.ui.navigation.Screen
import kv.compose.musicplayer.ui.navigation.bottomBarRoutes

@Composable
fun MainScreen(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes.map { it.route }) {
                BottomBar(navController, currentRoute ?: Screen.OnlineTracks.route)
            }
        }
    ) {
        AppNavigation(
            navController, Modifier
                .padding(it)
                .padding(vertical = 32.dp)
        )
    }
}