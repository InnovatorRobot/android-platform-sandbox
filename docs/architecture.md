# Architecture Documentation

## Overview

This document provides detailed architectural decisions and rationale for the Android Platform Sandbox project.

## Design Principles

### 1. Isolation Over Integration

**Decision**: Features are completely isolated from each other.

**Rationale**:
- Enables parallel development
- Reduces cognitive load
- Improves testability
- Allows features to be developed and tested independently

**Tradeoffs**:
- Some code duplication may occur
- Communication between features requires platform-level coordination
- Slightly more complex initial setup

**Why This Is Worth It**:
- At scale, isolation prevents cascading failures
- Teams can work independently without coordination overhead
- Features can be removed or replaced without affecting others

### 2. Platform Over Features

**Decision**: Platform modules provide stable interfaces; features depend on platform, not each other.

**Rationale**:
- Platform provides stable contracts
- Features can evolve independently
- Platform changes are intentional and well-considered
- Clear separation of concerns

**Implementation**:
- Platform interfaces are versioned and stable
- Features depend on interfaces, not implementations
- Platform modules are tested independently

### 3. Native Core for Performance-Critical Logic

**Decision**: Playback state machine and core logic in C++.

**Rationale**:
- Performance-critical operations benefit from native code
- Platform-agnostic core can be shared (e.g., with iOS)
- Thread safety at the native level
- Clear boundary between platform-specific and platform-agnostic code

**Tradeoffs**:
- More complex build setup
- JNI overhead for cross-language calls
- Requires C++ expertise

**Why This Is Worth It**:
- Demonstrates real-world interoperability
- Shows understanding of performance tradeoffs
- Common pattern for performance-critical applications

## Module Responsibilities

### `app/`
**Purpose**: Composition root

**Responsibilities**:
- Wire features and services together
- Application-level configuration
- No business logic

**Dependencies**: All platform and feature modules

### `platform/core/`
**Purpose**: Pure Kotlin utilities

**Responsibilities**:
- Result types
- Disposable pattern
- Platform-agnostic utilities

**Dependencies**: None (pure Kotlin)

### `platform/state/`
**Purpose**: State management

**Responsibilities**:
- State observation
- State holders
- State coordination

**Dependencies**: `platform:core`

### `platform/services/`
**Purpose**: Service lifecycle management

**Responsibilities**:
- Service registry
- Lifecycle coordination
- Service discovery

**Dependencies**: `platform:core`

### `platform/native-bridge/`
**Purpose**: JNI boundary layer

**Responsibilities**:
- Kotlin ↔ C++ conversions
- Lifecycle management
- Type safety

**Dependencies**: `platform:core`, `native:core-engine`

### `features/playback/`
**Purpose**: Playback feature

**Responsibilities**:
- Playback control
- Playback state management
- User-facing playback logic

**Dependencies**: Platform modules only

### `features/library/`
**Purpose**: Library feature

**Responsibilities**:
- Track library management
- Track selection
- Library state

**Dependencies**: Platform modules only

### `native/core-engine/`
**Purpose**: Platform-agnostic C++ core

**Responsibilities**:
- Playback state machine
- Thread-safe operations
- Core playback logic

**Dependencies**: None (pure C++)

## Data Flow

### Playback Flow

```
User Action (UI)
    ↓
PlaybackFeature.playTrack()
    ↓
NativeEngine.loadTrack() (JNI)
    ↓
JniBridge.loadTrack() (C++)
    ↓
Engine.loadTrack() (C++)
    ↓
PlaybackStateMachine.transitionTo() (C++)
    ↓
State Change Callback
    ↓
Kotlin Observer
    ↓
UI Update
```

### Service Initialization Flow

```
App.onCreate()
    ↓
Create ServiceRegistry
    ↓
Register Services (NativeEngine, PlaybackFeature, LibraryFeature)
    ↓
registry.startAll()
    ↓
Each service.start() called in order
    ↓
Services ready
```

## Threading Model

### C++ Core
- Thread-safe operations using `std::mutex`
- State machine protected by mutex
- Callbacks executed on calling thread

### Kotlin/JNI
- JNI calls from any thread
- State callbacks on JNI thread
- UI updates must be posted to main thread

## Error Handling

### C++ Layer
- Return boolean for success/failure
- Exceptions caught and converted to error states

### Kotlin Layer
- `Result<T>` type for operations
- `onSuccess` / `onError` callbacks
- Errors propagated through platform interfaces

## Testing Strategy

### Unit Tests
- **C++**: GoogleTest for state machine and engine logic
- **Kotlin**: JUnit for platform modules
- **Features**: Isolated tests with fake services

### Integration Tests
- Service registry lifecycle
- JNI bridge functionality
- Feature-to-platform integration

### Test Doubles
- `FakeNativeEngine`: Mock implementation for feature tests
- `FakeServiceRegistry`: Test service lifecycle
- `FakeStateHolder`: Test state observation

## Build Configuration

### Gradle
- Multi-module project structure
- Dependency constraints enforce isolation
- Native code compiled with CMake

### CMake
- C++17 standard
- Shared STL for Android compatibility
- Platform-agnostic core engine

## Performance Considerations

### Build Performance
- Module isolation enables parallel builds
- Only changed modules rebuild
- Native code changes isolated

### Runtime Performance
- Native code for performance-critical paths
- Minimal JNI overhead
- Efficient state machine transitions

## Security Considerations

### JNI Safety
- Proper reference management
- Exception handling
- Thread safety

### Module Isolation
- Prevents unauthorized dependencies
- Clear boundaries reduce attack surface
- Service lifecycle prevents resource leaks

## Future Enhancements

### Potential Improvements
1. **Dependency Injection**: Use Dagger/Hilt for service wiring
2. **Coroutines**: Use Kotlin coroutines for async operations
3. **Flow**: Use Kotlin Flow for state observation
4. **Multiplatform**: Share C++ core with iOS
5. **Advanced Tooling**: More sophisticated dependency analysis

### Scalability Considerations
- Service registry could support lazy initialization
- State management could use reactive streams
- Native bridge could support multiple engines

