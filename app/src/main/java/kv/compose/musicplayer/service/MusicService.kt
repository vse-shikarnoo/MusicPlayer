package kv.compose.musicplayer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kv.compose.musicplayer.MainActivity
import kv.compose.musicplayer.R
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var imageLoader: ImageLoader

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
        Log.d("MusicService", "onCreate called")

        // Создаем канал уведомлений до создания MediaSession
        createNotificationChannel()

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
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(mediaControlReceiver, intentFilter)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MusicService", "onStartCommand called")
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
            Log.e("MusicService", "Error unregistering receiver", e)
        }

        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
            mediaSession = null
        }
        serviceJob.cancel()
        stopForeground(true)
        super.onDestroy()
    }

    private fun startForegroundService() {
        if (!isForegroundService) {
            Log.d("MusicService", "Starting foreground service")
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

        val notification = buildNotification(
            title = title,
            artist = artist,
            isPlaying = player.isPlaying,
            artworkUrl = artworkUrl,
            scope = serviceScope
        )

        updateNotification(notification)
    }

    private fun createEmptyNotification(): Notification {
        return buildNotification(
            title = "Loading...",
            artist = "",
            isPlaying = player.isPlaying,
            artworkUrl = null,
            scope = serviceScope
        )
    }

    private fun createNotificationChannel() {
        Log.d("MusicService", "Creating notification channel")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d("MusicService", "Notification channel created")
        }
    }

    @OptIn(UnstableApi::class)
    private fun buildNotification(
        title: String,
        artist: String,
        isPlaying: Boolean,
        artworkUrl: String?,
        scope: CoroutineScope
    ): Notification {
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("Now Playing")
            .setOngoing(isPlaying)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    mainIntent,
                    pendingIntentFlags
                )
            )
            .setDeleteIntent(
                PendingIntent.getBroadcast(
                    this,
                    0,
                    Intent(ACTION_STOP).setPackage(packageName),
                    pendingIntentFlags
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setShowWhen(false)
            .addAction(
                R.drawable.ic_skip_previous,
                "Previous",
                PendingIntent.getBroadcast(
                    this,
                    1,
                    Intent(ACTION_PREVIOUS).setPackage(packageName),
                    pendingIntentFlags
                )
            )
            .addAction(
                playPauseIcon,
                if (isPlaying) "Pause" else "Play",
                PendingIntent.getBroadcast(
                    this,
                    2,
                    Intent(ACTION_PLAY_PAUSE).setPackage(packageName),
                    pendingIntentFlags
                )
            )
            .addAction(
                R.drawable.ic_skip_next,
                "Next",
                PendingIntent.getBroadcast(
                    this,
                    3,
                    Intent(ACTION_NEXT).setPackage(packageName),
                    pendingIntentFlags
                )
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession?.sessionCompatToken)
            )

        if (!artworkUrl.isNullOrEmpty()) {
            scope.launch {
                try {
                    val request = ImageRequest.Builder(this@MusicService)
                        .data(artworkUrl)
                        .size(256, 256)
                        .crossfade(true)
                        .build()
                    val result = imageLoader.execute(request)
                    val bitmap = result.drawable?.toBitmap()
                    bitmap?.let {
                        builder.setLargeIcon(it)
                        updateNotification(builder.build())
                    }
                } catch (e: Exception) {
                    Log.e("MusicService", "Error loading artwork", e)
                }
            }
        }

        return builder.build()
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(notification: Notification) {
        try {
            NotificationManagerCompat.from(this)
                .notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("MusicService", "Error updating notification", e)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_playback_channel"
        const val ACTION_PLAY_PAUSE = "com.example.musicplayer.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.musicplayer.NEXT"
        const val ACTION_PREVIOUS = "com.example.musicplayer.PREVIOUS"
        const val ACTION_STOP = "com.example.musicplayer.STOP"
    }
}