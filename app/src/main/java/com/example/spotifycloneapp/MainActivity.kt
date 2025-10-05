package com.example.spotifycloneapp

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
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
import android.app.NotificationChannel
import android.app.NotificationManager

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()

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
//        createNotificationChannel()
        init()
        listenViewModel()


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
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                "default_channel",
//                "Music Playback",
//                NotificationManager.IMPORTANCE_LOW
//            ).apply {
//                description = "Channel for SpotifyClone music playback notifications"
//            }
//
//            val manager = getSystemService(NotificationManager::class.java)
//            manager.createNotificationChannel(channel)
//        }
//    }

    fun listenViewModel(){
        lifecycleScope.launch {
            viewModel.UIEvents.collect {
                it->when(it){
                    is MainActivityUIEvents.setHolderItem->binding.fragmentHolder.currentItem=it.position
//                is MainActivityUIEvents.startAnimation->{
//                    val iconView = binding.btmnav.findViewById<View>(it.itemId)
//                    iconView?.bounceAnimation()
//                }
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
//            iconView?.animation=anim
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

    private fun init(){
        binding.fragmentHolder.adapter= FragmentHolderAdapter(this)
        binding.fragmentHolder.isUserInputEnabled=false
       setupBtmNavFragSync()
        viewModel.preloadSongsOnce()


    }
}