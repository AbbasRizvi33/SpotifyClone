package com.example.spotifycloneapp.bindingclassess

import android.util.Log
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.spotifycloneapp.R
import java.io.File

import android.net.Uri
import kotlin.math.log

@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, path: String?) {
    if (!path.isNullOrEmpty()) {
        val uri = Uri.parse(path)
        Log.d("Image", "loadImage: $uri ")
        Glide.with(view.context)
            .load(uri)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(view)
    } else {
        view.setImageResource(R.drawable.ic_launcher_background)
    }
}


