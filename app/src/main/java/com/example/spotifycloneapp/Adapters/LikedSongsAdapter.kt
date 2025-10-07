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
import com.example.spotifycloneapp.bindingclassess.DisplaySongData
import com.example.spotifycloneapp.databinding.FragmentLibraryBinding
import com.example.spotifycloneapp.databinding.SongSearchResultBinding
import dagger.hilt.android.qualifiers.ApplicationContext

class LikedSongsAdapter(
    private val onPlaySongClick: (DisplaySongData) -> Unit,
    private val onLikeSongCLick: (DisplaySongData) -> Unit,
): ListAdapter<DisplaySongData, LikedSongsAdapter.ViewHolder>(LikedSongsDiffUtilsCallback()){

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
    class LikedSongsDiffUtilsCallback : DiffUtil.ItemCallback<DisplaySongData>(){
        override fun areItemsTheSame(
            oldItem: DisplaySongData,
            newItem: DisplaySongData
        ): Boolean {
            return oldItem.mediaId == newItem.mediaId
        }

        override fun areContentsTheSame(
            oldItem: DisplaySongData,
            newItem: DisplaySongData
        ): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(val binding : SongSearchResultBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(song: DisplaySongData, isThisSongPlaying: Boolean){
            binding.songtitle.text = song.title
            binding.songdesc.text = song.artist
            binding.playSong.setOnClickListener { onPlaySongClick(song) }
            binding.likeSong.setOnClickListener { onLikeSongCLick(song) }

            if (isThisSongPlaying) {
                binding.playSong.setImageResource(R.drawable.ic_pause)
            } else {
                binding.playSong.setImageResource(R.drawable.ic_play)
            }

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
        val isThisThePlayingSong = isPlaying && song.mediaId == currentPlayingSongId
        holder.bind(song, isThisThePlayingSong)
    }

}