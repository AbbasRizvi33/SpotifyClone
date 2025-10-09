package com.example.spotifycloneapp.Data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_updates")
data class PendingUpdate(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val songId: Int,
    val isLiked: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)