package com.mediaplatform.playback

import com.mediaplatform.core.Result
import com.mediaplatform.nativebridge.NativeEngine
import com.mediaplatform.services.PlatformService
import com.mediaplatform.state.StateObserver
import com.mediaplatform.state.StateHolder

/**
 * Playback feature module.
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
        stateHolder.observe(this)
    }

    override fun stop() {
        stateHolder.removeObserver(this)
    }

    override fun onStateChanged(state: State) {
        // TODO: implement state change handling
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

