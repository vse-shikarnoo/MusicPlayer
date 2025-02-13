package kv.compose.musicplayer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kv.compose.musicplayer.MainActivity
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var notificationManager: PlayerNotificationManager

    private var mediaSession: MediaSession? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var isForegroundService = false
    private lateinit var mediaControlReceiver: MediaControlReceiver

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { updateNotification(it) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startForegroundService()
            }
            updatePlaybackState()
        }

        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_ENDED -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    isForegroundService = false
                }
                Player.STATE_READY -> {
                    startForegroundService()
                    updatePlaybackState()
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

        notificationManager.setMediaSession(mediaSession!!)
        player.addListener(playerListener)

        // Register broadcast receiver
        mediaControlReceiver = MediaControlReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREVIOUS)
            addAction(ACTION_STOP)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                mediaControlReceiver,
                intentFilter,
                RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(mediaControlReceiver, intentFilter)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForegroundService()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(mediaControlReceiver)
        } catch (e: Exception) {
            // Ignore if receiver is not registered
        }

        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
            mediaSession = null
        }
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun startForegroundService() {
        if (!isForegroundService) {
            val notification = createEmptyNotification()
            startForeground(NOTIFICATION_ID, notification)
            isForegroundService = true
        }
    }

    private fun updatePlaybackState() {
        val mediaItem = player.currentMediaItem ?: return
        updateNotification(mediaItem)
    }

    private fun updateNotification(mediaItem: MediaItem) {
        val title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown"
        val artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown"
        val artworkUrl = mediaItem.mediaMetadata.artworkUri?.toString()

        val notification = notificationManager.buildNotification(
            title = title,
            artist = artist,
            isPlaying = player.isPlaying,
            artworkUrl = artworkUrl,
            scope = serviceScope
        )

        notificationManager.updateNotification(notification)
    }

    private fun createEmptyNotification(): Notification {
        return notificationManager.buildNotification(
            title = "Loading...",
            artist = "",
            isPlaying = player.isPlaying,
            artworkUrl = null,
            scope = serviceScope
        )
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY_PAUSE = "kv.compose.musicplayer.PLAY_PAUSE"
        const val ACTION_NEXT = "kv.compose.musicplayer.NEXT"
        const val ACTION_PREVIOUS = "kv.compose.musicplayer.PREVIOUS"
        const val ACTION_STOP = "kv.compose.musicplayer.STOP"
    }
}