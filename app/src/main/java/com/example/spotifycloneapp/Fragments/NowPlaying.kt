package com.example.spotifycloneapp.Fragments

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.spotifycloneapp.R
import com.example.spotifycloneapp.ViewModels.NowPlayingViewModel
import com.example.spotifycloneapp.databinding.FragmentNowPlayingBinding
import dagger.hilt.android.AndroidEntryPoint

// Add this annotation so Hilt can inject the ViewModel
@AndroidEntryPoint
class NowPlaying : Fragment() {

    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!

    // Get the Hilt ViewModel
    private val viewModel: NowPlayingViewModel by viewModels()

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
        viewModel.currentMetadata.observe(viewLifecycleOwner) { metadata ->
            updateMetadataUI(metadata)
        }
        viewModel.playbackState.observe(viewLifecycleOwner) { state ->
            updatePlaybackStateUI(state)
        }
        viewModel.currentPosition.observe(viewLifecycleOwner) { position ->
            if (!isUserSeeking) {
                binding.seekBar.progress = position.toInt()
            }
        }
        // Observe the new LiveData for the liked status
        viewModel.isCurrentSongLiked.observe(viewLifecycleOwner) { isLiked ->
            val icon = if (isLiked) R.drawable.fav2 else R.drawable.fav
            binding.ibLikeSong.setImageResource(icon)
        }
    }

    private fun setupClickListeners() {
        binding.miniPlayerTouchAnchor.setOnClickListener {
            binding.rootLayout.transitionToEnd()
        }
        binding.ibDownArrow.setOnClickListener {
            binding.rootLayout.transitionToStart()
        }

        binding.ibPlayPauseMini.setOnClickListener { viewModel.playPause() }
        binding.ibPlayPauseFull.setOnClickListener { viewModel.playPause() }
        binding.ibSkipNextFull.setOnClickListener { viewModel.skipToNext() }
        binding.ibSkipPreviousFull.setOnClickListener { viewModel.skipToPrevious() }

        // Add a click listener for the new like button in the expanded player
        binding.ibLikeSong.setOnClickListener {
            viewModel.toggleLikeForCurrentSong()
        }
    }

    private fun updateMetadataUI(metadata: MediaMetadataCompat?) {
        if (metadata == null) return

        // Get all the data. The 'duration' is correct from the MusicService fix.
        val title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        val artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
        val artUri = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
        val duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)

        // --- Update UI for BOTH players ---

        // Update the small player views
        binding.tvTrackTitleMini.text = title
        binding.tvTrackArtistMini.text = artist

        // THIS IS THE FIX: Update the NEW big player views
        binding.tvTrackTitleFull.text = title
        binding.tvTrackArtistFull.text = artist

        // This fixes the slider
        binding.seekBar.max = if (duration > 0) duration.toInt() else 0

        // This will update the album art
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
                    viewModel.seekTo(it.progress.toLong())
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
