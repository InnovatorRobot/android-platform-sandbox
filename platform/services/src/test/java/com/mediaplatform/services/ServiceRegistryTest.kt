package com.mediaplatform.services

import org.junit.Assert.*
import org.junit.Test

class ServiceRegistryTest {

    @Test
    fun `services start in registration order`() {
        val registry = ServiceRegistry()
        val order = mutableListOf<String>()
        
        registry.register(object : PlatformService {
            override fun start() { order.add("A") }
            override fun stop() {}
        })
        
        registry.register(object : PlatformService {
            override fun start() { order.add("B") }
            override fun stop() {}
        })
        
        registry.startAll()
        
        assertEquals(listOf("A", "B"), order)
    }

    @Test
    fun `services stop in reverse order`() {
        val registry = ServiceRegistry()
        val order = mutableListOf<String>()
        
        registry.register(object : PlatformService {
            override fun start() {}
            override fun stop() { order.add("A") }
        })
        
        registry.register(object : PlatformService {
            override fun start() {}
            override fun stop() { order.add("B") }
        })
        
        registry.startAll()
        registry.stopAll()
        
        assertEquals(listOf("B", "A"), order)
    }

    @Test
    fun `getService returns registered service`() {
        val registry = ServiceRegistry()
        val service = object : PlatformService {
            override fun start() {}
            override fun stop() {}
        }
        
        registry.register(service)
        registry.startAll()
        
        val retrieved = registry.getService<PlatformService>()
        assertNotNull(retrieved)
        assertSame(service, retrieved)
    }

    @Test
    fun `getService returns null for unregistered service`() {
        val registry = ServiceRegistry()
        registry.startAll()
        
        val retrieved = registry.getService<PlatformService>()
        assertNull(retrieved)
    }

    @Test(expected = IllegalStateException::class)
    fun `register after startAll throws exception`() {
        val registry = ServiceRegistry()
        registry.startAll()
        
        registry.register(object : PlatformService {
            override fun start() {}
            override fun stop() {}
        })
    }
}

