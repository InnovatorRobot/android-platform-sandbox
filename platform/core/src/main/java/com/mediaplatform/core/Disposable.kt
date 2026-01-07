package com.mediaplatform.core

/**
 * Resource disposal interface.
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

