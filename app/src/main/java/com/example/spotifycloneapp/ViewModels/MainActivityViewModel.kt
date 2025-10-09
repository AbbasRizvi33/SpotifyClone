package com.example.spotifycloneapp.ViewModels
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotifycloneapp.Data.SongEntity
import com.example.spotifycloneapp.EventsClasses.MainActivityUIEvents
import com.example.spotifycloneapp.R
import com.example.spotifycloneapp.Repos.Repository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch


@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val repo: Repository, @ApplicationContext private val context: Context
)  : ViewModel() {

    private val _UIEvent= MutableSharedFlow<MainActivityUIEvents>()
    val UIEvents : SharedFlow<MainActivityUIEvents> =_UIEvent

    fun onEvent(event: MainActivityUIEvents){
        viewModelScope.launch {
//            when(event){
//                is MainActivityUIEvents.startAnimation -> {
//                    _UIEvent.emit(MainActivityUIEvents.initiateStartAnimation(event.view))
//                }
//                else -> {}
//            }
        }
    }

    fun navItemSelected(itemId:Int){
        viewModelScope.launch {
            when(itemId){
                R.id.homeid-> {
                    _UIEvent.emit(MainActivityUIEvents.setHolderItem(0))
//                    _UIEvent.emit(MainActivityUIEvents.startAnimation(0))
                }
                R.id.searchid-> {
                    _UIEvent.emit(MainActivityUIEvents.setHolderItem(1))
//                    _UIEvent.emit(MainActivityUIEvents.startAnimation(1))
                }
                R.id.libid-> {
                    _UIEvent.emit(MainActivityUIEvents.setHolderItem(2))
//                    _UIEvent.emit(MainActivityUIEvents.startAnimation(2))
                }
            }
        }

    }

    val allSongs = repo.getAllSongs()

    fun preloadSongsOnce() {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val isPreloaded = prefs.getBoolean("is_preloaded", false)
            if (!isPreloaded) {
                val demoSongs = listOf(
                    SongEntity(
                        title = "12 to 12",
                        artist = "Sombr",
                        category = "Pop",
                        filePath = "android.resource://${context.packageName}/raw/sombr",
                        coverPath = "android.resource://${context.packageName}/drawable/cover1"
                    ),
                    SongEntity(
                        title = "Dracula",
                        artist = "Tame Impala",
                        category = "Rock",
                        filePath = "android.resource://${context.packageName}/raw/dracula",
                        coverPath = "android.resource://${context.packageName}/drawable/cover2"
                    ),
                    SongEntity(
                        title = "Electric",
                        artist = "Alina Baraz",
                        category = "Pop",
                        filePath = "android.resource://${context.packageName}/raw/electric",
                        coverPath = "android.resource://${context.packageName}/drawable/cover2"
                    ),
                    SongEntity(
                        title = "Oh what a drenched man i am",
                        artist = "Suicide Boys",
                        category = "Pop",
                        filePath = "android.resource://${context.packageName}/raw/drechedman",
                        coverPath = "android.resource://${context.packageName}/drawable/cover2"
                    ),
                    SongEntity(
                        title = "Fallen Star",
                        artist = "Neighbourhood",
                        category = "Pop",
                        filePath = "android.resource://${context.packageName}/raw/fallenstar",
                        coverPath = "android.resource://${context.packageName}/drawable/cover2"
                    ),
                    SongEntity(
                        title = "You are the right one",
                        artist = "Sports",
                        category = "Rock",
                        filePath = "android.resource://${context.packageName}/raw/uaretherightone",
                        coverPath = "android.resource://${context.packageName}/drawable/cover2"
                    ),
                    SongEntity(
                        title = "A Little Death",
                        artist = "Neighbourhood",
                        category = "Rock",
                        filePath = "android.resource://${context.packageName}/raw/alildeath",
                        coverPath = "android.resource://${context.packageName}/drawable/cover2"
                    )
                )
                repo.insertSongs(demoSongs)
                prefs.edit().putBoolean("is_preloaded", true).apply()
            }
        }
    }



}