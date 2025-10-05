package com.example.spotifycloneapp.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.spotifycloneapp.bindingclassess.SongData
import com.example.spotifycloneapp.databinding.SingleCategoryItemBinding

class SongPagerAdapter(
    private val onSongClick: (SongData) -> Unit
): ListAdapter<SongData, SongPagerAdapter.SongViewHolder>(SongPagerDiffUtilsCallback()) {

    inner class SongViewHolder(val binding: SingleCategoryItemBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(song: SongData) {
            binding.song = song
            binding.root.setOnClickListener { onSongClick(song) }
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SongViewHolder {
        val binding= SingleCategoryItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SongViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }


    private class SongPagerDiffUtilsCallback() : DiffUtil.ItemCallback<SongData>(){
        override fun areItemsTheSame(
            oldItem: SongData,
            newItem: SongData
        ): Boolean {
            return oldItem.title==newItem.title
        }

        override fun areContentsTheSame(
            oldItem: SongData,
            newItem: SongData
        ): Boolean {
            return oldItem==newItem
        }
    }
}