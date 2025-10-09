package com.example.spotifycloneapp.Fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spotifycloneapp.Adapters.LikedSongsAdapter
import com.example.spotifycloneapp.Adapters.SearchSongResultAdapter
import com.example.spotifycloneapp.EventsClasses.RecieveEvents
import com.example.spotifycloneapp.Services.MusicService
import com.example.spotifycloneapp.ViewModels.SharedViewModel
import com.example.spotifycloneapp.bindingclassess.AdapterClassData
import com.example.spotifycloneapp.bindingclassess.DisplaySongData
import com.example.spotifycloneapp.bindingclassess.SongData
import com.example.spotifycloneapp.databinding.FragmentLibraryBinding


class Library : Fragment() {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private val sharedvm : SharedViewModel by activityViewModels()
    private var activityCallback: ActivityCallbacks? = null

    private var likedSongs : List<DisplaySongData> = emptyList()

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
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        binding.likedsongsrv.layoutManager = LinearLayoutManager(requireContext())
        binding.likedsongsrv.adapter = adapter
        binding.searchrv.layoutManager = LinearLayoutManager(requireContext())
        binding.searchrv.adapter = searchAdapter
        return binding.root
    }

    private val adapter: LikedSongsAdapter by lazy {
        LikedSongsAdapter()
    }

    private val searchAdapter: SearchSongResultAdapter by lazy{
        SearchSongResultAdapter()
    }

    interface ActivityCallbacks {
        fun setBottomBarVisibility(isVisible: Boolean)
    }



//    override fun onStart() {
//        super.onStart()
////        sharedvm.showLikedSongs()
//    }

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
            sharedvm.playReqSong(song.mediaId, MusicService.LIKED_SONGS_ROOT_ID)
        }
    }
    private fun handleLikeCLick(song: DisplaySongData){
        sharedvm.unLikeSong(song.mediaId.toInt())
    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedvm.likedSongs.observe(viewLifecycleOwner) { songsList ->
            likedSongs=songsList
            val adapterData = songsList.map { song ->
                AdapterClassData(
                    mediaId = song.mediaId,
                    title = song.title,
                    artist = song.artist,
                    filePath = song.filePath,
                    category = song.category,
                    coverPath = song.coverPath,
                    isLiked = song.isLiked,
                    onPlaySongClick = { handlePLayClick(song) },
                    onLikeSongClick = {
                        handleLikeCLick(song)
                    }
                )
            }
            adapter.submitList(adapterData)
        }

        sharedvm.filteredLikedSongs.observe(viewLifecycleOwner){
            song->
            val dataToBeSent = song.map{
                item->
                AdapterClassData(
                    mediaId = item.mediaId,
                    title = item.title,
                    artist = item.artist,
                    filePath = item.filePath,
                    category = item.category,
                    coverPath = item.coverPath,
                    isLiked = item.isLiked,
                    onPlaySongClick = { handlePLayClick(item) },
                    onLikeSongClick = {
                        handleLikeCLick(item)
                    }
                )
        }
            searchAdapter.submitList(dataToBeSent)
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
            sharedvm.filterSongs(text.toString())
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

//    override fun onPause() {
//        super.onPause()
//        if(binding.searchv.isShowing){
//            binding.searchv.hide()
//        }
//    }

    override fun onDetach() {
        super.onDetach()
        activityCallback = null
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding=null
    }
}