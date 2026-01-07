# Android Platform Sandbox

## Project Overview

This project is **not a feature app**. It's a **miniature client platform** designed to showcase:

- **SEM (Service Extraction Model) / isolation strategies** - Strict module boundaries with enforced dependency rules
- **Kotlin/Java ↔ C++ interoperability** - JNI bridge with shared C++ core
- **Platform building blocks** - Reusable foundation modules
- **Service lifecycle management** - Explicit service registry and lifecycle
- **Developer experience** - Fast builds, clear contracts, tooling
- **Architectural leadership** - Strong documentation with rationale

## Architecture

### High-Level Structure

```
android-platform-sandbox/
│
├── app/                       # Thin composition layer
│   └── MainActivity.kt       # Wires features + services together
│
├── platform/                 # Platform building blocks
│   ├── core/                 # Pure Kotlin utilities (no Android deps)
│   ├── state/                # App state & flow coordination
│   ├── services/             # Service registry & lifecycle mgmt
│   └── native-bridge/        # JNI boundary (Kotlin ↔ C++)
│
├── features/                 # Isolated feature modules
│   ├── playback/             # Playback feature (isolated)
│   └── library/              # Library feature (isolated)
│
├── native/                   # Native C++ code
│   └── core-engine/          # Pure C++ logic (platform-agnostic)
│
├── tools/                    # Developer tooling
│   └── dependency-checker/   # Enforces isolation rules
│
└── docs/                     # Architecture documentation
```

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         app/                                 │
│  (Composition Root - wires everything together)              │
└──────────────┬──────────────────────────────────────────────┘
               │
    ┌──────────┴──────────┐
    │                     │
┌───▼────────┐    ┌───────▼────────┐
│  features/ │    │   platform/    │
│            │    │                │
│ playback   │    │ core           │
│ library    │    │ state          │
│            │    │ services       │
│            │    │ native-bridge  │
└───┬────────┘    └───────┬────────┘
    │                     │
    │                     │
    └──────────┬──────────┘
               │
        ┌──────▼──────┐
        │  native/    │
        │ core-engine │
        │  (C++)      │
        └─────────────┘
```

## Core Technical Concepts

### 1. Strict Module Isolation (SEM-style)

**Rule**: Features never depend on other features. Features only depend on platform interfaces.

**Enforcement**:
- Gradle dependency constraints
- Custom build-time checker (`tools/dependency-checker/check_dependencies.py`)

**Why This Matters**:
- Features can be **developed and tested in isolation**
- Reduces cognitive load
- Enables parallel development
- Improves build times (only changed modules rebuild)

**Example**:
```kotlin
// Allowed: Feature depends on platform
dependencies {
    implementation(project(":platform:core"))
    implementation(project(":platform:services"))
}

// Forbidden: Feature depends on another feature
dependencies {
    implementation(project(":features:playback"))  // NOT ALLOWED
}
```

### 2. Kotlin ↔ C++ Interoperability

**Architecture**:
- **C++ Core** (`native/core-engine/`): Platform-agnostic playback logic
  - Thread-safe state machine
  - No Android dependencies
  - Could be shared with iOS
  
- **JNI Bridge** (`platform/native-bridge/`): Clean boundary layer
  - Type-safe conversions
  - Lifecycle management
  - Callback mechanism

**Why C++ for Core Logic**:
- Performance-critical operations
- Platform-agnostic (could be shared with iOS)
- Thread safety at the native level
- Clear separation of concerns

**Example Flow**:
```
Kotlin (PlaybackFeature)
    ↓
JNI Bridge (NativeEngine.kt)
    ↓
JNI (jni_bridge.cpp)
    ↓
