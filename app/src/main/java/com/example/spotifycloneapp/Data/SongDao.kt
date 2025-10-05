package com.example.spotifycloneapp.Data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE category = :categoryName")
    fun getSongsByCategory(categoryName: String): Flow<List<SongEntity>>

    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateSongLikeStatus(songId: Long, isLiked: Boolean)

    @Query("SELECT * FROM songs WHERE isLiked = 1")
    fun getLikedSongs(): LiveData<List<SongEntity>>

    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikedStatus(songId: Int, isLiked: Boolean)

    // In SongDao.kt
    @Query("SELECT * FROM songs WHERE title = :title LIMIT 1")
    suspend fun getSongByTitle(title: String?): SongEntity?

}
