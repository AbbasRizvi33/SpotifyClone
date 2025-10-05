package com.example.spotifycloneapp.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.spotifycloneapp.bindingclassess.SongCategory
import com.example.spotifycloneapp.bindingclassess.SongData
import com.example.spotifycloneapp.databinding.MusicViewsBinding

class CategoryAdapter(
    private val onSongClick: (SongData) -> Unit,
    private var context: Context
) : ListAdapter<SongCategory, CategoryAdapter.CategoryViewHolder>(CategoryDiffUtilsCallback())
{
    inner class CategoryViewHolder(val binding: MusicViewsBinding): RecyclerView.ViewHolder(binding.root){
        private val songAdapter = SongPagerAdapter(onSongClick)
        init {
            binding.songcontent.layoutManager= LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL,false)
            binding.songcontent.adapter=songAdapter
        }
        fun bind(category: SongCategory) {
            binding.category = category
            songAdapter.submitList(category.songs)
            binding.executePendingBindings()
        }
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryViewHolder {
        val binding= MusicViewsBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CategoryViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }


    private class CategoryDiffUtilsCallback(): DiffUtil.ItemCallback<SongCategory>(){
        override fun areItemsTheSame(
            oldItem: SongCategory,
            newItem: SongCategory
        ): Boolean {
            return oldItem.categoryName==newItem.categoryName
        }

        override fun areContentsTheSame(
            oldItem: SongCategory,
            newItem: SongCategory
        ): Boolean {
           return oldItem==newItem
        }
    }
}