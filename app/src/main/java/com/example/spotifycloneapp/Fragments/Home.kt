package com.example.spotifycloneapp.Fragments

import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
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
import com.example.spotifycloneapp.Services.MusicService
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

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaController: MediaControllerCompat? = null
    private val categoryAdapter by lazy {
        CategoryAdapter(
            onSongClick = { song ->
               viewModel.viewEvent(HomeUIEvents.playSong(song))
            },
            context = requireContext()
        )
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: android.support.v4.media.session.PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            // You can update UI here, e.g., play/pause button
            state?.let {
                // Example: Log playback state
                android.util.Log.d("HomeFragment", "Playback state: ${it.state}")
            }
        }

        override fun onMetadataChanged(metadata: android.support.v4.media.MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            metadata?.let {
                // Example: show current song title in a Toast (or update UI)
                val title = it.getString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE)
                android.util.Log.d("HomeFragment", "Current song: $title")
            }
        }
    }


    override fun onStart() {
        super.onStart()
        mediaBrowser = MediaBrowserCompat(
            requireContext(),
            ComponentName(requireContext(), MusicService::class.java),
            object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    // 1️⃣ Create the controller
                    mediaController = MediaControllerCompat(requireContext(), mediaBrowser.sessionToken)

                    // 2️⃣ Set it for the activity
                    MediaControllerCompat.setMediaController(requireActivity(), mediaController)

                    // 3️⃣ Register your callback to listen for playback & metadata changes
                    mediaController?.registerCallback(controllerCallback)

                    // 4️⃣ Optional: pass controller to ViewModel for play/next/prev
                    viewModel.setController(mediaController!!)
                }

            },
            null
        )
        mediaBrowser.connect()
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

    override fun onStop() {
        super.onStop()
        if (MediaControllerCompat.getMediaController(requireActivity()) != null) {
            MediaControllerCompat.getMediaController(requireActivity()).unregisterCallback(controllerCallback)
        }
        mediaBrowser.disconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding=null
    }
}