package com.example.spotifycloneapp.ViewModels

import android.app.Application
import android.content.ComponentName
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.spotifycloneapp.Repos.Repository
import com.example.spotifycloneapp.Services.MusicService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    application: Application,
    private val repository: Repository
) : AndroidViewModel(application) {

    val currentMetadata = MutableLiveData<MediaMetadataCompat?>()
    val playbackState = MutableLiveData<PlaybackStateCompat?>()
    val currentPosition = MutableLiveData<Long>()
    val isCurrentSongLiked = MutableLiveData<Boolean>()

    private var currentSongId: Int? = null
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaController: MediaControllerCompat? = null
    private var updateJob: Job? = null

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            currentMetadata.postValue(metadata)
            val songTitle = metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            viewModelScope.launch {
                val song = repository.getSongByTitle(songTitle)
                if (song != null) {
                    currentSongId = song.id
                    isCurrentSongLiked.postValue(song.isLiked)
                } else {
                    currentSongId = null
                    isCurrentSongLiked.postValue(false)
                }
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            playbackState.postValue(state)
        }
    }

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaController = MediaControllerCompat(getApplication(), mediaBrowser.sessionToken).apply {
                registerCallback(controllerCallback)
            }
            currentMetadata.postValue(mediaController?.metadata)
            playbackState.postValue(mediaController?.playbackState)
        }
    }

    init {
        mediaBrowser = MediaBrowserCompat(
            application,
            ComponentName(application, MusicService::class.java),
            connectionCallbacks,
            null
        ).apply { connect() }

        playbackState.observeForever { state ->
            updateJob?.cancel()
            if (state?.state == PlaybackStateCompat.STATE_PLAYING) {
                startSeekBarUpdate() // Call the corrected function
            }
        }
    }

    fun playPause() {
        val state = playbackState.value?.state
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            mediaController?.transportControls?.pause()
        } else {
            mediaController?.transportControls?.play()
        }
    }

    fun skipToNext() {
        mediaController?.transportControls?.skipToNext()
    }

    fun skipToPrevious() {
        mediaController?.transportControls?.skipToPrevious()
    }

    fun seekTo(position: Long) {
        mediaController?.transportControls?.seekTo(position)
    }

    fun toggleLikeForCurrentSong() {
        val id = currentSongId ?: return
        val isLiked = isCurrentSongLiked.value ?: return
        viewModelScope.launch {
            repository.updateLikedStatus(id, !isLiked)
            isCurrentSongLiked.postValue(!isLiked)
        }
    }

    // THIS IS THE CORRECTED FUNCTION
    private fun startSeekBarUpdate() {
        updateJob = viewModelScope.launch {
            while (isActive) {
                // Directly ask the mediaController for its playback state.
                // This is safer and how it worked before.
                val currentPlaybackState = mediaController?.playbackState
                currentPosition.postValue(currentPlaybackState?.position ?: 0)
                delay(100)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.unregisterCallback(controllerCallback)
        mediaBrowser.disconnect()
        updateJob?.cancel()
    }
}
