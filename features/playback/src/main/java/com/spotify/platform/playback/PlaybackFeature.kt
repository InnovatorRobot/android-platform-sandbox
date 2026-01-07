package com.spotify.platform.playback

import com.spotify.platform.core.Result
import com.spotify.platform.nativebridge.NativeEngine
import com.spotify.platform.services.PlatformService
import com.spotify.platform.state.StateObserver
import com.spotify.platform.state.StateHolder

/**
 * Playback feature module.
 * 
 * This demonstrates:
 * - Feature isolation (no dependencies on other features)
 * - Dependency on platform interfaces only
 * - Clean separation of concerns
 */
class PlaybackFeature(
    private val nativeEngine: NativeEngine
) : PlatformService, StateObserver<PlaybackFeature.State> {

    data class State(
        val trackId: String? = null,
        val playbackState: NativeEngine.PlaybackState = NativeEngine.PlaybackState.Idle
    )

    private val stateHolder = StateHolder(State())

    override fun start() {
        // Feature initialization
        stateHolder.observe(this)
    }

    override fun stop() {
        stateHolder.removeObserver(this)
    }

    override fun onStateChanged(state: State) {
        // Handle state changes
    }

    fun playTrack(trackId: String): Result<Unit> {
        return nativeEngine.loadTrack(trackId)
            .onSuccess {
                nativeEngine.play()
            }
    }

    fun pause(): Result<Unit> {
        return nativeEngine.pause()
    }

    fun resume(): Result<Unit> {
        return nativeEngine.play()
    }

    fun stop(): Result<Unit> {
        return nativeEngine.stopPlayback()
    }

    fun getCurrentState(): State {
        return stateHolder.state
    }
}

