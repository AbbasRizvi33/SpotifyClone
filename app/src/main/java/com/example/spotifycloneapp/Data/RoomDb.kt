package com.example.spotifycloneapp.Data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SongEntity::class, PendingUpdate::class], version = 1, exportSchema = false)
abstract class RoomDb: RoomDatabase(){

    abstract fun songDao(): SongDao
    abstract fun pendingUpdateDao(): PendingUpdateDao
}