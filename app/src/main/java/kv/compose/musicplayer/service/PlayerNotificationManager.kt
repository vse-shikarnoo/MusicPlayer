package kv.compose.musicplayer.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kv.compose.musicplayer.MainActivity
import kv.compose.musicplayer.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader
) {
    private var mediaSession: MediaSession? = null

    init {
        createNotificationChannel()
    }

    fun setMediaSession(session: MediaSession) {
        mediaSession = session
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(true)
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    @OptIn(UnstableApi::class)
    fun buildNotification(
        title: String,
        artist: String,
        isPlaying: Boolean,
        artworkUrl: String?,
        scope: CoroutineScope
    ): Notification {
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("Now Playing") // Add a subtitle
            .setOngoing(isPlaying) // Make notification persistent while playing
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    mainIntent,
                    pendingIntentFlags
                )
            )
            .setDeleteIntent(
                PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(MusicService.ACTION_STOP).setPackage(context.packageName),
                    pendingIntentFlags
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT) // Set proper category for media controls
            .setShowWhen(false) // Hide timestamp
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_skip_previous,
                    "Previous",
                    PendingIntent.getBroadcast(
                        context,
                        1,
                        Intent(MusicService.ACTION_PREVIOUS).setPackage(context.packageName),
                        pendingIntentFlags
                    )
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    playPauseIcon,
                    if (isPlaying) "Pause" else "Play",
                    PendingIntent.getBroadcast(
                        context,
                        2,
                        Intent(MusicService.ACTION_PLAY_PAUSE).setPackage(context.packageName),
                        pendingIntentFlags
                    )
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_skip_next,
                    "Next",
                    PendingIntent.getBroadcast(
                        context,
                        3,
                        Intent(MusicService.ACTION_NEXT).setPackage(context.packageName),
                        pendingIntentFlags
                    )
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.baseline_close_24,
                    "Stop",
                    PendingIntent.getBroadcast(
                        context,
                        4,
                        Intent(MusicService.ACTION_STOP).setPackage(context.packageName),
                        pendingIntentFlags
                    )
                ).build()
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2) // Show previous, play/pause, next in compact view
                    .setMediaSession(mediaSession?.sessionCompatToken)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        PendingIntent.getBroadcast(
                            context,
                            5,
                            Intent(MusicService.ACTION_STOP).setPackage(context.packageName),
                            pendingIntentFlags
                        )
                    )
            )

        if (!artworkUrl.isNullOrEmpty()) {
            scope.launch {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(artworkUrl)
                        .size(256, 256) // Set optimal size for notification
                        .crossfade(true)
                        .build()
                    val result = imageLoader.execute(request)
                    val bitmap = result.drawable?.toBitmap()
                    bitmap?.let {
                        builder.setLargeIcon(it)
                        updateNotification(builder.build())
                    }
                } catch (e: Exception) {
                    // Ignore artwork loading errors
                }
            }
        }

        return builder.build()
    }

    fun updateNotification(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context)
                    .notify(MusicService.NOTIFICATION_ID, notification)
            }
        } else {
            NotificationManagerCompat.from(context)
                .notify(MusicService.NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "music_playback_channel"
    }
}