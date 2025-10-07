package com.example.spotifycloneapp.EventsClasses

import com.example.spotifycloneapp.Data.SongEntity
import com.example.spotifycloneapp.bindingclassess.DisplaySongData

sealed class RecieveEvents {
    data class Success(val songs: List<DisplaySongData>) : RecieveEvents()
    data class Error(val message: String) : RecieveEvents()
    object Empty : RecieveEvents()
    object Loading: RecieveEvents()

    object toggleLikeSongIcon : RecieveEvents()

}