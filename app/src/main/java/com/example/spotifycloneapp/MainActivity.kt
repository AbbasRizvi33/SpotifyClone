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
import android.content.Intent
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.example.spotifycloneapp.Fragments.Library
import com.example.spotifycloneapp.Services.MusicService
import com.example.spotifycloneapp.ViewModels.SharedViewModel
import com.example.spotifycloneapp.bindingclassess.DisplaySongData


private const val TAG = "SpotifyCloneDebug"

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), Library.ActivityCallbacks {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()
    private val sharedvm: SharedViewModel by viewModels()

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaController: MediaControllerCompat? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity: onCreate() called")
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        val intent = Intent(this, MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        askNotificationPermission()
        init()
        listenViewModel()
        observeLoadingState()

    }

    override fun setBottomBarVisibility(isVisible: Boolean) {

        binding.btmnav.visibility = if (isVisible) View.VISIBLE else View.GONE
        binding.nowPlayingFragment.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            Log.d(TAG, "MainActivity → Metadata changed: title=${metadata?.description?.title}")
            sharedvm.updateMetadata(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            Log.d(TAG, "MainActivity → Playback state changed: ${state?.state}")
            sharedvm.updatePlaybackState(state)
        }
    }

    private fun observeLoadingState() {
        sharedvm.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.loadingRoot.visibility = View.VISIBLE
                val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse)
                binding.loadingLogo.startAnimation(pulseAnimation)
            } else {
                binding.loadingLogo.clearAnimation()
                binding.loadingRoot.visibility = View.GONE
            }
        }
    }


    private fun init() {
        Log.d(TAG, "MainActivity → init() called, setting up MediaBrowser")
            mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MusicService::class.java),
            object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    Log.d(TAG, "MainActivity → MediaBrowser connected successfully")
                    val metadata = mediaController?.metadata
                    if (metadata != null) {
                        sharedvm.updateMetadata(metadata)
                    }
                    mediaController =
                        MediaControllerCompat(this@MainActivity, mediaBrowser.sessionToken)
                    MediaControllerCompat.setMediaController(
                        this@MainActivity,
                        this@MainActivity.mediaController
                    )
                    mediaController?.registerCallback(controllerCallback)
                    Log.d(TAG, "MainActivity → MediaController created & callback registered")
                    sharedvm.setController(mediaController!!)
                    sharedvm.updateMetadata(mediaController?.metadata)
                    sharedvm.updatePlaybackState(mediaController?.playbackState)
                    Log.d(TAG, "MainActivity → SharedVM updated with controller info")

                    mediaBrowser.subscribe(
                        mediaBrowser.root,
                        object : MediaBrowserCompat.SubscriptionCallback() {
                            override fun onChildrenLoaded(
                                parentId: String,
                                children: MutableList<MediaBrowserCompat.MediaItem>
                            ) {
                                Log.d(TAG, "MainActivity → onChildrenLoaded() called, got ${children.size} items")
                                super.onChildrenLoaded(parentId, children)
                                val songs = children.mapNotNull { item ->
                                    item?.let {
                                        DisplaySongData(
                                            mediaId = it.mediaId!!,
                                            title = it.description.title?.toString()
                                                ?: "Unknown Title",
                                            artist = it.description.subtitle?.toString()
                                                ?: "Unknown Artist",
                                            filePath = it.description.mediaUri?.toString() ?: "",
                                            category = it.description.extras?.getString("category")
                                                ?: "Music",
                                            coverPath = it.description.extras?.getString("coverPath")
                                                ?: "",
                                            isLiked = it.description.extras?.getBoolean("isLiked")
                                                ?: false
                                        )
                                    }
                                }
                                if (songs.isEmpty()) {
                                    Log.e(
                                        "MainActivity",
                                        "The master song list returned by MusicService is empty"
                                    )
                                } else {
                                    Log.d(
                                        "MainActivity",
                                        "Successfully loaded ${songs.size} songs."
                                    )
                                    sharedvm.setSongs(songs)
                                }

                            }
                        })

                }

                override fun onConnectionSuspended() {
                    super.onConnectionSuspended()
                    Log.w(TAG, "MainActivity → MediaBrowser connection suspended")
                    mediaBrowser.connect()
                }

                override fun onConnectionFailed() {
                    super.onConnectionFailed()
                    Log.e(TAG, "MainActivity → MediaBrowser connection failed — reconnecting")
                    mediaBrowser.connect()
                }
            },
            null
        )
        Log.d(TAG, "MainActivity → connecting MediaBrowser now…")
            mediaBrowser.connect()

        binding.fragmentHolder.adapter = FragmentHolderAdapter(this)
        binding.fragmentHolder.isUserInputEnabled = false
        setupBtmNavFragSync()
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


    fun listenViewModel() {
        lifecycleScope.launch {
            viewModel.UIEvents.collect { it ->
                when (it) {
                    is MainActivityUIEvents.setHolderItem -> binding.fragmentHolder.currentItem =
                        it.position

                    else -> {}
                }
            }
        }
    }

    fun setupBtmNavFragSync() {
        binding.btmnav.setOnItemSelectedListener { item ->
            viewModel.navItemSelected(item.itemId)

            val iconView = binding.btmnav.findViewById<View>(item.itemId)
            val anim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.small_bounce)
            iconView.startAnimation(anim)

            true
        }

        binding.fragmentHolder.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.btmnav.menu.getItem(position).isChecked = true
            }
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity → onDestroy() called. isFinishing=${isFinishing}, mediaBrowserConnected=${mediaBrowser.isConnected}")
        mediaController?.unregisterCallback(controllerCallback)
        if (isFinishing && mediaBrowser.isConnected) {
            mediaBrowser.disconnect()
            Log.d(TAG, "MainActivity → mediaBrowser disconnected manually")
        }
    }

}