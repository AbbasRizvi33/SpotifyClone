package com.example.spotifycloneapp.EventsClasses

import android.view.View

sealed class MainActivityUIEvents {
    class setHolderItem(val position:Int): MainActivityUIEvents()
    class startAnimation(val itemId: Int): MainActivityUIEvents()
}