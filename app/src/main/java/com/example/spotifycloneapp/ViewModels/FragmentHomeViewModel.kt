package com.example.spotifycloneapp.ViewModels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotifycloneapp.Data.SongEntity
import com.example.spotifycloneapp.EventsClasses.HomeUIEvents
import com.example.spotifycloneapp.EventsClasses.RecieveEvents
import com.example.spotifycloneapp.Repos.Repository
import com.example.spotifycloneapp.Services.MusicService
import com.example.spotifycloneapp.bindingclassess.SongData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FragmentHomeViewModel @Inject constructor(
    private val repo: Repository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _Events: MutableSharedFlow<RecieveEvents> = MutableSharedFlow<RecieveEvents>()
    val Events get() = _Events

    private var songs: List<SongEntity> = emptyList()
    private var currentSongs: List<SongEntity> = emptyList()
    private var mediaController: MediaControllerCompat? = null

    fun viewEvent(event : HomeUIEvents){
        viewModelScope.launch {
            when(event){
                is HomeUIEvents.getSongs -> getSongs()
                is HomeUIEvents.filterCategory -> filterResults(event.category)
                is HomeUIEvents.playSong-> playReqSong(event.song)
                else -> {}
            }
        }
    }

    fun setController(controller: MediaControllerCompat) {
        mediaController = controller
    }


    // FragmentHomeViewModel.kt
    fun playReqSong(song: SongData) {
        // 1. Ensure the service is running
        val serviceIntent = Intent(context, MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(serviceIntent)
        else
            context.startService(serviceIntent)

        // 2. Prepare playlist for the service
        val songIndex = currentSongs.indexOfFirst { it.filePath == song.filePath }
        if (songIndex == -1) return // Should not happen

        val bundle = Bundle().apply {
            val songEntity = currentSongs[songIndex]
            putString("artist", songEntity.artist) // Pass artist from the full entity

            // Pass the whole playlist
            val filePaths = currentSongs.map { it.filePath }.toTypedArray()
            val titles = currentSongs.map { it.title }.toTypedArray()
            val artists = currentSongs.map { it.artist }.toTypedArray()
            val coverPaths = currentSongs.map { it.coverPath }.toTypedArray()

            putStringArray("filePaths", filePaths)
            putStringArray("titles", titles)
            putStringArray("artists", artists)
            putStringArray("coverPaths", coverPaths)
            putInt("currentIndex", songIndex)
        }

        // 3. Instruct the MediaSession to play the track via URI with the full playlist
        mediaController?.transportControls?.playFromUri(Uri.parse(song.filePath), bundle)
    }



    fun skipNext() {
        mediaController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        mediaController?.transportControls?.skipToPrevious()
    }

//    private fun playReqSong(playable: SongData) {
//        val intent = Intent(context, MusicService::class.java).apply {
//            action = "ACTION_PLAY"
//            putExtra("filePath", playable.filePath)
//            putExtra("title", playable.title)
//            putExtra("coverPath", playable.coverPath)
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//            context.startForegroundService(intent)
//        else
//            context.startService(intent)
//    }

    private suspend fun getSongs(){
        withContext(Dispatchers.IO){
            repo.getAllSongs().collect { data->
                if(data.isNotEmpty()){
                    songs = data
                    currentSongs = data // Keep track of the current list
                    _Events.emit(RecieveEvents.Success(data))
                }
                else{
                    _Events.emit(RecieveEvents.Error("No Data Found"))
                }
            }
        }
    }

    private suspend fun filterResults(category: String) {
        val filtered = if (category.equals("All", true)) songs
        else songs.filter { it.category.equals(category, true) }

        currentSongs = filtered // Keep track of the filtered list

        if (filtered.isNotEmpty()) {
            _Events.emit(RecieveEvents.Success(filtered))
        } else {
            _Events.emit(RecieveEvents.Error("No songs found in $category"))
        }
    }



}
