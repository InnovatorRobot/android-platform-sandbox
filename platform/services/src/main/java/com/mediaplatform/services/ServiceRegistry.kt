package com.mediaplatform.services

import com.mediaplatform.core.disposeAll

/**
 * Central registry for managing platform services.
 * 
 * Services are started in registration order and stopped in reverse order.
 */
class ServiceRegistry {
    private val services = mutableListOf<PlatformService>()
    private var started = false

    /**
     * Register a service. Services should be registered before calling startAll().
     */
    fun register(service: PlatformService) {
        require(!started) { "Cannot register services after startAll() has been called" }
        services.add(service)
    }

    /**
     * Start all registered services in order.
     */
    fun startAll() {
        require(!started) { "Services already started" }
        services.forEach { it.start() }
        started = true
    }

    /**
     * Stop all services in reverse order and clear the registry.
     */
    fun stopAll() {
        if (started) {
            services.asReversed().forEach { it.stop() }
            started = false
        }
        services.clear()
    }

    /**
     * Get a service by type. Useful for features to access platform services.
     */
    inline fun <reified T : PlatformService> getService(): T? {
        return services.find { it is T } as? T
    }
}

