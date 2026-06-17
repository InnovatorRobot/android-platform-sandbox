package com.mediaplatform.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.mediaplatform.app.databinding.ActivityMainBinding
import com.mediaplatform.audio.AudioFeature
import com.mediaplatform.camera.CameraFeature
import com.mediaplatform.filters.FiltersFeature
import com.mediaplatform.filters.FilterType
import com.mediaplatform.nativebridge.AudioProcessorEngine
import com.mediaplatform.nativebridge.ImageProcessorEngine
import com.mediaplatform.services.ServiceRegistry

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var serviceRegistry: ServiceRegistry
    private lateinit var cameraFeature: CameraFeature
    private lateinit var audioFeature: AudioFeature
    private lateinit var filtersFeature: FiltersFeature
    private var imageProcessor: ImageProcessorEngine? = null
    private var audioProcessor: AudioProcessorEngine? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    // FPS tracking
    private var frameCount  = 0
    private var lastFpsTime = System.currentTimeMillis()

    // ── Permission launcher (camera + mic together) ───────────────────────────

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val cameraGranted = results[Manifest.permission.CAMERA] == true
        val audioGranted  = results[Manifest.permission.RECORD_AUDIO] == true
        if (cameraGranted) {
            showCamera()
            startServices(audioGranted)
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFilterChips()

        val cameraOk = hasPermission(Manifest.permission.CAMERA)
        val audioOk  = hasPermission(Manifest.permission.RECORD_AUDIO)

        if (cameraOk) {
            showCamera()
            startServices(audioOk)
        } else {
            showPermissionScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::serviceRegistry.isInitialized) serviceRegistry.stopAll()
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private fun hasPermission(perm: String) =
        ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionScreen() {
        binding.permissionLayout.visibility = View.VISIBLE
        binding.btnGrantPermission.setOnClickListener {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ))
        }
    }

    private fun showCamera() {
        binding.permissionLayout.visibility = View.GONE
    }

    // ── Service init ──────────────────────────────────────────────────────────

    private fun startServices(withAudio: Boolean) {
        serviceRegistry = ServiceRegistry()

        // Image processor (graceful degrade if .so unavailable)
        try {
            val ip = ImageProcessorEngine()
            serviceRegistry.register(ip)
            imageProcessor = ip
        } catch (_: UnsatisfiedLinkError) { }

        // Audio processor (graceful degrade if .so unavailable or no permission)
        if (withAudio) {
            try {
                val ap = AudioProcessorEngine()
                serviceRegistry.register(ap)
                audioProcessor = ap
            } catch (_: UnsatisfiedLinkError) { }
        }

        filtersFeature = FiltersFeature()
        cameraFeature  = CameraFeature(this, this)
        audioFeature   = AudioFeature()

        serviceRegistry.register(filtersFeature)
        serviceRegistry.register(cameraFeature)
        if (withAudio) serviceRegistry.register(audioFeature)

        cameraFeature.onFrameAvailable = { raw -> processAndDisplay(raw) }

        if (withAudio) {
            audioFeature.onAudioFrame = { samples -> processAudio(samples) }
        }

        try {
            serviceRegistry.startAll()
        } catch (e: Throwable) {
            // Native init failed — restart without native processors
            imageProcessor = null
            audioProcessor = null
            serviceRegistry = ServiceRegistry()
            filtersFeature  = FiltersFeature()
            cameraFeature   = CameraFeature(this, this)
            cameraFeature.onFrameAvailable = { raw -> processAndDisplay(raw) }
            serviceRegistry.register(filtersFeature)
            serviceRegistry.register(cameraFeature)
            serviceRegistry.startAll()
        }
    }

    // ── Audio pipeline ────────────────────────────────────────────────────────

    /** Runs on the AudioCapture thread. */
    private fun processAudio(samples: ShortArray) {
        val ap = audioProcessor ?: return

        val db     = ap.computeDb(samples)
        val typeId = ap.filterForDb(db)
        val bands  = ap.computeBands(samples, 16)

        // Find the matching FilterType from the int id
        val filter = FilterType.entries.firstOrNull { it.id == typeId } ?: FilterType.NONE

        mainHandler.post {
            // Auto-select the filter driven by audio level
            if (::filtersFeature.isInitialized) filtersFeature.selectFilter(filter)
            binding.dbText.text   = "%.0f dB".format(db)
            binding.activeFilterLabel.text = "Auto: ${filter.displayName}"
            updateChipAppearance(filter)
            binding.visualizerView.updateBands(bands)
        }
    }

    // ── Frame pipeline ────────────────────────────────────────────────────────

    /** Runs on the CameraX analyser thread. */
    private fun processAndDisplay(raw: Bitmap) {
        val filter    = filtersFeature.currentFilter
        val processor = imageProcessor

        val output: Bitmap = if (processor != null && filter != FilterType.NONE) {
            val result = processor.applyFilter(raw, filter.id)
            if (result is com.mediaplatform.core.Result.Success) result.data else raw
        } else {
            raw
        }

        mainHandler.post {
            binding.frameView.setImageBitmap(output)
            updateFps()
        }
    }

    private fun updateFps() {
        frameCount++
        val now = System.currentTimeMillis()
        if (now - lastFpsTime >= 1000L) {
            binding.fpsText.text = getString(R.string.filter_fps, frameCount)
            frameCount  = 0
            lastFpsTime = now
        }
    }

    // ── Filter chips (manual override when audio unavailable) ─────────────────

    private val chipFilterMap by lazy {
        listOf(
            binding.chipNone      to FilterType.NONE,
            binding.chipGrayscale to FilterType.GRAYSCALE,
            binding.chipBlur      to FilterType.BLUR,
            binding.chipSharpen   to FilterType.SHARPEN,
            binding.chipEdges     to FilterType.EDGE_DETECT
        )
    }

    private fun setupFilterChips() {
        chipFilterMap.forEach { (chip, filter) ->
            chip.setOnClickListener { selectFilter(filter) }
        }
    }

    private fun selectFilter(filter: FilterType) {
        if (::filtersFeature.isInitialized) filtersFeature.selectFilter(filter)
        binding.activeFilterLabel.text = "Filter: ${filter.displayName}"
        updateChipAppearance(filter)
    }

    private fun updateChipAppearance(active: FilterType) {
        chipFilterMap.forEach { (chip, filter) ->
            val selected = filter == active
            chip.chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    if (selected) R.color.chip_selected_bg else R.color.chip_unselected_bg
                )
            )
            chip.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (selected) R.color.chip_text_selected else R.color.chip_text_unselected
                )
            )
        }
    }
}
