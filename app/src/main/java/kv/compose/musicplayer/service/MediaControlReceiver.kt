package kv.compose.musicplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.media3.common.Player
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MediaControlReceiver : BroadcastReceiver() {

    @Inject
    lateinit var player: Player

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MusicService.ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            }
            MusicService.ACTION_NEXT -> {
                player.seekToNext()
            }
            MusicService.ACTION_PREVIOUS -> {
                player.seekToPrevious()
            }
            MusicService.ACTION_STOP -> {
                player.stop()
                context.stopService(Intent(context, MusicService::class.java))
            }
        }
    }
}