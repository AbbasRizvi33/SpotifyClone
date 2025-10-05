package com.example.spotifycloneapp.Services

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.spotifycloneapp.MainActivity
import com.example.spotifycloneapp.Repos.Repository
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MusicService() : MediaBrowserServiceCompat() {

    @Inject lateinit var repo: Repository
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var session: MediaSessionCompat
    private var songList: List<MediaBrowserCompat.MediaItem> = emptyList()
    private var currentIndex = 0

    companion object {
        const val NOTIFICATION_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()

        session = MediaSessionCompat(this, "SpotifyCloneSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {

                override fun onPlay() {
                    if (exoPlayer.currentMediaItem == null && songList.isNotEmpty()) {
                        playSong(currentIndex)
                    } else {
                        exoPlayer.play()
                    }
                    showNotification()
                }

                override fun onPause() {
                    exoPlayer.pause()
                    showNotification()
                }

                override fun onStop() {
                    exoPlayer.stop()
                    stopForeground(true)
                }

                override fun onSkipToNext() {
                    if (songList.isNotEmpty()) {
                        currentIndex = (currentIndex + 1) % songList.size
                        playSong(currentIndex)
                    }
                }

                override fun onSkipToPrevious() {
                    if (songList.isNotEmpty()) {
                        currentIndex = if (currentIndex - 1 < 0) songList.size - 1 else currentIndex - 1
                        playSong(currentIndex)
                    }
                }

                override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
                    val index = songList.indexOfFirst { it.description.mediaId == mediaId }
                    if (index != -1) {
                        currentIndex = index
                        playSong(currentIndex)
                    }
                }
            })
        }

        sessionToken = session.sessionToken
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot("root_id", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem?>?>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            repo.getAllSongs().first().let { songs ->
                songList = songs.map { song ->
                    val description = MediaDescriptionCompat.Builder()
                        .setMediaId(song.id.toString())
                        .setTitle(song.title)
                        .setSubtitle(song.artist)
                        .setIconUri(Uri.parse(song.coverPath))
                        .setExtras(Bundle().apply {
                            putString("category", song.category)
                            putBoolean("liked", song.isLiked)
                            putBoolean("synced", song.isSyncedOnline)
                            putString("audioPath", song.filePath)
                        })
                        .build()

                    MediaBrowserCompat.MediaItem(
                        description,
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                    )
                }
                result.sendResult(songList)
            }
        }
    }

    private fun playSong(index: Int) {
        val song = songList[index]
        val path = song.description.extras?.getString("audioPath") ?: return
        val mediaItem = MediaItem.fromUri(Uri.parse(path))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        showNotification()
    }

    private fun showNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, "default_channel")
            .setContentTitle(exoPlayer.currentMediaItem?.mediaId ?: "Playing")
            .setContentText("Spotify")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(exoPlayer.isPlaying)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}
