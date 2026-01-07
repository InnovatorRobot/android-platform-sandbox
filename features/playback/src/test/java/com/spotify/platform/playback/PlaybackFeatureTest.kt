package com.spotify.platform.playback

import com.spotify.platform.core.Result
import com.spotify.platform.nativebridge.NativeEngine
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.Mockito.mock

/**
 * Example test for PlaybackFeature.
 * 
 * This demonstrates:
 * - Feature testing with mocked dependencies
 * - Isolation testing (feature tested independently)
 * - Platform interface mocking
 */
class PlaybackFeatureTest {

    @Test
    fun `playTrack loads track and plays`() {
        // Create mock native engine
        val mockEngine = mock(NativeEngine::class.java)
        `when`(mockEngine.loadTrack("track1")).thenReturn(Result.Success(Unit))
        `when`(mockEngine.play()).thenReturn(Result.Success(Unit))
        
        val feature = PlaybackFeature(mockEngine)
        feature.start()
        
        val result = feature.playTrack("track1")
        
        assertTrue(result is Result.Success)
        verify(mockEngine).loadTrack("track1")
        verify(mockEngine).play()
    }

    @Test
    fun `pause calls native engine pause`() {
        val mockEngine = mock(NativeEngine::class.java)
        `when`(mockEngine.pause()).thenReturn(Result.Success(Unit))
        
        val feature = PlaybackFeature(mockEngine)
        feature.start()
        
        val result = feature.pause()
        
        assertTrue(result is Result.Success)
        verify(mockEngine).pause()
    }

    @Test
    fun `stop calls native engine stop`() {
        val mockEngine = mock(NativeEngine::class.java)
        `when`(mockEngine.stopPlayback()).thenReturn(Result.Success(Unit))
        
        val feature = PlaybackFeature(mockEngine)
        feature.start()
        
        val result = feature.stop()
        
        assertTrue(result is Result.Success)
        verify(mockEngine).stopPlayback()
    }
}

