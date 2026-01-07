# Service Lifecycle Management

## Overview

This document describes the service lifecycle system used in the Android Platform Sandbox.

## Design

### PlatformService Interface

All services implement the `PlatformService` interface:

```kotlin
interface PlatformService : Disposable {
    fun start()
    fun stop()
    override fun dispose() {
        stop()
    }
}
```

### ServiceRegistry

The `ServiceRegistry` manages all services:

```kotlin
class ServiceRegistry {
    fun register(service: PlatformService)
    fun startAll()
    fun stopAll()
    fun <T : PlatformService> getService(): T?
}
```

## Lifecycle Phases

### 1. Registration

Services are registered with the registry:

```kotlin
val registry = ServiceRegistry()
registry.register(NativeEngine())
registry.register(PlaybackFeature(engine))
registry.register(LibraryFeature())
```

**Rules**:
- Services must be registered before `startAll()` is called
- Registration order matters (started in registration order)
- Services can be registered multiple times (idempotent)

### 2. Initialization

All services are started:

```kotlin
registry.startAll()
```

**Behavior**:
- Services are started in registration order
- Each service's `start()` method is called
- If a service fails to start, subsequent services still start
- `start()` should be idempotent

### 3. Runtime

Services are active and can be used:

```kotlin
val engine = registry.getService<NativeEngine>()
engine?.play()
```

**Rules**:
- Services can be accessed via `getService<T>()`
- Services should handle being called before `start()` gracefully
- Services should handle being called after `stop()` gracefully

### 4. Shutdown

All services are stopped:

```kotlin
registry.stopAll()
```

**Behavior**:
- Services are stopped in reverse registration order
- Each service's `stop()` method is called
- Registry is cleared after shutdown
- `stop()` should be idempotent

## Service Implementation Pattern

### Basic Service

```kotlin
class MyService : PlatformService {
    private var started = false

    override fun start() {
        if (started) return
        // Initialize resources
        started = true
    }

    override fun stop() {
        if (!started) return
        // Clean up resources
        started = false
    }
}
```

### Service with Dependencies

```kotlin
class PlaybackFeature(
    private val engine: NativeEngine
) : PlatformService {
    override fun start() {
        // Engine should already be started by registry
        // But check anyway for safety
        if (engine is PlatformService) {
            (engine as PlatformService).start()
        }
        // Initialize feature
    }

    override fun stop() {
        // Clean up feature
    }
}
```

## Best Practices

### 1. Idempotent Operations

Both `start()` and `stop()` should be idempotent:

```kotlin
override fun start() {
    if (initialized) return
    // Initialize
    initialized = true
}
```

### 2. Resource Management

Services should clean up all resources in `stop()`:

```kotlin
override fun stop() {
    observer?.dispose()
    connection?.close()
    resources.clear()
}
```

### 3. Error Handling

Services should handle errors gracefully:

```kotlin
override fun start() {
    try {
        // Initialize
    } catch (e: Exception) {
        // Log error, but don't throw
        // Service can still be used in degraded mode
    }
}
```

### 4. Dependency Ordering

Register services in dependency order:

```kotlin
// Core services first
registry.register(NativeEngine())

// Services that depend on core
registry.register(PlaybackFeature(engine))

// Features last
registry.register(LibraryFeature())
```

## Testing

### Unit Tests

Test services in isolation:

```kotlin
@Test
fun `service starts and stops correctly`() {
    val service = MyService()
    service.start()
    assertTrue(service.isActive)
    service.stop()
    assertFalse(service.isActive)
}
```

### Integration Tests

Test service registry:

```kotlin
@Test
fun `services start in registration order`() {
    val registry = ServiceRegistry()
    val order = mutableListOf<String>()
    
    registry.register(Service("A") { order.add("A") })
    registry.register(Service("B") { order.add("B") })
    registry.startAll()
    
    assertEquals(listOf("A", "B"), order)
}
```

## Common Patterns

### Lazy Initialization

Services can defer expensive initialization:

```kotlin
class ExpensiveService : PlatformService {
    private var expensiveResource: Resource? = null

    override fun start() {
        // Mark as started, but don't initialize yet
        started = true
    }

    fun getResource(): Resource {
        if (expensiveResource == null) {
            expensiveResource = createResource()
        }
        return expensiveResource!!
    }
}
```

### Service Discovery

Services can discover other services:

```kotlin
class MyService(private val registry: ServiceRegistry) : PlatformService {
    private lateinit var engine: NativeEngine

    override fun start() {
        engine = registry.getService<NativeEngine>()
            ?: throw IllegalStateException("NativeEngine not registered")
        // Use engine
    }
}
```

## Benefits

### Explicit Lifecycle

- Clear start/stop semantics
- Predictable initialization order
- Easy to reason about

### Testability

- Services can be tested independently
- Easy to mock services
- Can start/stop services in tests

### Resource Management

- Explicit cleanup
- Prevents resource leaks
- Clear ownership

## Benefits of This Approach

This lifecycle system provides:

- Explicit lifecycle management
- Service registry pattern
- Clear initialization and shutdown
- Testable service architecture
- Established pattern for managing application services and their lifecycles

