package com.example.spotifycloneapp.ViewModels

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotifycloneapp.EventsClasses.HomeUIEvents
import com.example.spotifycloneapp.EventsClasses.RecieveEvents
import com.example.spotifycloneapp.Services.MusicService
import com.example.spotifycloneapp.bindingclassess.DisplaySongData
import com.example.spotifycloneapp.bindingclassess.SongData
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.text.equals

class SharedViewModel : ViewModel() {

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
    private val _likedSongs = MutableLiveData<List<DisplaySongData>>()
    val likedSongs: LiveData<List<DisplaySongData>> = _likedSongs

    private val _currentPosition = MutableLiveData<Long>()
    val currentPosition: LiveData<Long> = _currentPosition
    private var updateJob: Job? = null

    private val _filteredLikedSongs = MutableLiveData<List<DisplaySongData>>()
    val filteredLikedSongs: LiveData<List<DisplaySongData>> = _filteredLikedSongs

    private val _filteredSearchedSongs = MutableLiveData<List<DisplaySongData>>()
    val filteredSearchedSongs: LiveData<List<DisplaySongData>> = _filteredSearchedSongs


//    fun updateCurrentPosition(position: Long) {
//        // We use postValue because this will be called from a background thread in the service
//        _currentPosition.postValue(position)
//    }

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
//        _filteredSearchedSongs.postValue(songs)
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

    // In SharedViewModel.kt

// ... after toggleLikeForCurrentSong() ...

    fun toggleLikeForSong(songId: Int) {
        // Find the song in your master list of all songs
        val song = songs.value?.find { it.mediaId.toInt() == songId }

        // Or check the liked songs list if that's easier
        val isCurrentlyLiked = likedSongs.value?.any { it.mediaId.toInt() == songId } ?: false

        if (isCurrentlyLiked || song?.isLiked == true) {
            unLikeSong(songId)
        } else {
            likeSong(songId)
        }
    }

    fun playPause() {
        if (state.value?.state == PlaybackStateCompat.STATE_PLAYING) {
            mediaController?.transportControls?.pause()
        } else {
            mediaController?.transportControls?.play()
        }
    }
    fun setLikedSongs(songs: List<DisplaySongData>) {
        _likedSongs.postValue(songs)

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
        // Stop any existing job to prevent duplicates
        stopSeekBarUpdate()
        updateJob = viewModelScope.launch {
            while (isActive) {
                // Directly ask the mediaController for its playback state.
                val currentPlaybackState = mediaController?.playbackState
                _currentPosition.postValue(currentPlaybackState?.position ?: 0)
                delay(100)
            }
        }
    }

    // Add a function to stop the updates to save resources when paused.
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
        val originalList = _likedSongs.value ?: emptyList()

        val filteredList = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter { song ->
                song.title.contains(query, ignoreCase = true) || song.artist.contains(query, ignoreCase = true)
            }
        }
        _filteredLikedSongs.postValue(filteredList)
    }

//    fun viewEvent(event : HomeUIEvents){
//        viewModelScope.launch {
//            when(event){
//                is HomeUIEvents.playSong -> playReqSong(event.song)
//                is HomeUIEvents.filterCategory -> {
////                    showfilterSongs(event.category)
//                }
//                else -> {}
//            }
//        }
//    }

    fun showAllSongs(){
        viewModelScope.launch {
            val allSongs = songs.value ?: emptyList()
            if (allSongs.isNotEmpty()) {
                _Events.send(RecieveEvents.Success(allSongs))
            }
            else{
                _Events.send(RecieveEvents.Error("No songs found"))
            }
        }
    }



//    fun showfilterSongs(category: String) {
//        viewModelScope.launch {
//            val allSongs = songs.value ?: emptyList()
//
//            val filteredList = when (category) {
//                "All" -> {
//                    allSongs
//                }
//                MusicService.LIKED_SONGS_ROOT_ID -> {
//                    allSongs.filter { it.isLiked?:false }
//                }
//                else -> {
//                    allSongs.filter { it.category.equals(category, true) }
//                }
//            }
//
//            if (filteredList.isNotEmpty()) {
//                _Events.send(RecieveEvents.Success(filteredList))
//            } else {
//                _Events.send(RecieveEvents.Error("No songs found in $category"))
//            }
//        }
//    }




}
