package com.example.spotifycloneapp.Data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingUpdateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addUpdate(pendingUpdate: PendingUpdate)

    @Query("SELECT * FROM pending_updates ORDER BY timestamp ASC")
    suspend fun getAllPendingUpdates(): List<PendingUpdate>

    @Query("DELETE FROM pending_updates WHERE id = :updateId")
    suspend fun deleteUpdate(updateId: Int)
}