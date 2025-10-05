package com.example.spotifycloneapp.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spotifycloneapp.Adapters.CategoryAdapter
import com.example.spotifycloneapp.EventsClasses.HomeUIEvents
import com.example.spotifycloneapp.EventsClasses.RecieveEvents
import com.example.spotifycloneapp.ViewModels.FragmentHomeViewModel
import com.example.spotifycloneapp.bindingclassess.SongCategory
import com.example.spotifycloneapp.bindingclassess.SongData
import com.example.spotifycloneapp.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Home : Fragment() {
    private val viewModel: FragmentHomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding?=null
    private val binding get() = _binding!!
    private val categoryAdapter by lazy {
        CategoryAdapter(
            onSongClick = { song ->
                // handle song click here
            },
            context = requireContext()
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding= FragmentHomeBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.musicCards.layoutManager= LinearLayoutManager(requireContext())
        binding.musicCards.setHasFixedSize(true)
        binding.musicCards.adapter= categoryAdapter

        eventCollector()
        setupChips()

        viewModel.viewEvent(HomeUIEvents.getSongs)
    }

    private fun eventCollector() {
        lifecycleScope.launch {
            viewModel.Events.collect { event ->
                when(event) {
                    is RecieveEvents.Success -> {
                        val categoryMap = event.songs.groupBy { it.category}
                        val categoryList = categoryMap.map { (categoryName, songs) ->
                            SongCategory(
                                categoryName = categoryName,
                                songs = songs.map { songEntity ->
                                    SongData(
                                        title = songEntity.title,
                                        coverPath = songEntity.coverPath!!,
                                        filePath = songEntity.filePath
                                    )
                                }
                            )
                        }

                        categoryAdapter.submitList(categoryList)
                    }
                    is RecieveEvents.Error -> {
                        // TODO: handle error
                    }
                }
            }
        }
    }

    private fun setupChips() {
        binding.allchip.setOnClickListener { viewModel.viewEvent(HomeUIEvents.filterCategory("All")) }
        binding.musicchip.setOnClickListener { viewModel.viewEvent(HomeUIEvents.filterCategory("Music")) }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding=null
    }
}