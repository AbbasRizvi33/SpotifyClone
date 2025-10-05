package com.example.spotifycloneapp.ViewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotifycloneapp.Data.SongEntity
import com.example.spotifycloneapp.EventsClasses.HomeUIEvents
import com.example.spotifycloneapp.EventsClasses.RecieveEvents
import com.example.spotifycloneapp.Repos.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FragmentHomeViewModel @Inject constructor(
    private val repo: Repository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _Events: MutableSharedFlow<RecieveEvents> = MutableSharedFlow<RecieveEvents>()
    val Events get() = _Events

    private var songs: List<SongEntity> = emptyList()

    fun viewEvent(event : HomeUIEvents){
        viewModelScope.launch {
            when(event){
                is HomeUIEvents.getSongs -> getSongs()
                is HomeUIEvents.filterCategory -> filterResults(event.category)
                else -> {}
            }
        }
    }

    private suspend fun getSongs(){
        withContext(Dispatchers.IO){
            repo.getAllSongs().collect { data->
                if(data.isNotEmpty()){
                    songs=data
                    _Events.emit(RecieveEvents.Success(data))
                }
                else{
                    _Events.emit(RecieveEvents.Error("No Data Found"))
                }
            }
        }
    }

    private suspend fun filterResults(category: String) {
        val filtered = if (category.equals("All", true)) songs
        else songs.filter { it.category.equals(category, true) }

        if (filtered.isNotEmpty()) {
            _Events.emit(RecieveEvents.Success(filtered))
        } else {
            _Events.emit(RecieveEvents.Error("No songs found in $category"))
        }
    }



}