C++ Core (engine.cpp)
```

### 3. Service Lifecycle System

**Design**:
- `PlatformService` interface with explicit `start()` and `stop()` methods
- `ServiceRegistry` manages all services
- Services started in registration order, stopped in reverse

**Why This Matters**:
- Explicit lifecycle management
- Predictable initialization order
- Easy to test (can start/stop services independently)
- Maps to requirement: *"Use established service systems to manage application services and their lifecycles"*

**Example**:
```kotlin
val registry = ServiceRegistry()
registry.register(NativeEngine())
registry.register(PlaybackFeature(engine))
registry.startAll()  // All services initialized
```

### 4. Composable App Assembly

The `app` module:
- Knows **nothing** about implementation details
- Wires features + platform services together
- Acts as a **composition root**

**Why This Matters**:
- Clean architecture
- Dependency inversion
- Platform thinking over feature thinking
- Easy to swap implementations

## Testing Strategy

### Unit Tests
- **C++ Core**: GoogleTest for native logic (setup included)
- **Platform Modules**: JVM tests for Kotlin code
- **Features**: Isolated tests with fake services

### Test Utilities
- Fake implementations of `PlatformService`
- Mock `NativeEngine` for feature tests
- State verification helpers

**Example Test**:
```kotlin
@Test
fun `playTrack loads track and plays`() {
    val mockEngine = mock(NativeEngine::class.java)
    val feature = PlaybackFeature(mockEngine)
    
    feature.playTrack("track1")
    
    verify(mockEngine).loadTrack("track1")
    verify(mockEngine).play()
}
```

## Developer Experience

### Build Performance
- Module isolation enables parallel builds
- Only changed modules rebuild
- Native code isolated reduces rebuild scope

### Tooling
- **Dependency Checker**: Fails build if isolation rules violated
  ```bash
  ./gradlew checkDependencies
  ```

### Clear Contracts
- Platform interfaces define clear boundaries
- Features depend on interfaces, not implementations
- Easy to understand module responsibilities

## 📊 Module Dependency Rules

| Module | Can Depend On |
|--------|---------------|
| `app` | All platform modules, all features |
| `features:*` | Platform modules only (never other features) |
| `platform:core` | Nothing (pure utilities) |
| `platform:state` | `platform:core` |
| `platform:services` | `platform:core` |
| `platform:native-bridge` | `platform:core`, `native:core-engine` |

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 24+
- CMake 3.22.1+
- Python 3 (for dependency checker)

### Build
```bash
./gradlew build
```

### Run Dependency Checker
```bash
./gradlew checkDependencies
# or
python3 tools/dependency-checker/check_dependencies.py
```

### Run Tests
```bash
./gradlew test
```

## Documentation

- [Architecture Details](docs/architecture.md)
- [Module Rules](docs/module-rules.md)
- [Service Lifecycle](docs/lifecycle.md)

## Why This Project Demonstrates Platform Engineering

### Key Platform Engineering Principles

| Principle | How This Project Shows It |
|---------------------|--------------------------|
| Module isolation strategies | Strict module boundaries + dependency rules |
| Kotlin/Java ↔ C++ interoperability | JNI + CMake + shared C++ core |
| Platform building blocks | Reusable "foundation" modules |
| Service lifecycle management | Explicit service registry + lifecycle |
| Developer experience | Fast builds, clear contracts, tooling |
| Architectural leadership | Strong README + diagrams + rationale |

### Key Differentiators

1. **Isolation First**: Features are truly isolated, not just separated
2. **Tooling**: Build-time enforcement of architectural rules
3. **Native Integration**: Real JNI implementation, not just a stub
4. **Lifecycle Management**: Explicit service system, not ad-hoc initialization
5. **Documentation**: Thoughtful explanations of tradeoffs and decisions

## Future Extensions

- **Build Performance**: Demonstrate parallel builds and incremental compilation
- **Multiplatform**: Show how C++ core could be shared with iOS
- **Advanced Tooling**: More sophisticated dependency analysis
- **Testing Infrastructure**: Comprehensive test suite with coverage

## License

This project is a demonstration/portfolio piece.

---

**Built to demonstrate platform engineering principles at scale.**
