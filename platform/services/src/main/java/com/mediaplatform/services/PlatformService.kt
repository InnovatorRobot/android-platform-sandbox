package com.mediaplatform.services

import com.mediaplatform.core.Disposable

/**
 * Base interface for all platform services.
 * 
 * Services are managed by the ServiceRegistry and follow explicit lifecycle:
 * - start(): Initialize and begin service operations
 * - stop(): Clean up resources and stop operations
 */
interface PlatformService : Disposable {
    /**
     * Start the service. Should be idempotent.
     */
    fun start()

    /**
     * Stop the service and release resources. Should be idempotent.
     */
    fun stop()

    /**
     * Alias for stop() to implement Disposable interface.
     */
    override fun dispose() {
        stop()
    }
}

