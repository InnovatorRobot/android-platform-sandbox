package com.mediaplatform.app

import android.Manifest
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
import com.mediaplatform.camera.CameraFeature
import com.mediaplatform.filters.FiltersFeature
import com.mediaplatform.filters.FilterType
import com.mediaplatform.nativebridge.ImageProcessorEngine
import com.mediaplatform.services.ServiceRegistry

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var serviceRegistry: ServiceRegistry
    private lateinit var cameraFeature: CameraFeature
    private lateinit var filtersFeature: FiltersFeature
    private var imageProcessor: ImageProcessorEngine? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    // FPS tracking
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()

    // ── Permission launcher ───────────────────────────────────────────────────

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showCamera()
            startServices()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show()
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFilterChips()

        if (hasCameraPermission()) {
            showCamera()
            startServices()
        } else {
            showPermissionScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceRegistry.stopAll()
    }

    // ── Permission ────────────────────────────────────────────────────────────

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun showPermissionScreen() {
        binding.permissionLayout.visibility = View.VISIBLE
        binding.btnGrantPermission.setOnClickListener {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showCamera() {
        binding.permissionLayout.visibility = View.GONE
    }

    // ── Service init ──────────────────────────────────────────────────────────

    private fun startServices() {
        serviceRegistry = ServiceRegistry()

        // Native image processor — gracefully degrade if .so unavailable
        try {
            val processor = ImageProcessorEngine()
            serviceRegistry.register(processor)
            imageProcessor = processor
        } catch (e: UnsatisfiedLinkError) {
            // Demo mode: filters show on UI but no actual pixel processing
        }

        filtersFeature = FiltersFeature()
        cameraFeature  = CameraFeature(this, this)
        serviceRegistry.register(filtersFeature)
        serviceRegistry.register(cameraFeature)

        // Deliver each camera frame through the C++ filter, then show on screen
        cameraFeature.onFrameAvailable = { raw -> processAndDisplay(raw) }

        try {
            serviceRegistry.startAll()
        } catch (e: Throwable) {
            // If native init fails, restart without the image processor
            imageProcessor = null
            serviceRegistry = ServiceRegistry()
            filtersFeature  = FiltersFeature()
            cameraFeature   = CameraFeature(this, this)
            cameraFeature.onFrameAvailable = { raw -> processAndDisplay(raw) }
            serviceRegistry.register(filtersFeature)
            serviceRegistry.register(cameraFeature)
            serviceRegistry.startAll()
        }
    }

    // ── Frame processing ──────────────────────────────────────────────────────

    private fun processAndDisplay(raw: Bitmap) {
        val filter     = filtersFeature.currentFilter
        val processor  = imageProcessor

        val output: Bitmap = if (processor != null && filter != FilterType.NONE) {
            val result = processor.applyFilter(raw, filter.id)
            if (result is com.mediaplatform.core.Result.Success) result.data else raw
        } else {
            raw
        }

        // Update UI on main thread
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

    // ── Filter chips ──────────────────────────────────────────────────────────

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
        if (::filtersFeature.isInitialized) {
            filtersFeature.selectFilter(filter)
        }
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
