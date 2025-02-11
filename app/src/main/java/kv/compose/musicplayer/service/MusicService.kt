package kv.compose.musicplayer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kv.compose.musicplayer.MainActivity
import kv.compose.musicplayer.R
import javax.inject.Inject

@AndroidEntryPoint
class MusicService: MediaSessionService() {
    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var imageLoader: ImageLoader

    private var mediaSession: MediaSession? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var currentNotificationId = NOTIFICATION_ID

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { updateNotification(it) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(getMainActivityPendingIntent())
            .build()

        player.addListener(playerListener)
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
            mediaSession = null
        }
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updatePlaybackState() {
        val mediaItem = player.currentMediaItem ?: return
        updateNotification(mediaItem)
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(mediaItem: MediaItem) {
        val title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown"
        val artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown"
        val artworkUri = mediaItem.mediaMetadata.artworkUri

        serviceScope.launch {
            val artwork = if (artworkUri != null) {
                try {
                    val request = ImageRequest.Builder(this@MusicService)
                        .data(artworkUri)
                        .build()
                    val result = imageLoader.execute(request)
                    (result as? SuccessResult)?.drawable?.toBitmap()
                } catch (e: Exception) {
                    null
                }
            } else null

            val notification = buildNotification(title, artist, artwork)
            NotificationManagerCompat.from(this@MusicService).apply {
                notify(currentNotificationId, notification)
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun buildNotification(
        title: String,
        artist: String,
        artwork: Bitmap?
    ): Notification {
        val playPauseIcon = if (player.isPlaying) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(artwork)
            .setContentIntent(getMainActivityPendingIntent())
            .setDeleteIntent(getStopIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_skip_previous,
                "Previous",
                getPreviousIntent()
            )
            .addAction(
                playPauseIcon,
                "Play/Pause",
                getPlayPauseIntent()
            )
            .addAction(
                R.drawable.ic_skip_next,
                "Next",
                getNextIntent()
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionCompatToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        return builder.build()
    }

    private fun getMainActivityPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun getPlayPauseIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_IMMUTABLE
        )

    private fun getPreviousIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_PREVIOUS),
            PendingIntent.FLAG_IMMUTABLE
        )

    private fun getNextIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_NEXT),
            PendingIntent.FLAG_IMMUTABLE
        )

    private fun getStopIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        private const val CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_PLAY_PAUSE = "kv.compose.musicplayer.PLAY_PAUSE"
        const val ACTION_NEXT = "kv.compose.musicplayer.NEXT"
        const val ACTION_PREVIOUS = "kv.compose.musicplayer.PREVIOUS"
        const val ACTION_STOP = "kv.compose.musicplayer.STOP"
    }
}