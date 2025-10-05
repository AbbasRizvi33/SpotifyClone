package com.example.spotifycloneapp.EventsClasses

import com.example.spotifycloneapp.Data.SongEntity

sealed class HomeUIEvents {
    object getSongs : HomeUIEvents()
    data class filterCategory(val category: String) : HomeUIEvents()
}