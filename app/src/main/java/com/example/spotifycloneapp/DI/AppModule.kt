package com.example.spotifycloneapp.DI

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.spotifycloneapp.Data.PendingUpdateDao
import com.example.spotifycloneapp.Data.RoomDb
import com.example.spotifycloneapp.Data.SongDao
import com.example.spotifycloneapp.Data.SupabaseSource
import com.example.spotifycloneapp.Repos.Repository
import com.google.android.exoplayer2.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
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
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSongDao(db: RoomDb): SongDao = db.songDao()
    @Provides
    fun providePendingUpdateDao(db: RoomDb): PendingUpdateDao = db.pendingUpdateDao()

    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            setHandleAudioBecomingNoisy(true)
            setWakeMode(com.google.android.exoplayer2.C.WAKE_MODE_NETWORK)
        }
    }

    @Provides
    @Singleton
    fun provideRepository(
        songDao: SongDao,
        supabaseSource: SupabaseSource,
        pendingUpdateDao: PendingUpdateDao,
        @ApplicationContext context: Context
    ): Repository {
        return Repository(songDao, supabaseSource, pendingUpdateDao, context)
    }
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        val supabaseUrl = "https://voyhwvhdizltcgirsfqk.supabase.co"
        val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZveWh3dmhkaXpsdGNnaXJzZnFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjAwMTI5MjksImV4cCI6MjA3NTU4ODkyOX0.yFunRky1zQ0T_JUNk4zLn7xMMFZDi6k1OoGRc2Z9gz4"
        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Postgrest)
            install(Storage)
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseDatabase(client: SupabaseClient): Postgrest {
        return client.postgrest
    }

    @Provides
    @Singleton
    fun provideSupabaseStorage(client: SupabaseClient): Storage {
        return client.storage
    }

    @Provides
    @Singleton
    fun provideSupabaseSource(
        db: Postgrest,
        storage: Storage,
        @ApplicationContext context: Context
    ): SupabaseSource {
        return SupabaseSource(db, storage, context)
    }


}