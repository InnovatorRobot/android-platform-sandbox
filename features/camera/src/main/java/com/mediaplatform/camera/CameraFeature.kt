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
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * Manages the CameraX pipeline and delivers decoded Bitmap frames
 * to whoever registers [onFrameAvailable].
 *
 * Frame delivery happens on a background thread — callers must post
 * UI updates back to the main thread themselves.
 */
class CameraFeature(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : PlatformService {

    /** Called for every decoded camera frame. Invoked on a background thread. */
    var onFrameAvailable: ((Bitmap) -> Unit)? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    override fun start() {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()
            bindCamera()
        }, ContextCompat.getMainExecutor(context))
    }

    override fun stop() {
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
    }

    private fun bindCamera() {
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
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
        val bitmap = yuvToBitmap(this)
        val rotation = imageInfo.rotationDegrees
        return if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    /**
     * Convert a YUV_420_888 ImageProxy to an ARGB_8888 Bitmap.
     * Works on API 24+ without requiring OutputImageFormat.RGBA_8888 (API 26+).
     */
    private fun yuvToBitmap(proxy: ImageProxy): Bitmap {
        val yBuffer: ByteBuffer = proxy.planes[0].buffer
        val uBuffer: ByteBuffer = proxy.planes[1].buffer
        val vBuffer: ByteBuffer = proxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            proxy.width,
            proxy.height,
            null
        )
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, proxy.width, proxy.height),
            90,
            out
        )
        val bytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
