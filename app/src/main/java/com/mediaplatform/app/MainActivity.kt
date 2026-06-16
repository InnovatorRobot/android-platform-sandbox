package com.mediaplatform.app

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mediaplatform.app.databinding.ActivityMainBinding
import com.mediaplatform.core.Result
import com.mediaplatform.library.LibraryFeature
import com.mediaplatform.library.Track
import com.mediaplatform.nativebridge.NativeEngine
import com.mediaplatform.playback.PlaybackFeature
import com.mediaplatform.services.ServiceRegistry

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var serviceRegistry: ServiceRegistry
    private var playbackFeature: PlaybackFeature? = null
    private lateinit var libraryFeature: LibraryFeature
    private var nativeEngineReady = false
    private var selectedTrack: Track? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        initServices()
        updateStatusChips()
        loadLibraryTracks()
        setupPlaybackControls()
    }

    // ── Service initialisation ─────────────────────────────────────────────

    private fun initServices() {
        serviceRegistry = ServiceRegistry()

        // NativeEngine requires a compiled .so; fall back to demo mode gracefully.
        try {
            val engine = NativeEngine()
            val pf = PlaybackFeature(engine)
            serviceRegistry.register(engine)
            serviceRegistry.register(pf)
            playbackFeature = pf
            nativeEngineReady = true
        } catch (e: UnsatisfiedLinkError) {
            nativeEngineReady = false
        }

        libraryFeature = LibraryFeature()
        serviceRegistry.register(libraryFeature)
        serviceRegistry.startAll()
    }

    // ── Status chips ───────────────────────────────────────────────────────

    private fun updateStatusChips() {
        if (nativeEngineReady) {
            binding.chipNativeEngine.text = "● Native Engine: Active"
            binding.chipNativeEngine.chipBackgroundColor =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.chip_engine_active))
            binding.chipNativeEngine.setTextColor(
                ContextCompat.getColor(this, R.color.white)
            )
        } else {
            binding.chipNativeEngine.text = "● Native Engine: Demo"
            binding.chipNativeEngine.chipBackgroundColor =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.chip_engine_unavailable))
            binding.chipNativeEngine.setTextColor(
                ContextCompat.getColor(this, R.color.white)
            )
        }
        // Services and isolation chips are always visible with fixed colours from XML.
        binding.chipServices.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.chipIsolation.setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    // ── Library ────────────────────────────────────────────────────────────

    private fun loadLibraryTracks() {
        val result = libraryFeature.getTracks()
        if (result !is Result.Success) return
        val tracks = result.data

        if (tracks.size >= 1) {
            binding.track1Title.text = tracks[0].title
            binding.track1Artist.text = tracks[0].artist
            binding.trackRow1.setOnClickListener { selectTrack(tracks[0]) }
        }
        if (tracks.size >= 2) {
            binding.track2Title.text = tracks[1].title
            binding.track2Artist.text = tracks[1].artist
            binding.trackRow2.setOnClickListener { selectTrack(tracks[1]) }
        }
        if (tracks.size >= 3) {
            binding.track3Title.text = tracks[2].title
            binding.track3Artist.text = tracks[2].artist
            binding.trackRow3.setOnClickListener { selectTrack(tracks[2]) }
        }
    }

    private fun selectTrack(track: Track) {
        selectedTrack = track
        binding.nowPlayingTitle.text = track.title
        binding.nowPlayingArtist.text = track.artist

        if (nativeEngineReady) {
            val ok = playbackFeature?.playTrack(track.id) is Result.Success
            updatePlaybackStateChip(if (ok) "Playing" else "Error")
        } else {
            // Demo mode: reflect playing state without real audio.
            updatePlaybackStateChip("Playing")
            Toast.makeText(this, "Demo: Now playing ${track.title}", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Playback controls ──────────────────────────────────────────────────

    private fun setupPlaybackControls() {
        binding.btnPlay.setOnClickListener {
            if (selectedTrack == null) {
                Toast.makeText(this, "Select a track from Your Library first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (nativeEngineReady) {
                playbackFeature?.resume()
            }
            updatePlaybackStateChip("Playing")
        }

        binding.btnPause.setOnClickListener {
            if (selectedTrack == null) return@setOnClickListener
            if (nativeEngineReady) {
                playbackFeature?.pause()
            }
            updatePlaybackStateChip("Paused")
        }

        binding.btnStop.setOnClickListener {
            if (selectedTrack == null) return@setOnClickListener
            if (nativeEngineReady) {
                playbackFeature?.stopPlayback()
            }
            updatePlaybackStateChip("Idle")
        }
    }

    // ── Playback state chip ────────────────────────────────────────────────

    private fun updatePlaybackStateChip(state: String) {
        binding.chipPlaybackState.text = when (state) {
            "Playing" -> "▶  Playing"
            "Paused"  -> "⏸  Paused"
            "Error"   -> "✕  Error"
            else      -> "○  Idle"
        }
        val (bgColor, textColor) = when (state) {
            "Playing" -> R.color.chip_playing_bg to R.color.text_on_playing
            "Paused"  -> R.color.chip_paused_bg  to R.color.text_on_paused
            else      -> R.color.chip_idle_bg    to R.color.text_secondary
        }
        binding.chipPlaybackState.chipBackgroundColor =
            ColorStateList.valueOf(ContextCompat.getColor(this, bgColor))
        binding.chipPlaybackState.setTextColor(
            ContextCompat.getColor(this, textColor)
        )
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        serviceRegistry.stopAll()
    }
}

