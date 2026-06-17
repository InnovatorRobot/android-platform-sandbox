package com.mediaplatform.nativebridge

import android.graphics.Bitmap
import com.mediaplatform.core.Result
import com.mediaplatform.services.PlatformService

/**
 * Kotlin wrapper around the native C++ ImageFilter engine.
 *
 * Passes ARGB_8888 pixel arrays through JNI to the C++ filter engine and
 * returns a new Bitmap with the processed pixels.
 */
class ImageProcessorEngine : PlatformService {

    private var nativePtr: Long = 0L

    init {
        System.loadLibrary("native-bridge")
    }

    override fun start() {
        if (nativePtr == 0L) {
            nativePtr = nativeCreate()
        }
    }

    override fun stop() {
        if (nativePtr != 0L) {
            nativeDestroy(nativePtr)
            nativePtr = 0L
        }
    }

    /**
     * Apply a filter to the given Bitmap.
     * @param bitmap     Source frame (any config; internally read as ARGB_8888).
     * @param filterTypeId  Integer id matching the C++ FilterType enum (0–4).
     * @return Result containing a new filtered Bitmap, or an error.
     */
    fun applyFilter(bitmap: Bitmap, filterTypeId: Int): Result<Bitmap> {
        if (nativePtr == 0L) return Result.Error(IllegalStateException("Engine not started"))
        return try {
            val width  = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val filtered = nativeApplyFilter(nativePtr, pixels, width, height, filterTypeId)

            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            output.setPixels(filtered, 0, width, 0, 0, width, height)
            Result.Success(output)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ── Native declarations ───────────────────────────────────────────────────

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(ptr: Long)
    private external fun nativeApplyFilter(
        ptr: Long,
        pixels: IntArray,
        width: Int,
        height: Int,
        filterType: Int
    ): IntArray
}
