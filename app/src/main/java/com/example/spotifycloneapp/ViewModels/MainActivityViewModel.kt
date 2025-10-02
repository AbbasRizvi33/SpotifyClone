package com.example.spotifycloneapp.ViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotifycloneapp.EventsClasses.MainActivityUIEvents
import com.example.spotifycloneapp.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class MainActivityViewModel: ViewModel() {

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




}