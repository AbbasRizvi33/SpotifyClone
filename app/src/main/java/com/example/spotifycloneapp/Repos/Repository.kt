
package com.example.spotifycloneapp.Repos

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.spotifycloneapp.Data.PendingUpdate
import com.example.spotifycloneapp.Data.PendingUpdateDao
import com.example.spotifycloneapp.Data.SongDao
import com.example.spotifycloneapp.Data.SongEntity
import com.example.spotifycloneapp.Data.SupabaseSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
    private val songDao: SongDao,
    private val supabaseSource: SupabaseSource,
    private val pendingUpdateDao: PendingUpdateDao,
    @ApplicationContext private val context: Context
) {
    suspend fun insertSongs(songs: List<SongEntity>) {
        songDao.insertSongs(songs)
    }
    suspend fun getSongByTitle(title: String?): SongEntity? {
        if (title == null) return null
        return songDao.getSongByTitle(title)
    }

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        processPendingUpdates()
    }

    fun getAllSongs(): Flow<List<SongEntity>> {
        return songDao.getAllSongs()
            .onStart {
                Log.d("Repository", "New collector subscribed. Checking for remote updates...")
                if (isNetworkAvailable()) {
                    try {
                        val remoteSongs = supabaseSource.getRemoteSongs()
                        val localSongs = songDao.getAllSongs().firstOrNull() ?: emptyList()
                        val localSongIds = localSongs.map { it.id }.toSet()
                        val remoteSongIds = remoteSongs.map { it.id }.toSet()

                        if (remoteSongIds != localSongIds && remoteSongs.isNotEmpty()) {
                            Log.d("Repository", "Remote data is different. Updating local database.")
                            songDao.deleteAllSongs()
                            songDao.insertSongs(remoteSongs)
                        } else {
                            Log.d("Repository", "Local data is already up-to-date.")
                        }
                    } catch (e: Exception) {
                        Log.e("Repository", "Failed to fetch remote songs. Will rely on local data. Error: ${e.message}")
                    }
                } else {
                    Log.d("Repository", "Network not available. Displaying local data only.")
                }
            }.catch { e ->
                Log.e("Repository", "Error in the Room flow itself: ${e.message}")
                emit(emptyList())
            }
    }


    suspend fun updateLikedStatus(songId: Int, isLiked: Boolean) {
        songDao.updateLikedStatus(songId, isLiked)

        if (isNetworkAvailable()) {
            supabaseSource.updateLikedStatus(songId, isLiked)
        } else {
            Log.w("Repository", "Offline. Queuing like/unlike for song $songId.")
            val update = PendingUpdate(songId = songId, isLiked = isLiked)
            pendingUpdateDao.addUpdate(update)
        }
    }

    fun processPendingUpdates() {
        if (!isNetworkAvailable()) return

        repositoryScope.launch {
            val pending = pendingUpdateDao.getAllPendingUpdates()
            if (pending.isNotEmpty()) {
                Log.d("Repository", "Found ${pending.size} pending updates. Syncing with server...")
                pending.forEach { update ->
                    try {
                        supabaseSource.updateLikedStatus(update.songId, update.isLiked)
                        pendingUpdateDao.deleteUpdate(update.id)
                        Log.d("Repository", "Successfully synced update for song ${update.songId}")
                    } catch (e: Exception) {
                        Log.e("Repository", "Failed to sync pending update for song ${update.songId}", e)
                    }
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val result = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        Log.d("Repository", "Network available check: $result, network=$network, capabilities=$capabilities")
        return result
    }


}

