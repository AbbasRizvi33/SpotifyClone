// C:/.../app/src/main/java/com/example/spotifycloneapp/Repos/Repository.kt

package com.example.spotifycloneapp.Repos

import androidx.lifecycle.LiveData
import com.example.spotifycloneapp.Data.SongDao
import com.example.spotifycloneapp.Data.SongEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // It's good practice to make the repository a singleton
class Repository @Inject constructor(
    private val songDao: SongDao
) {

    fun getAllSongs(): Flow<List<SongEntity>> {
        return songDao.getAllSongs()
    }

    fun getLikedSongs(): LiveData<List<SongEntity>> {
        return songDao.getLikedSongs()
    }

    fun getSongsByCategory(category: String): Flow<List<SongEntity>> {
        return songDao.getSongsByCategory(category)
    }


    suspend fun insertSongs(songs: List<SongEntity>) {
        songDao.insertSongs(songs)
    }

    suspend fun updateLikedStatus(songId: Int, isLiked: Boolean) {
        songDao.updateLikedStatus(songId, isLiked)
    }

    // In Repository.kt
    suspend fun getSongByTitle(title: String?): SongEntity? {
        if (title == null) return null
        return songDao.getSongByTitle(title)
    }
}
