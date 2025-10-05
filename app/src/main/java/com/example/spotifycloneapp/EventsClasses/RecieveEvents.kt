package com.example.spotifycloneapp.EventsClasses

import com.example.spotifycloneapp.Data.SongEntity

sealed class RecieveEvents {
    data class Success(val songs: List<SongEntity>) : RecieveEvents()
    data class Error(val message: String) : RecieveEvents()

}