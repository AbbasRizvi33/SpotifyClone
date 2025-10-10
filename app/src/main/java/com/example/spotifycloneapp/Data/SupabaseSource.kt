// In new file: app/src/main/java/com/example/spotifycloneapp/data/SupabaseSource.kt
package com.example.spotifycloneapp.Data

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileOutputStream

@Serializable
data class RemoteSong(
    val id: Int,
    val title: String,
    val artist: String,
    val category: String,
    val storage_path: String,
    val cover_path: String? = null
)

class SupabaseSource @Inject constructor(
    private val db: Postgrest,
    private val storage: Storage,
    @ApplicationContext private val context: Context
) {

    suspend fun getRemoteSongs(): List<SongEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val remoteSongs = db.from("songs").select().decodeList<RemoteSong>()
                Log.d("Repo", "getRemoteSongs: $remoteSongs ")
                downloadAndCacheSongs(remoteSongs)
            } catch (e: Exception) {
                Log.e("SupabaseSource", "Failed to fetch remote songs", e)
                emptyList()
            }
        }
    }

    private suspend fun downloadAndCacheSongs(songs: List<RemoteSong>): List<SongEntity> {
        val localSongsDir = File(context.filesDir, "songs").apply { mkdirs() }
        val localCoversDir = File(context.filesDir, "covers").apply { mkdirs() }

        return withContext(Dispatchers.IO) {
            songs.mapNotNull { remoteSong ->
                try {
                    val localSongFilename = "${remoteSong.id}.mp3"
                    val songFile = File(localSongsDir, localSongFilename)
                    val possiblePaths = listOf(
                        remoteSong.storage_path,
                        "${remoteSong.storage_path}.mp3",
                        "songs/${remoteSong.storage_path}",
                        "songs/${remoteSong.storage_path}.mp3"
                    )

                    if (!songFile.exists()) {
                        var downloaded = false
                        for (path in possiblePaths) {
                            try {
                                Log.d("SupabaseSource", "Trying song path: $path")
                                val songBytes = storage.from("song-files").downloadPublic(path)
                                FileOutputStream(songFile).use { it.write(songBytes) }
                                Log.d("SupabaseSource", "Cached song '${remoteSong.title}' to ${songFile.absolutePath}")
                                downloaded = true
                                break
                            } catch (e: Exception) {
                                Log.w("SupabaseSource", "Failed for path '$path': ${e.message}")
                            }
                        }

                        if (!downloaded) {
                            throw Exception("No valid song path found for ${remoteSong.title}")
                        }
                    }

                    // --- Handle cover image if exists ---
                    var localCoverPath: String? = null
                    if (!remoteSong.cover_path.isNullOrEmpty()) {
                        val coverFilename = remoteSong.cover_path.substringAfterLast('/')
                        val localCoverFilename = "${remoteSong.id}.${coverFilename.substringAfterLast('.')}"
                        val coverFile = File(localCoversDir, localCoverFilename)
                        localCoverPath = coverFile.absolutePath

                        if (!coverFile.exists()) {
                            val possibleCoverPaths = listOf(
                                remoteSong.cover_path,
                                "covers/${remoteSong.cover_path}"
                            )

                            var coverDownloaded = false
                            for (coverPath in possibleCoverPaths) {
                                try {
                                    Log.d("SupabaseSource", "Trying cover path: $coverPath")
                                    val coverBytes = storage.from("song-files").downloadPublic(coverPath)
                                    FileOutputStream(coverFile).use { it.write(coverBytes) }
                                    Log.d("SupabaseSource", "Cached cover for '${remoteSong.title}' to ${coverFile.absolutePath}")
                                    coverDownloaded = true
                                    break
                                } catch (e: Exception) {
                                    Log.w("SupabaseSource", "Failed for cover path '$coverPath': ${e.message}")
                                }
                            }

                            if (!coverDownloaded) {
                                Log.e("SupabaseSource", "No valid cover path found for ${remoteSong.title}")
                            }
                        }
                    }

                    SongEntity(
                        id = remoteSong.id,
                        title = remoteSong.title,
                        artist = remoteSong.artist,
                        category = remoteSong.category,
                        filePath = songFile.absolutePath,
                        coverPath = localCoverPath
                    )

                } catch (e: Exception) {
                    Log.e(
                        "SupabaseSource",
                        "CRITICAL FAILURE: Could not cache '${remoteSong.title}' [${remoteSong.storage_path}]: ${e.message}",
                        e
                    )
                    null
                }
            }
        }
    }

    suspend fun updateLikedStatus(songId: Int, isLiked: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                db.from("songs")
                    .update(
                        {
                            set("is_liked", isLiked)
                        },
                        {
                            filter {
                                eq("id", songId)
                            }
                        }
                    )
                Log.d("SupabaseSource", "Successfully updated liked status for song $songId on the server.")
            } catch (e: Exception) {
                Log.e("SupabaseSource", "Failed to update liked status for song $songId on the server.", e)
                throw e
            }
        }
    }

}
