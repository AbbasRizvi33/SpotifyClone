package com.example.spotifycloneapp.EventsClasses

import com.example.spotifycloneapp.Data.SongEntity
import com.example.spotifycloneapp.bindingclassess.SongData

sealed class HomeUIEvents {
    object getSongs : HomeUIEvents()
    data class filterCategory(val category: String) : HomeUIEvents()
    data class playSong(val song: SongData) : HomeUIEvents()
}