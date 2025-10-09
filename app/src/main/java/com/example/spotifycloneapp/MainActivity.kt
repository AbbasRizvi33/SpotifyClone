package com.example.spotifycloneapp

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.spotifycloneapp.Adapters.FragmentHolderAdapter
import com.example.spotifycloneapp.EventsClasses.MainActivityUIEvents
import com.example.spotifycloneapp.ViewModels.MainActivityViewModel
import com.example.spotifycloneapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.Manifest
import android.content.ComponentName
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.example.spotifycloneapp.Fragments.Library
import com.example.spotifycloneapp.Services.MusicService
import com.example.spotifycloneapp.ViewModels.SharedViewModel
import com.example.spotifycloneapp.bindingclassess.DisplaySongData

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), Library.ActivityCallbacks {
    private lateinit var binding:ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()
    private val sharedvm : SharedViewModel by viewModels()

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaController: MediaControllerCompat? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= DataBindingUtil.setContentView(this,R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        askNotificationPermission()
        init()
        listenViewModel()


    }

    override fun setBottomBarVisibility(isVisible: Boolean) {

        binding.btmnav.visibility = if (isVisible) View.VISIBLE else View.GONE
        binding.nowPlayingFragment.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            sharedvm.updateMetadata(metadata)
        }
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            sharedvm.updatePlaybackState(state)
        }
    }

    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            sharedvm.updatePlaybackState(state)
        }
    }


    private fun init() {
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MusicService::class.java),object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    mediaController = MediaControllerCompat(this@MainActivity, mediaBrowser.sessionToken)
                    MediaControllerCompat.setMediaController(this@MainActivity, this@MainActivity.mediaController)
                    mediaController?.registerCallback(controllerCallback)
                    sharedvm.setController(mediaController!!)
                    sharedvm.updateMetadata(mediaController?.metadata)
                    sharedvm.updatePlaybackState(mediaController?.playbackState)
                    mediaBrowser.subscribe(mediaBrowser.root, object : MediaBrowserCompat.SubscriptionCallback() {
                        override fun onChildrenLoaded(
                            parentId: String,
                            children: MutableList<MediaBrowserCompat.MediaItem>
                        ) {
                            super.onChildrenLoaded(parentId, children)
                            val songs = children.mapNotNull { item ->
                                item?.let {
                                    DisplaySongData(
                                        mediaId = it.mediaId!!,
                                        title = it.description.title?.toString() ?: "Unknown Title",
                                        artist = it.description.subtitle?.toString() ?: "Unknown Artist",
                                        filePath = it.description.mediaUri?.toString() ?: "",
                                        category = it.description.extras?.getString("category") ?: "Music",
                                        coverPath = it.description.extras?.getString("coverPath") ?: "",
                                        isLiked = it.description.extras?.getBoolean("isLiked") ?: false
                                    )
                                }
                            }
                            if (songs.isEmpty()) {
                                // Log an error: This means your MusicService is not returning songs.
                                Log.e("MainActivity", "The master song list returned by MusicService is empty!")
                            } else {
                                Log.d("MainActivity", "Successfully loaded ${songs.size} songs.")
                            }
                            sharedvm.setSongs(songs)
                        }
                    })

                    mediaBrowser.subscribe(MusicService.LIKED_SONGS_ROOT_ID, object : MediaBrowserCompat.SubscriptionCallback() {
                        override fun onChildrenLoaded(
                            parentId: String,
                            children: MutableList<MediaBrowserCompat.MediaItem>
                        ) {
                            super.onChildrenLoaded(parentId, children)
                            val likedSongs = children.mapNotNull { item ->
                                item?.let {
                                    DisplaySongData(
                                        mediaId = it.mediaId!!,
                                        title = it.description.title?.toString() ?: "",
                                        artist = it.description.subtitle?.toString() ?: "",
                                        filePath = it.description.mediaUri?.toString() ?: "",
                                        category = it.description.extras?.getString("category") ?: "",
                                        coverPath = it.description.extras?.getString("coverPath") ?: "",
                                        isLiked = it.description.extras?.getBoolean("isLiked") ?: false
                                    )
                                }
                            }
                            sharedvm.setLikedSongs(likedSongs)
                        }
                    })

                }
            },
            null
        )
        mediaBrowser.connect()

        binding.fragmentHolder.adapter = FragmentHolderAdapter(this)
        binding.fragmentHolder.isUserInputEnabled = false
        setupBtmNavFragSync()
        viewModel.preloadSongsOnce()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }


    fun listenViewModel(){
        lifecycleScope.launch {
            viewModel.UIEvents.collect {
                it->when(it){
                    is MainActivityUIEvents.setHolderItem->binding.fragmentHolder.currentItem=it.position
                else -> {}
                }
            }
        }
    }



    fun setupBtmNavFragSync(){
        binding.btmnav.setOnItemSelectedListener {item ->
            viewModel.navItemSelected(item.itemId)

            val iconView = binding.btmnav.findViewById<View>(item.itemId)
            val anim= AnimationUtils.loadAnimation(this@MainActivity,R.anim.small_bounce)
            iconView.startAnimation(anim)

            true
        }

        binding.fragmentHolder.registerOnPageChangeCallback(object :ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.btmnav.menu.getItem(position).isChecked=true
            }
        })
    }

//    override fun onStart() {
//        super.onStart()
//        if (!mediaBrowser.isConnected) {
//            mediaBrowser.connect()
//        }
//    }

    override fun onStop() {
        super.onStop()
        mediaController?.unregisterCallback(controllerCallback)
        if (mediaBrowser.isConnected) {
            mediaBrowser.disconnect()
        }
    }

}