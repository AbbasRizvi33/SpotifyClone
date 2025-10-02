package com.example.spotifycloneapp.Adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.spotifycloneapp.Fragments.Home
import com.example.spotifycloneapp.Fragments.Library
import com.example.spotifycloneapp.Fragments.Search

class FragmentHolderAdapter(fragmentActivity: FragmentActivity): FragmentStateAdapter(fragmentActivity) {
    override fun createFragment(position: Int): Fragment {
       when(position){
           0->return Home()
           1->return Search()
           2->return Library()
           else -> return Home()
       }
    }

    override fun getItemCount(): Int {
        return 3
    }
}