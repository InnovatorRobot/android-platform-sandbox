package com.mediaplatform.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.mediaplatform.services.PlatformService
import java.util.concurrent.Executors

/**
 * Manages the CameraX pipeline and delivers decoded Bitmap frames to whoever registers
 * [onFrameAvailable].
 *
 * Frame delivery happens on a background thread — callers must post UI updates back to the main
 * thread themselves.
 */
class CameraFeature(private val context: Context, private val lifecycleOwner: LifecycleOwner) :
        PlatformService {

    /** Called for every decoded camera frame. Invoked on a background thread. */
    var onFrameAvailable: ((Bitmap) -> Unit)? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    override fun start() {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
                {
                    cameraProvider = future.get()
                    bindCamera()
                },
                ContextCompat.getMainExecutor(context)
        )
    }

    override fun stop() {
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
    }

    private fun bindCamera() {
        val imageAnalysis =
                ImageAnalysis.Builder()
                        .setTargetResolution(android.util.Size(960, 540))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()

        imageAnalysis.setAnalyzer(analysisExecutor) { proxy ->
            try {
                val bitmap = proxy.toRotatedBitmap()
                onFrameAvailable?.invoke(bitmap)
            } finally {
                proxy.close()
            }
        }

        cameraProvider?.unbindAll()
        cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                imageAnalysis
        )
    }

    // ── ImageProxy → Bitmap ───────────────────────────────────────────────────

    private fun ImageProxy.toRotatedBitmap(): Bitmap {
        // CameraX delivers RGBA_8888 frames, so toBitmap() gives us a clean,
        // artifact-free bitmap with no JPEG round-trip or manual YUV packing.
        val bitmap = toBitmap()
        val rotation = imageInfo.rotationDegrees
        return if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }
}
