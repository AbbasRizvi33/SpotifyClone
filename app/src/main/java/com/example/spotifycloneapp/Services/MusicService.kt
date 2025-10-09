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
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.example.spotifycloneapp.Data.SongEntity
import com.example.spotifycloneapp.MainActivity
import com.example.spotifycloneapp.R
import com.example.spotifycloneapp.Repos.Repository
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.net.toUri

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat(), Player.Listener {
    @Inject lateinit var repo: Repository
    @Inject lateinit var exoPlayer: ExoPlayer
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIFICATION_ID = 1
        const val MEDIA_ROOT_ID = "root_id"
        const val ACTION_UNLIKE_SONG = "com.example.spotifycloneapp.ACTION_UNLIKE_SONG"
        const val ACTION_LIKE_SONG = "com.example.spotifycloneapp.ACTION_LIKE_SONG"

    }

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playbackStateBuilder: PlaybackStateCompat.Builder

    private var playlist: List<MediaItem> = emptyList()
    private var currentIndex = 0

    override fun onCreate() {
        super.onCreate()
        exoPlayer.addListener(this)

        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(mediaSessionCallback)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        playbackStateBuilder = PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO
        )

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Service")
            .setContentText("Preparing...")
            .setSmallIcon(R.drawable.spotify_icon_foreground)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playbackState == Player.STATE_READY && exoPlayer.currentMediaItem != null) {
            updateMetadata(exoPlayer.currentMediaItem!!)
        }
        if(playbackState == Player.STATE_ENDED){
            if(currentIndex<playlist.size){
                currentIndex++
                playCurrentTrack()
            }
            else{
                currentIndex = 0
                playCurrentTrack()
            }
        }
    }


    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?) =
        BrowserRoot(MEDIA_ROOT_ID, null)


    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.detach()
        serviceScope.launch(Dispatchers.IO) {
            val allSongs = repo.getAllSongs().first()
            val mediaItems = when (parentId) {
                MEDIA_ROOT_ID -> {
                    allSongs.map { song ->
                        createMediaItem(song)
                    }.toMutableList()
                }
                else -> null
            }
            result.sendResult(mediaItems)

        }
    }

    private fun createMediaItem(song: SongEntity): MediaBrowserCompat.MediaItem {
        val extras = Bundle().apply {
            putString("category", song.category)
            putString("coverPath", song.coverPath)
            putBoolean("isLiked", song.isLiked)
        }
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(song.id.toString())
            .setTitle(song.title)
            .setSubtitle(song.artist)
            .setMediaUri(song.filePath.toUri())
            .setIconUri(song.coverPath?.let { it.toUri() })
            .setExtras(extras)
            .build()

        return MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onSeekTo(pos: Long) {
            exoPlayer.seekTo(pos)
            updatePlaybackState(mediaSession.controller.playbackState.state)
        }

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

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
            if (mediaId == null) return

            val parentMediaId = extras?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            val clickedSongExistsInCurrentPlaylist = playlist.any { it.mediaId == mediaId }
            val currentPlaylistParentId = mediaSession.controller?.metadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST)

            if (clickedSongExistsInCurrentPlaylist && parentMediaId == currentPlaylistParentId) {
                serviceScope.launch(Dispatchers.Main) {
                    val indexToPlay = playlist.indexOfFirst { it.mediaId == mediaId }
                    if (indexToPlay != -1) {
                        exoPlayer.seekTo(indexToPlay, 0L)
                        exoPlayer.playWhenReady = true
                        currentIndex = indexToPlay
                    }
                }
            } else {
                serviceScope.launch(Dispatchers.IO) {
                    val songSource =
                        when (parentMediaId) {
                            MEDIA_ROOT_ID -> repo.getAllSongs().first()
                            else -> repo.getAllSongs().first()
                        }

                    val songToPlayIndex = songSource.indexOfFirst { it.id.toString() == mediaId }
                    if (songToPlayIndex == -1) return@launch

                    currentIndex = songToPlayIndex
                    playlist = songSource.map { songEntity ->
                        val metadataExtras = Bundle().apply {
                            putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, parentMediaId)
                            putString("isLiked", songEntity.isLiked.toString())
                        }

                        val exoPlayerMediaMetadata = com.google.android.exoplayer2.MediaMetadata.Builder()
                            .setTitle(songEntity.title)
                            .setArtist(songEntity.artist)
                            .setArtworkUri(songEntity.coverPath?.let { Uri.parse(it) })
                            .setExtras(metadataExtras)
                            .setAlbumArtist(parentMediaId)
                            .build()

                        MediaItem.Builder()
                            .setUri(songEntity.filePath)
                            .setMediaId(songEntity.id.toString())
                            .setMediaMetadata(exoPlayerMediaMetadata)
                            .build()

                    }

                    withContext(Dispatchers.Main) {
                        playCurrentTrack()
                    }
                }
            }
        }

        private suspend fun refreshCurrentSongMetadata() {
            val currentMediaItem = withContext(Dispatchers.Main) {
                exoPlayer.currentMediaItem
            } ?: return

            val currentMediaId = currentMediaItem.mediaId.toIntOrNull() ?: return
            val updatedSong = withContext(Dispatchers.IO) {
                repo.getAllSongs().first().find { it.id == currentMediaId }
            } ?: return

            val updatedExoPlayerMediaMetadata = currentMediaItem.mediaMetadata.buildUpon()
                .setExtras(Bundle().apply { putString("isLiked", updatedSong.isLiked.toString()) })
                .build()

            val updatedMediaItem = currentMediaItem.buildUpon()
                .setMediaMetadata(updatedExoPlayerMediaMetadata)
                .build()

            withContext(Dispatchers.Main) {
                updateMetadata(updatedMediaItem)
            }
        }



        override fun onCustomAction(action: String?, extras: Bundle?) {
            super.onCustomAction(action, extras)
            when(action){
                ACTION_UNLIKE_SONG -> {
                    unlikeASong(extras?.getInt("song_id",0)?:0)
                }
                ACTION_LIKE_SONG -> {
                    likeASong(extras?.getInt("song_id",0)?:0)
                }
                else -> {}
            }

        }

        private fun unlikeASong(songID : Int){
            serviceScope.launch {
                repo.updateLikedStatus(songID, false)
                notifyChildrenChanged(MEDIA_ROOT_ID)

                val currentPlayingId = mediaSession.controller.metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)?.toIntOrNull()
                if (currentPlayingId == songID) {
                    refreshCurrentSongMetadata()
                }
            }
        }
        private fun likeASong(songID : Int){
            serviceScope.launch {
                repo.updateLikedStatus(songID, true)
                notifyChildrenChanged(MEDIA_ROOT_ID)
                val currentPlayingId = mediaSession.controller.metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)?.toIntOrNull()
                if (currentPlayingId == songID) {
                    refreshCurrentSongMetadata()
                }

            }
        }

    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun playCurrentTrack() {
        if (playlist.isEmpty() || currentIndex < 0 || currentIndex >= playlist.size) return
        exoPlayer.setMediaItems(playlist, currentIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        this@MusicService.updateMetadata(playlist[currentIndex])
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
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer.duration)
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.mediaId)
            .putString("isLiked", track.mediaMetadata.extras?.getString("isLiked"))
            .build()
        mediaSession.setMetadata(metadata)
    }

    private fun updatePlaybackState(state: Int) {
        playbackStateBuilder.setState(state, exoPlayer.currentPosition, 1.0f)
        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }
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
            .setVisibility(VISIBILITY_PUBLIC)
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
        exoPlayer.removeListener(this)
        exoPlayer.release()
        mediaSession.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)
}
