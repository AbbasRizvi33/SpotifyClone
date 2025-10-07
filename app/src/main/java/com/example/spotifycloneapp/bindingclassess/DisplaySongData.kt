package com.example.spotifycloneapp.bindingclassess

data class DisplaySongData (
    val mediaId: String,
    val title: String,
    val artist: String,
    val filePath: String,
    val category: String,
    val coverPath : String,
    val isLiked: Boolean? = null
)