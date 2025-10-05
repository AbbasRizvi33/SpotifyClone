package com.example.spotifycloneapp.DI

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.spotifycloneapp.Data.RoomDb
import com.example.spotifycloneapp.Data.SongDao
import com.example.spotifycloneapp.Repos.Repository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RoomDb {
        return Room.databaseBuilder(
            context,
            RoomDb::class.java,
            "songs_db"
        ).build()
    }

    @Provides
    fun provideSongDao(db: RoomDb): SongDao = db.songDao()


}