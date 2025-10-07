package com.example.spotifycloneapp.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spotifycloneapp.R
import com.example.spotifycloneapp.bindingclassess.DisplaySongData
import com.example.spotifycloneapp.databinding.SongSearchResultBinding

class LikedSongsSingleItemAdapter(
    private val onPlaySongClick: (DisplaySongData) -> Unit,
    private val onLikeSongCLick: (DisplaySongData) -> Unit
) : ListAdapter<DisplaySongData, LikedSongsSingleItemAdapter.ViewHolder>(LikedSongsDiffUtilsCallback())

{
    inner class ViewHolder(val binding : SongSearchResultBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(song: DisplaySongData){
            binding.songtitle.text = song.title
            binding.songdesc.text = song.artist
            binding.playSong.setOnClickListener { onPlaySongClick(song) }
            binding.likeSong.setOnClickListener { onLikeSongCLick(song) }

            Glide.with(itemView.context)
                .load(song.coverPath)
                .placeholder(R.drawable.spotify_icon_foreground)
                .into(binding.songimage)
            binding.executePendingBindings()
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
        holder.bind(getItem(position))
    }
}
