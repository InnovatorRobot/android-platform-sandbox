package com.mediaplatform.core

import org.junit.Assert.*
import org.junit.Test

class ResultTest {

    @Test
    fun `Success maps correctly`() {
        val result: Result<Int> = Result.Success(42)
        val mapped = result.map { it * 2 }
        
        assertTrue(mapped is Result.Success)
        assertEquals(84, (mapped as Result.Success).data)
    }

    @Test
    fun `Error maps to Error`() {
        val result: Result<Int> = Result.Error(Exception("test"))
        val mapped = result.map { it * 2 }
        
        assertTrue(mapped is Result.Error)
    }

    @Test
    fun `onSuccess is called for Success`() {
        var called = false
        val result: Result<Int> = Result.Success(42)
        
        result.onSuccess { called = true }
        
        assertTrue(called)
    }

    @Test
    fun `onSuccess is not called for Error`() {
        var called = false
        val result: Result<Int> = Result.Error(Exception("test"))
        
        result.onSuccess { called = true }
        
        assertFalse(called)
    }

    @Test
    fun `onError is called for Error`() {
        var called = false
        val exception = Exception("test")
        val result: Result<Int> = Result.Error(exception)
        
        result.onError { called = true }
        
        assertTrue(called)
    }

    @Test
    fun `onError is not called for Success`() {
        var called = false
        val result: Result<Int> = Result.Success(42)
        
        result.onError { called = true }
        
        assertFalse(called)
    }
}

