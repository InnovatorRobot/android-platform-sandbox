package com.mediaplatform.library

import com.mediaplatform.core.Result
import com.mediaplatform.services.PlatformService
import com.mediaplatform.state.StateObserver
import com.mediaplatform.state.StateHolder

/**
 * Library feature module.
 */
data class Track(
    val id: String,
    val title: String,
    val artist: String
)

class LibraryFeature : PlatformService, StateObserver<LibraryFeature.State> {

    data class State(
        val tracks: List<Track> = emptyList(),
        val selectedTrack: Track? = null
    )

    private val stateHolder = StateHolder(State())

    override fun start() {
        stateHolder.updateState(
            State(
                tracks = listOf(
                    Track("1", "Song 1", "Artist 1"),
                    Track("2", "Song 2", "Artist 2"),
                    Track("3", "Song 3", "Artist 3")
                )
            )
        )
        stateHolder.observe(this)
    }

    override fun stop() {
        stateHolder.removeObserver(this)
    }

    override fun onStateChanged(state: State) {
        // TODO: implement state change handling
    }

    fun getTracks(): Result<List<Track>> {
        return Result.Success(stateHolder.state.tracks)
    }

    fun selectTrack(track: Track): Result<Unit> {
        stateHolder.updateState(stateHolder.state.copy(selectedTrack = track))
        return Result.Success(Unit)
    }

    fun getSelectedTrack(): Track? {
        return stateHolder.state.selectedTrack
    }
}

