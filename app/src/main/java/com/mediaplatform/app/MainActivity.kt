package com.mediaplatform.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mediaplatform.library.LibraryFeature
import com.mediaplatform.playback.PlaybackFeature
import com.mediaplatform.nativebridge.NativeEngine
import com.mediaplatform.services.ServiceRegistry

/**
 * Main activity - wires everything together.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var serviceRegistry: ServiceRegistry
    private lateinit var nativeEngine: NativeEngine
    private lateinit var playbackFeature: PlaybackFeature
    private lateinit var libraryFeature: LibraryFeature

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serviceRegistry = ServiceRegistry()
        nativeEngine = NativeEngine()
        serviceRegistry.register(nativeEngine)
        
        playbackFeature = PlaybackFeature(nativeEngine)
        libraryFeature = LibraryFeature()
        serviceRegistry.register(playbackFeature)
        serviceRegistry.register(libraryFeature)
        
        serviceRegistry.startAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceRegistry.stopAll()
    }
}

