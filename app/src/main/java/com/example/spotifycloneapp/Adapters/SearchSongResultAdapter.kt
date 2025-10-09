package com.example.spotifycloneapp.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spotifycloneapp.R
import com.example.spotifycloneapp.bindingclassess.AdapterClassData
import com.example.spotifycloneapp.databinding.SearchSongResultBinding

class SearchSongResultAdapter(
): ListAdapter<AdapterClassData, SearchSongResultAdapter.ViewHolder>(SearchSongResultDiffUtilsCallback()) {

    private var isPlaying = false
    private var currentPlayingSongId: String? = null

    fun updatePlaybackState(playing: Boolean, songId: String?) { // call later from lib
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

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        var binding = SearchSongResultBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    inner class ViewHolder(val binding: SearchSongResultBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(song: AdapterClassData){
            binding.songData=song
            binding.isPlaying = (song.mediaId == currentPlayingSongId && isPlaying)

            Glide.with(itemView.context)
                .load(song.coverPath)
                .placeholder(R.drawable.spotify_icon_foreground)
                .into(binding.songimage)

            binding.executePendingBindings()
        }
    }

    class SearchSongResultDiffUtilsCallback : DiffUtil.ItemCallback<AdapterClassData>(){
        override fun areItemsTheSame(
            oldItem: AdapterClassData,
            newItem: AdapterClassData
        ): Boolean {
            return oldItem.mediaId==newItem.mediaId
        }

        override fun areContentsTheSame(
            oldItem: AdapterClassData,
            newItem: AdapterClassData
        ): Boolean {
            return oldItem==newItem
        }
    }

}