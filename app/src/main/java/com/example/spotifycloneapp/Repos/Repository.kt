package com.example.spotifycloneapp.Repos

import com.example.spotifycloneapp.Data.SongDao
import com.example.spotifycloneapp.Data.SongEntity
import javax.inject.Inject

public class Repository @Inject constructor(
    private val songDao: SongDao
) {
    fun getAllSongs() = songDao.getAllSongs()
    fun getSongsByCategory(category: String) = songDao.getSongsByCategory(category)

    suspend fun insertSong(song: SongEntity) = songDao.insertSong(song)
    suspend fun insertSongs(song: List<SongEntity>) = songDao.insertSongs(song)
    suspend fun updateLiked(id: Int, isLiked: Boolean) = songDao.updateLiked(id, isLiked)
}