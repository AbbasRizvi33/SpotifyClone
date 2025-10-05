package com.example.spotifycloneapp.Data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String="",
    val artist: String="",
    val album: String? = null,
    val category: String = "",
    val description: String? = null,
    val duration: Long? = null,
    val filePath: String="",
    val coverPath: String?="",
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = true,
    val isSyncedOnline: Boolean = false
)