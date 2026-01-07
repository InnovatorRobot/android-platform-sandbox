package com.spotify.platform.core

/**
 * Simple resource disposal interface.
 * Platform-agnostic utility for managing lifecycle.
 */
interface Disposable {
    fun dispose()
}

/**
 * Helper to dispose multiple resources.
 */
fun disposeAll(vararg disposables: Disposable) {
    disposables.forEach { it.dispose() }
}

