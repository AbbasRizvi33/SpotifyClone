// In C:/Users/abbas/AndroidStudioProjects/SpotifyCloneApp/app/src/main/java/com/example/spotifycloneapp/Fragments/Home.kt

package com.example.spotifycloneapp.Fragments

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spotifycloneapp.Adapters.CategoryAdapter
import com.example.spotifycloneapp.Services.MusicService
import com.example.spotifycloneapp.ViewModels.SharedViewModel
import com.example.spotifycloneapp.bindingclassess.SongCategory
import com.example.spotifycloneapp.bindingclassess.SongData
import com.example.spotifycloneapp.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Home : Fragment() {
    private val sharedvm: SharedViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val categoryAdapter by lazy {
        CategoryAdapter(
            onSongClick = { song ->
                val isCurrentlyPlaying = sharedvm.state.value?.state == PlaybackStateCompat.STATE_PLAYING
                val isThisThePlayingSong = sharedvm.metadata.value?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) == song.mediaID

                if (isCurrentlyPlaying && isThisThePlayingSong) {
                    sharedvm.pause()
                } else if (!isCurrentlyPlaying && isThisThePlayingSong) {
                    sharedvm.resume()
                } else {
                    sharedvm.playReqSong(song.mediaID, MusicService.MEDIA_ROOT_ID)
                }
            },
            context = requireContext()
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.musicCards.layoutManager = LinearLayoutManager(requireContext())
        binding.musicCards.setHasFixedSize(true)
        binding.musicCards.adapter = categoryAdapter

        sharedvm.songs.observe(viewLifecycleOwner) { allSongs ->
            val categoryMap = allSongs.groupBy { it.category }
            val categoryList = categoryMap.map { (categoryName, songs) ->
                SongCategory(
                    categoryName = categoryName,
                    songs = songs.map { displaySong ->
                        SongData(
                            title = displaySong.title,
                            coverPath = displaySong.coverPath,
                            filePath = displaySong.filePath,
                            mediaID = displaySong.mediaId
                        )
                    }
                )
            }


            categoryAdapter.submitList(categoryList)
        }
        setupChips()
    }


    private fun setupChips() {
        binding.allchip.setOnClickListener {
        }
        binding.musicchip.setOnClickListener {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}