package com.example.spotifycloneapp.ViewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotifycloneapp.EventsClasses.GeneralEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FragmentHomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _GEvents : MutableSharedFlow<GeneralEvents> = MutableSharedFlow()
    val GEvents get() = _GEvents

    fun viewEvent(event : GeneralEvents){
        viewModelScope.launch {
            when(event){
                is GeneralEvents.isLoading -> showLoader()
                else -> {}
            }
        }
    }
    fun showLoader(){
        //todo
    }

}
