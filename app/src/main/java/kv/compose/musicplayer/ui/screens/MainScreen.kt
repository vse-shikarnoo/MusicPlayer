package kv.compose.musicplayer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetValue.Collapsed
import androidx.compose.material.BottomSheetValue.Expanded
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.musicplayer.ui.viewmodel.PlayerUiState
import com.example.musicplayer.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import kv.compose.musicplayer.R
import kv.compose.musicplayer.ui.common.ErrorMessage
import kv.compose.musicplayer.ui.common.LoadingIndicator
import kv.compose.musicplayer.ui.common.PlayerContent
import kv.compose.musicplayer.ui.navigation.AppNavigation
import kv.compose.musicplayer.ui.navigation.BottomBar
import kv.compose.musicplayer.ui.navigation.Screen

@Composable
fun MainScreen(
    navController: NavHostController,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    val navBarVisibility = remember { mutableStateOf(true) }


    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = androidx.compose.material.rememberBottomSheetScaffoldState(
    )

    var bspadding = remember { mutableStateOf(0.dp) }





    androidx.compose.material.Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = navBarVisibility.value,
                enter = fadeIn()
            ) {
                BottomBar(navController, currentRoute ?: Screen.OnlineTracks.route)
            }
        }
    ) { padding ->


        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = bspadding.value,
            sheetContent = {


                when (scaffoldState.bottomSheetState.currentValue) {
                    Collapsed -> {
                        navBarVisibility.value = true
                        when (val state = uiState) {
                            is PlayerUiState.Success -> {
                                Box(
                                    modifier = Modifier
                                        .height(bspadding.value)
                                        .clickable {
                                            coroutineScope.launch {
                                                scaffoldState.bottomSheetState.expand()
                                            }
                                        }
                                ) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = state.track.title,
                                            style = MaterialTheme.typography.body1,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = viewModel::onPlayPauseClick) {
                                            Icon(
                                                if (playbackState.isPlaying) painterResource(R.drawable.ic_pause) else painterResource(
                                                    R.drawable.ic_play
                                                ),
                                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                                modifier = Modifier.size(48.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            else -> {}
                        }


                    }

                    Expanded -> {
                        navBarVisibility.value = false
                        when (val state = uiState) {
                            is PlayerUiState.Loading -> LoadingIndicator()
                            is PlayerUiState.Success -> {
                                PlayerContent(
                                    track = state.track,
                                    isPlaying = playbackState.isPlaying,
                                    progress = playbackState.progress,
                                    currentPosition = playbackState.currentPosition,
                                    duration = playbackState.duration,
                                    onPlayPauseClick = viewModel::onPlayPauseClick,
                                    onPreviousClick = viewModel::playPreviousTrack,
                                    onNextClick = viewModel::playNextTrack,
                                    onSeekTo = viewModel::onSeekTo,
                                    modifier = Modifier.padding(padding)
                                )
                            }

                            is PlayerUiState.Error -> ErrorMessage(state.message) {

                            }

                            else -> {}
                        }
                    }
                }

            }) {


            AppNavigation(
                navController, Modifier
                    .padding(
                        bottom = if (bspadding.value == 0.dp) {
                            0.dp
                        } else {
                            64.dp
                        }
                    )
                    .padding(padding),
                expandPlayer = {
                    coroutineScope.launch {
                        scaffoldState.bottomSheetState.expand()
                        viewModel.prepareAndPlayTrack()
                        if (bspadding.value == 0.dp) {
                            bspadding.value = 200.dp
                        }
                    }
                }
            )
        }
    }

}