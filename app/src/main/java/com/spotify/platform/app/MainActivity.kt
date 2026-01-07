package com.spotify.platform.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.spotify.platform.library.LibraryFeature
import com.spotify.platform.playback.PlaybackFeature
import com.spotify.platform.nativebridge.NativeEngine
import com.spotify.platform.services.ServiceRegistry

/**
 * Main activity - composition root.
 * 
 * This demonstrates:
 * - App module knows nothing about implementation details
 * - Wires features + platform services together
 * - Acts as composition root (clean architecture)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var serviceRegistry: ServiceRegistry
    private lateinit var nativeEngine: NativeEngine
    private lateinit var playbackFeature: PlaybackFeature
    private lateinit var libraryFeature: LibraryFeature

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize platform services
        serviceRegistry = ServiceRegistry()
        nativeEngine = NativeEngine()
        
        // Register services
        serviceRegistry.register(nativeEngine)
        
        // Initialize features (they depend only on platform interfaces)
        playbackFeature = PlaybackFeature(nativeEngine)
        libraryFeature = LibraryFeature()
        
        // Register features as services
        serviceRegistry.register(playbackFeature)
        serviceRegistry.register(libraryFeature)
        
        // Start all services
        serviceRegistry.startAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceRegistry.stopAll()
    }
}

