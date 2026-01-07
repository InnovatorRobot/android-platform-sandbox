package com.mediaplatform.nativebridge

import com.mediaplatform.core.Result
import com.mediaplatform.services.PlatformService

/**
 * Kotlin wrapper for the native C++ engine.
 */
class NativeEngine : PlatformService {
    private var nativePtr: Long = 0

    enum class PlaybackState(val value: Int) {
        Idle(0),
        Buffering(1),
        Playing(2),
        Paused(3),
        Error(4);

        companion object {
            fun fromInt(value: Int): PlaybackState {
                return values().find { it.value == value } ?: Error
            }
        }
    }

    init {
        System.loadLibrary("native-bridge")
    }

    override fun start() {
        if (nativePtr == 0L) {
            nativePtr = nativeCreate()
            nativeInitialize(nativePtr)
        }
    }

    override fun stop() {
        if (nativePtr != 0L) {
            nativeShutdown(nativePtr)
            nativeDestroy(nativePtr)
            nativePtr = 0
        }
    }

    fun loadTrack(trackId: String): Result<Unit> {
        return try {
            if (nativeLoadTrack(nativePtr, trackId)) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to load track"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun play(): Result<Unit> {
        return try {
            if (nativePlay(nativePtr)) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to play"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun pause(): Result<Unit> {
        return try {
            if (nativePause(nativePtr)) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to pause"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun stopPlayback(): Result<Unit> {
        return try {
            if (nativeStop(nativePtr)) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to stop"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun getState(): PlaybackState {
        return PlaybackState.fromInt(nativeGetState(nativePtr))
    }

    // Native JNI methods
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(nativePtr: Long)
    private external fun nativeInitialize(nativePtr: Long): Boolean
    private external fun nativeShutdown(nativePtr: Long)
    private external fun nativeLoadTrack(nativePtr: Long, trackId: String): Boolean
    private external fun nativePlay(nativePtr: Long): Boolean
    private external fun nativePause(nativePtr: Long): Boolean
    private external fun nativeStop(nativePtr: Long): Boolean
    private external fun nativeGetState(nativePtr: Long): Int
}

