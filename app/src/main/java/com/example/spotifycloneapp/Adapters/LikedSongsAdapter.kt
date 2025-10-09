package com.example.spotifycloneapp.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spotifycloneapp.R
import com.example.spotifycloneapp.bindingclassess.AdapterClassData
import com.example.spotifycloneapp.bindingclassess.DisplaySongData
import com.example.spotifycloneapp.databinding.FragmentLibraryBinding
import com.example.spotifycloneapp.databinding.SongSearchResultBinding
import dagger.hilt.android.qualifiers.ApplicationContext

class LikedSongsAdapter(
): ListAdapter<AdapterClassData, LikedSongsAdapter.ViewHolder>(LikedSongsDiffUtilsCallback()){
    private var isPlaying = false
    private var currentPlayingSongId: String? = null
    fun updatePlaybackState(playing: Boolean, songId: String?) {
        val oldPlayingSongId = currentPlayingSongId

        isPlaying = playing
        currentPlayingSongId = songId
        val oldPosition = currentList.indexOfFirst { it.mediaId == oldPlayingSongId }
        val newPosition = currentList.indexOfFirst { it.mediaId == currentPlayingSongId }
        if (oldPosition != RecyclerView.NO_POSITION && oldPosition != newPosition) {
            notifyItemChanged(oldPosition)
        }

        if (newPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(newPosition)
        }
    }
    class LikedSongsDiffUtilsCallback : DiffUtil.ItemCallback<AdapterClassData>(){
        override fun areItemsTheSame(
            oldItem: AdapterClassData,
            newItem: AdapterClassData
        ): Boolean {
            return oldItem.mediaId == newItem.mediaId
        }

        override fun areContentsTheSame(
            oldItem: AdapterClassData,
            newItem: AdapterClassData
        ): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(val binding : SongSearchResultBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(song: AdapterClassData){
            binding.songData = song

            binding.isPlaying = (song.mediaId == currentPlayingSongId && isPlaying)

            Glide.with(itemView.context)
                .load(song.coverPath)
                .placeholder(R.drawable.spotify_icon_foreground)
                .into(binding.songimage)
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = SongSearchResultBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val song = getItem(position)
        holder.bind(song)
    }

}