package com.example.spotifycloneapp.Services

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.example.spotifycloneapp.MainActivity
import com.example.spotifycloneapp.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player // 1. IMPORT PLAYER

// 2. IMPLEMENT THE LISTENER INTERFACE
class MusicService : MediaBrowserServiceCompat(), Player.Listener {

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIFICATION_ID = 1
    }

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playbackStateBuilder: PlaybackStateCompat.Builder

    private var playlist: List<MediaItem> = emptyList()
    private var currentIndex = 0

    override fun onCreate() {
        super.onCreate()

        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer.setWakeMode(com.google.android.exoplayer2.C.WAKE_MODE_NETWORK)
        // 3. REGISTER THE LISTENER
        exoPlayer.addListener(this)

        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(mediaSessionCallback)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        // 4. ADD SEEK_TO ACTION. THIS IS CRITICAL FOR THE SLIDER.
        playbackStateBuilder = PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO // <-- THIS IS THE FIX
        )

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Service")
            .setContentText("Preparing...")
            .setSmallIcon(R.drawable.spotify_icon_foreground)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    // 5. THIS FUNCTION IS THE FIX FOR THE DURATION.
    // It's called automatically when the player is ready and knows the duration.
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playbackState == Player.STATE_READY && exoPlayer.currentMediaItem != null) {
            // Now that the player is ready, send the metadata with the CORRECT duration.
            updateMetadata(exoPlayer.currentMediaItem!!)
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?) =
        BrowserRoot("root", null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(mutableListOf())
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        // 6. ADD onSeekTo. This allows the ViewModel to control the slider.
        override fun onSeekTo(pos: Long) {
            exoPlayer.seekTo(pos)
            // Immediately update the playback state so the UI doesn't jump
            updatePlaybackState(mediaSession.controller.playbackState.state)
        }

        // --- NO OTHER CHANGES NEEDED TO THE REST OF THE CALLBACK ---
        override fun onPlay() {
            exoPlayer.playWhenReady = true
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            showNotification()
        }

        override fun onPause() {
            exoPlayer.playWhenReady = false
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            showNotification()
        }

        override fun onSkipToNext() {
            if (currentIndex + 1 < playlist.size) {
                currentIndex++
                playCurrentTrack()
            }
        }

        override fun onSkipToPrevious() {
            if (currentIndex - 1 >= 0) {
                currentIndex--
                playCurrentTrack()
            }
        }

        override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
            extras?.let {
                val filePaths = it.getStringArray("filePaths")
                val titles = it.getStringArray("titles")
                val artists = it.getStringArray("artists")
                val coverPaths = it.getStringArray("coverPaths")
                currentIndex = it.getInt("currentIndex", 0)

                if (filePaths != null && titles != null && artists != null && coverPaths != null) {
                    playlist = filePaths.mapIndexed { index, path ->
                        val exoPlayerMediaMetadata = com.google.android.exoplayer2.MediaMetadata.Builder()
                            .setTitle(titles[index])
                            .setArtist(artists[index])
                            .setArtworkUri(coverPaths[index]?.let { Uri.parse(it) })
                            .build()

                        MediaItem.Builder()
                            .setUri(path)
                            .setMediaId(titles[index])
                            .setMediaMetadata(exoPlayerMediaMetadata)
                            .build()
                    }
                    playCurrentTrack()
                }
            }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun playCurrentTrack() {
        if (playlist.isEmpty()) return
        val track = playlist.getOrNull(currentIndex) ?: return
        exoPlayer.setMediaItem(track)
        exoPlayer.prepare()
        exoPlayer.play()
        // DO NOT call updateMetadata here. The listener will do it at the right time.
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        showNotification()
    }

    private fun updateMetadata(track: MediaItem) {
        val title = track.mediaMetadata.title?.toString() ?: "Unknown Title"
        val artist = track.mediaMetadata.artist?.toString() ?: "Unknown Artist"
        val artUri = track.mediaMetadata.artworkUri

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, track.localConfiguration?.uri.toString())
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUri?.toString())
            // This now sends the REAL duration because it's called at the right time.
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer.duration)
            .build()
        mediaSession.setMetadata(metadata)
    }

    private fun updatePlaybackState(state: Int) {
        playbackStateBuilder.setState(state, exoPlayer.currentPosition, 1.0f)
        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }

    // --- NO CHANGES to showNotification or createNotificationChannel ---
    private fun showNotification() {
        val metadata = mediaSession.controller.metadata
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT
        val contentIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)

        val playIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
        val pauseIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)
        val nextIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        val prevIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "Playing")
            .setContentText(metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
            .setSmallIcon(R.drawable.spotify_icon_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_skip_previous, "Prev", prevIntent)
            .addAction(if (exoPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (exoPlayer.isPlaying) "Pause" else "Play",
                if (exoPlayer.isPlaying) pauseIntent else playIntent)
            .addAction(R.drawable.ic_skip_next, "Next", nextIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
            )

        val artUriString = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
        artUriString?.let {
            Glide.with(this)
                .asBitmap()
                .load(it)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                        builder.setLargeIcon(resource)
                        startForeground(NOTIFICATION_ID, builder.build())
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        } ?: startForeground(NOTIFICATION_ID, builder.build())
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        // 7. CLEAN UP THE LISTENER
        exoPlayer.removeListener(this)
        exoPlayer.release()
        mediaSession.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)
}
