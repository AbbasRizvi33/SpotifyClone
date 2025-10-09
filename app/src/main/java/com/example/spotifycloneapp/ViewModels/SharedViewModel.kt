package com.example.spotifycloneapp.ViewModels

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.spotifycloneapp.EventsClasses.HomeUIEvents
import com.example.spotifycloneapp.EventsClasses.RecieveEvents
import com.example.spotifycloneapp.Repos.Repository
import com.example.spotifycloneapp.Services.MusicService
import com.example.spotifycloneapp.bindingclassess.DisplaySongData
import com.example.spotifycloneapp.bindingclassess.SongData
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.text.equals

@HiltViewModel
class SharedViewModel @Inject constructor(
) : ViewModel() {
    private val _Events: Channel<RecieveEvents> = Channel<RecieveEvents>()
    val Events get() = _Events.receiveAsFlow()
    private val _LikedSongsEvents: MutableLiveData<RecieveEvents> = MutableLiveData<RecieveEvents>()
    val LikedSongsEvents get() = _LikedSongsEvents
    private var mediaController: MediaControllerCompat? = null
    private val _metadata = MutableLiveData<MediaMetadataCompat?>()
    val metadata: LiveData<MediaMetadataCompat?> = _metadata
    private val _state = MutableLiveData<PlaybackStateCompat?>()
    val state: LiveData<PlaybackStateCompat?> = _state
    private val _songs = MutableLiveData<List<DisplaySongData>>()
    val songs: LiveData<List<DisplaySongData>> = _songs
    val likedSongs: LiveData<List<DisplaySongData>> = _songs
    .map { allSongs -> allSongs.filter { it.isLiked?:false } }
    private val _currentPosition = MutableLiveData<Long>()
    val currentPosition: LiveData<Long> = _currentPosition
    private var updateJob: Job? = null
    private val _filteredLikedSongs = MutableLiveData<List<DisplaySongData>>()
    val filteredLikedSongs: LiveData<List<DisplaySongData>> = _filteredLikedSongs

    private val _filteredSearchedSongs = MutableLiveData<List<DisplaySongData>>()
    val filteredSearchedSongs: LiveData<List<DisplaySongData>> = _filteredSearchedSongs
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading


    init{
        _isLoading.postValue(true)
    }
    fun seekTo(position: Long) {
        mediaController?.transportControls?.seekTo(position)
    }
    fun setController(controller: MediaControllerCompat) {
        mediaController = controller
    }

    fun updateMetadata(metadata: MediaMetadataCompat?) {
        _metadata.postValue(metadata)
    }

    fun updatePlaybackState(state: PlaybackStateCompat?) {
        _state.postValue(state)
        if (state?.state == PlaybackStateCompat.STATE_PLAYING) {
            startSeekBarUpdate()
        } else {
            stopSeekBarUpdate()
        }
    }

    fun setSongs(songs: List<DisplaySongData>) {
        _songs.postValue(songs)
        _isLoading.postValue(false)
    }


    fun playReqSong(mediaId: String, parentMediaId: String) {
        val extras = Bundle().apply {
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, parentMediaId)
        }
        mediaController?.transportControls?.playFromMediaId(mediaId, extras)
    }

    fun skipToNext(){
        mediaController?.transportControls?.skipToNext()
    }
    fun skipToPrevious(){
        mediaController?.transportControls?.skipToPrevious()
    }
    fun pause() {
        mediaController?.transportControls?.pause()
    }

    fun resume(){
        mediaController?.transportControls?.play()
    }

    fun toggleLikeForCurrentSong() {
        val currentMetadata = metadata.value ?: return
        val id= currentMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)?.toIntOrNull() ?: return
        val isLikedS = currentMetadata.getString("isLiked")
        val isLiked = isLikedS.toBoolean() ?: false
        if(isLiked){
            unLikeSong(id)
        }
        else{
            likeSong(id)
        }
    }

    fun playPause() {
        if (state.value?.state == PlaybackStateCompat.STATE_PLAYING) {
            mediaController?.transportControls?.pause()
        } else {
            mediaController?.transportControls?.play()
        }
    }

    fun likeSong(songId: Int){
        val extras = Bundle().apply {
            putInt("song_id", songId)
        }
        mediaController?.transportControls?.sendCustomAction(MusicService.ACTION_LIKE_SONG, extras)
    }
    fun unLikeSong(songId:Int){
        val extras = Bundle().apply {
            putInt("song_id", songId)
        }
        mediaController?.transportControls?.sendCustomAction(MusicService.ACTION_UNLIKE_SONG, extras)
    }

    private fun startSeekBarUpdate() {
        stopSeekBarUpdate()
        updateJob = viewModelScope.launch {
            while (isActive) {
                val currentPlaybackState = mediaController?.playbackState
                _currentPosition.postValue(currentPlaybackState?.position ?: 0)
                delay(100)
            }
        }
    }

    private fun stopSeekBarUpdate() {
        updateJob?.cancel()
        updateJob = null
    }

    fun filterAllSongs(query: String){
        val originalList = _songs.value ?: emptyList()

        val filteredList = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter { song ->
                song.title.contains(query, ignoreCase = true) || song.artist.contains(query, ignoreCase = true)
            }
        }
        _filteredSearchedSongs.postValue(filteredList)
    }

    fun filterSongs(query: String) {
        val originalList = likedSongs.value ?: emptyList()

        val filteredList = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter { song ->
                song.title.contains(query, ignoreCase = true) || song.artist.contains(query, ignoreCase = true)
            }
        }
        _filteredLikedSongs.postValue(filteredList)
    }

}
