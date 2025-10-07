package com.example.spotifycloneapp.Fragments

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spotifycloneapp.Adapters.LikedSongsAdapter
import com.example.spotifycloneapp.EventsClasses.HomeUIEvents
import com.example.spotifycloneapp.EventsClasses.RecieveEvents
import com.example.spotifycloneapp.R
import com.example.spotifycloneapp.Services.MusicService
import com.example.spotifycloneapp.ViewModels.SharedViewModel
import com.example.spotifycloneapp.bindingclassess.SongData
import com.example.spotifycloneapp.databinding.FragmentLibraryBinding
import com.example.spotifycloneapp.databinding.FragmentNowPlayingBinding
import kotlinx.coroutines.launch


class Library : Fragment() {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private val sharedvm : SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        binding.likedsongsrv.layoutManager = LinearLayoutManager(requireContext())
        binding.likedsongsrv.adapter = adapter
        return binding.root
    }

    private val adapter: LikedSongsAdapter by lazy {
        LikedSongsAdapter(
            onPlaySongClick = { displaySong ->
                val isCurrentlyPlaying = sharedvm.state.value?.state == PlaybackStateCompat.STATE_PLAYING
                val isThisThePlayingSong = sharedvm.metadata.value?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) == displaySong.mediaId

                if (isCurrentlyPlaying && isThisThePlayingSong) {
                    sharedvm.pause()
                } else {
                    val temp = SongData(
                        title = displaySong.title,
                        coverPath = displaySong.coverPath,
                        filePath = displaySong.filePath
                    )
                    sharedvm.playReqSong(temp)
                }
            },
            onLikeSongCLick = { song ->
                sharedvm.unLikeSong(song.mediaId.toInt())

            }
        )
    }



//    override fun onStart() {
//        super.onStart()
////        sharedvm.showLikedSongs()
//    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedvm.likedSongs.observe(viewLifecycleOwner) { songsList ->
            adapter.submitList(songsList)
        }

        sharedvm.LikedSongsEvents.observe(viewLifecycleOwner) { event ->
            when (event) {
                is RecieveEvents.Success -> {
                    // binding.loadingIndicator.visibility = View.GONE
//                    adapter.submitList(event.songs)
                }
                is RecieveEvents.Empty -> {
                    // binding.loadingIndicator.visibility = View.GONE
//                    adapter.submitList(emptyList())
                }
                is RecieveEvents.Error -> {
                    // binding.loadingIndicator.visibility = View.GONE
//                    adapter.submitList(emptyList())
                }
                is RecieveEvents.Loading -> {
                    // binding.loadingIndicator.visibility = View.VISIBLE
                }
                else -> {}
            }
        }
//        observeEvents()

        val updateAdapterState = {
            val isPlaying = sharedvm.state.value?.state == PlaybackStateCompat.STATE_PLAYING
            val currentSongId = sharedvm.metadata.value?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            adapter.updatePlaybackState(isPlaying, currentSongId)
        }

        sharedvm.state.observe(viewLifecycleOwner) {
            updateAdapterState()
        }
        sharedvm.metadata.observe(viewLifecycleOwner) {
            updateAdapterState()
        }
    }


//    private fun observeEvents(){
//        lifecycleScope.launch {
//            sharedvm.LikedSongsEvents.collect { events ->
//                when(events){
//                    is RecieveEvents.Success -> {
//                        adapter.submitList(events.songs)
//                    }
//                    is RecieveEvents.Error -> {
//                        adapter.submitList(emptyList())
//                    }
//                    else -> {
//                        //loading
//                    }
//                }
//            }
//        }
//    }



    override fun onDestroy() {
        super.onDestroy()
        _binding=null
    }
}