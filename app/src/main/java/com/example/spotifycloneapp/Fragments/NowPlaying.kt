package com.example.spotifycloneapp.Fragments

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.spotifycloneapp.R
import com.example.spotifycloneapp.ViewModels.NowPlayingViewModel
import com.example.spotifycloneapp.ViewModels.SharedViewModel
import com.example.spotifycloneapp.databinding.FragmentNowPlayingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class NowPlaying : Fragment() {

    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NowPlayingViewModel by viewModels()
    private val sharedvm: SharedViewModel by activityViewModels()

    private var isUserSeeking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNowPlayingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupSeekBar()
        observeViewModel()
    }

    private fun observeViewModel() {
        sharedvm.metadata.observe(viewLifecycleOwner) { metadata ->
            updateMetadataUI(metadata)
        }
        sharedvm.state.observe(viewLifecycleOwner) { state ->
            updatePlaybackStateUI(state)
        }
        viewModel.currentPosition.observe(viewLifecycleOwner) { position ->
            if (!isUserSeeking) {
                binding.seekBar.progress = position.toInt()
            }
        }

        sharedvm.currentPosition.observe(viewLifecycleOwner) { position ->
            // The position can be null if playback hasn't started
            if (!isUserSeeking && position != null) {
                binding.seekBar.progress = position.toInt()
            }
        }
//        viewModel.isCurrentSongLiked.observe(viewLifecycleOwner) { isLiked ->
//            val icon = if (isLiked) R.drawable.fav2 else R.drawable.fav
//            binding.ibLikeSong.setImageResource(icon)
//        }
    }

    private fun setupClickListeners() {
        binding.miniPlayerTouchAnchor.setOnClickListener {
            binding.rootLayout.transitionToEnd()
        }
        binding.ibDownArrow.setOnClickListener {
            binding.rootLayout.transitionToStart()
        }

        binding.ibPlayPauseMini.setOnClickListener { sharedvm.playPause() }
        binding.ibPlayPauseFull.setOnClickListener { sharedvm.playPause() }
        binding.ibSkipNextFull.setOnClickListener { sharedvm.skipToNext() }
        binding.ibSkipPreviousFull.setOnClickListener { sharedvm.skipToPrevious() }

        binding.ibLikeSong.setOnClickListener {
            sharedvm.toggleLikeForCurrentSong()
        }
    }

    private fun updateMetadataUI(metadata: MediaMetadataCompat?) {
        if (metadata == null) return

        val title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        val artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
        val artUri = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
        val duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        binding.tvTrackTitleMini.text = title
        binding.tvTrackArtistMini.text = artist

        binding.tvTrackTitleFull.text = title
        binding.tvTrackArtistFull.text = artist

        val isLiked:Boolean = metadata.getString("isLiked").toBoolean()
        val icon = if (isLiked) R.drawable.fav2 else R.drawable.fav
        binding.ibLikeSong.setImageResource(icon)
//        binding.ibLikeSongMini.setImageResource(icon)

        binding.seekBar.max = if (duration > 0) duration.toInt() else 0

        Glide.with(this)
            .load(artUri)
            .placeholder(R.drawable.spotify_icon_foreground)
            .error(R.drawable.spotify_icon_foreground)
            .into(binding.ivAlbumArtMini)

        // I assume your MotionLayout is animating 'ivAlbumArtMini' to a bigger size.
        // If you have a second, separate ImageView for the full player (like 'ivAlbumArtFull'),
        // you would add a Glide call for it here. If not, this is all you need for the art.
    }





    private fun updatePlaybackStateUI(state: PlaybackStateCompat?) {
        val isPlaying = state?.state == PlaybackStateCompat.STATE_PLAYING
        val iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play

        binding.ibPlayPauseMini.setImageResource(iconRes)
        binding.ibPlayPauseFull.setImageResource(iconRes)
    }

    private fun setupSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    sharedvm.seekTo(it.progress.toLong())
                }
                isUserSeeking = false
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
