package com.example.spotifycloneapp.Fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spotifycloneapp.Adapters.SearchSongResultAdapter
import com.example.spotifycloneapp.Fragments.Library.ActivityCallbacks
import com.example.spotifycloneapp.R
import com.example.spotifycloneapp.Services.MusicService
import com.example.spotifycloneapp.ViewModels.SharedViewModel
import com.example.spotifycloneapp.bindingclassess.AdapterClassData
import com.example.spotifycloneapp.bindingclassess.DisplaySongData
import com.example.spotifycloneapp.bindingclassess.SongData
import com.example.spotifycloneapp.databinding.FragmentSearchBinding


class Search : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val sharedvm : SharedViewModel by activityViewModels()
    private var activityCallback: ActivityCallbacks? = null
    private lateinit var allSongs : List<DisplaySongData>

    private val searchAdapter: SearchSongResultAdapter by lazy{
        SearchSongResultAdapter()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActivityCallbacks) {
            activityCallback = context
        } else {
            throw RuntimeException("$context must implement ActivityCallbacks")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun handlePLayClick(song: DisplaySongData){
        val isCurrentlyPlaying = sharedvm.state.value?.state == PlaybackStateCompat.STATE_PLAYING
        val isThisThePlayingSong = sharedvm.metadata.value?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) == song.mediaId
        if (isCurrentlyPlaying && isThisThePlayingSong) {
            sharedvm.pause()
        }
        else if(!isCurrentlyPlaying && isThisThePlayingSong){
            sharedvm.resume()
        }
        else {
            sharedvm.playReqSong(song.mediaId, MusicService.MEDIA_ROOT_ID)
        }
    }
    private fun handleLikeCLick(song: DisplaySongData){
        sharedvm.unLikeSong(song.mediaId.toInt())
    }

    private fun setUpListeners(){
        binding.searchbar.setOnClickListener {
            binding.searchv.show()
        }

        binding.searchv.addTransitionListener { searchView, previousState, newState ->
            if (newState == com.google.android.material.search.SearchView.TransitionState.SHOWN || newState == com.google.android.material.search.SearchView.TransitionState.SHOWING) {
                activityCallback?.setBottomBarVisibility(false)
                binding.searchrv.visibility = View.VISIBLE
            } else if (newState == com.google.android.material.search.SearchView.TransitionState.HIDDEN || newState == com.google.android.material.search.SearchView.TransitionState.HIDING) {
                activityCallback?.setBottomBarVisibility(true)
                binding.searchrv.visibility = View.GONE
            }

        }
        binding.searchv.editText.doOnTextChanged {
                text, start, before, count ->
            sharedvm.filterAllSongs(text.toString())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchrv.layoutManager= LinearLayoutManager(requireContext())
        binding.searchrv.adapter=searchAdapter

       sharedvm.filteredSearchedSongs.observe(viewLifecycleOwner){
           item->
           allSongs=item
           if (item.isEmpty()) {
               Log.d("SearchFragment", "Filtered song list is empty.")
           } else {
               Log.d("SearchFragment", "Found ${item.size} filtered songs.")
           }
           var dataToSend = item.map{
               AdapterClassData(
                   mediaId = it.mediaId,
                   title = it.title,
                   artist = it.artist,
                   filePath = it.filePath,
                   category = it.category,
                   coverPath = it.coverPath,
                   isLiked = it.isLiked,
                   onPlaySongClick = { handlePLayClick(it) },
                   onLikeSongClick = {
                       handleLikeCLick(it)
                   }
               )
           }
           searchAdapter.submitList(dataToSend)
           if (dataToSend.isEmpty()) {
               Log.d("SearchFragment", "Adapter list (dataToSend) is empty.")
           } else {
               Log.d("SearchFragment", "Submitting ${dataToSend.size} items to adapter.")
           }
       }
        val updateAdapterState = {
            val isPlaying = sharedvm.state.value?.state == PlaybackStateCompat.STATE_PLAYING
            val currentSongId = sharedvm.metadata.value?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            searchAdapter.updatePlaybackState(isPlaying, currentSongId)
        }

        sharedvm.state.observe(viewLifecycleOwner) {
            updateAdapterState()
        }
        sharedvm.metadata.observe(viewLifecycleOwner) {
            updateAdapterState()
        }

        setUpListeners()


    }

    override fun onDetach() {
        super.onDetach()
        activityCallback = null
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding=null
    }

}