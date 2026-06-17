package com.mediaplatform.nativebridge

import com.mediaplatform.services.PlatformService

/**
 * JNI wrapper around the C++ [audio_proc::AudioProcessor].
 * Computes RMS dB levels, maps them to filter IDs, and produces
 * logarithmically-spaced FFT magnitude bands for visualisation.
 */
class AudioProcessorEngine : PlatformService {

    private var nativePtr: Long = 0L

    init {
        System.loadLibrary("native-bridge")
    }

    override fun start() {
        if (nativePtr == 0L) nativePtr = nativeCreate()
    }

    override fun stop() {
        if (nativePtr != 0L) {
            nativeDestroy(nativePtr)
            nativePtr = 0L
        }
    }

    override fun dispose() = stop()

    /** Compute RMS power in dBFS. Returns -100 for silence. */
    fun computeDb(samples: ShortArray): Float =
        if (nativePtr != 0L) nativeComputeDb(nativePtr, samples) else -100f

    /**
     * Map a dBFS level to an image_filter::FilterType int (0–4).
     *   < -50  → 0 None
     *  -50..-35 → 2 Blur
     *  -35..-20 → 1 Grayscale
     *  -20..-10 → 3 Sharpen
     *   > -10  → 4 EdgeDetect
     */
    fun filterForDb(db: Float): Int =
        if (nativePtr != 0L) nativeFilterForDb(nativePtr, db) else 0

    /**
     * Compute [bandCount] logarithmically-spaced FFT magnitude bands
     * from raw PCM samples, each normalised to [0, 1].
     */
    fun computeBands(samples: ShortArray, bandCount: Int): FloatArray =
        if (nativePtr != 0L) nativeComputeBands(nativePtr, samples, bandCount)
        else FloatArray(bandCount)

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(ptr: Long)
    private external fun nativeComputeDb(ptr: Long, samples: ShortArray): Float
    private external fun nativeFilterForDb(ptr: Long, db: Float): Int
    private external fun nativeComputeBands(
        ptr: Long, samples: ShortArray, bandCount: Int): FloatArray
}
