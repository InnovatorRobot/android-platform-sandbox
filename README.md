# FilterLens

A real-time image filter camera app for Android that demonstrates a production-quality
multi-module architecture with live C++ image processing via JNI.

## What It Does

FilterLens captures live camera frames and applies image filters in real time using native
C++ convolution kernels. Tap a chip at the bottom to switch filters instantly:

| Filter | Description |
|---|---|
| Original | Raw camera feed, no processing |
| Grayscale | BT.601 luminance-weighted conversion |
| Blur | 3×3 box blur |
| Sharpen | 3×3 sharpen kernel |
| Edges | Grayscale → Laplacian edge detection |

An FPS counter in the top-right corner shows processing throughput.

---

## Why This Architecture?

### The Problem With Monolithic Android Apps

As a codebase grows, a monolith develops:

- Features accidentally depend on each other — tightly coupled code
- Any change can break unrelated modules — slow iteration
- JVM-based pixel processing at 15 fps on 640×480 frames drops frames constantly
- No enforced contract between layers — spaghetti over time

### The Solution: Strict Module Isolation + Native Processing

```
app/                  ← thin composition root; wires all modules together

platform/
  core/               ← pure Kotlin utilities, zero Android deps
  services/           ← service registry & lifecycle management
  native-bridge/      ← JNI boundary (Kotlin ↔ C++)

features/
  camera/             ← CameraX frame capture only
  filters/            ← filter state only

native/
  core-engine/        ← pure C++ convolution kernels
```

**The rule**: features never depend on other features. Features only depend on platform
modules. This is enforced at Gradle level.

### Why C++ for the Filters?

Each camera frame is 640×480 = ~307,000 pixels. A 3×3 convolution kernel applies
9 multiply-add operations per pixel. At 15 fps that is ~41M operations/sec per filter pass.
The JVM cannot sustain this without dropped frames; C++ runs each pass in microseconds.

---

## Architecture

```
┌──────────────────────────────────────────┐
│                  app/                    │
│   camera permission · chip UI · FPS      │
└──────────────┬───────────────────────────┘
               │
    ┌──────────┴──────────┐
    │                     │
features/camera     features/filters
CameraX pipeline    FilterType enum
YUV → Bitmap        current filter state
    │                     │
    └──────────┬──────────┘
               │
    ┌──────────▼───────────────────┐
    │          platform/           │
    │  core · services             │
    │  native-bridge (JNI)         │
    └──────────┬───────────────────┘
               │
    ┌──────────▼──────────┐
    │      native/        │
    │    core-engine      │
    │   (C++ kernels)     │
    └─────────────────────┘
```

---

## Module Reference

### `platform/core`

Pure Kotlin. `Result<T>` sealed class (`Success` / `Error`) with `map`, `onSuccess`,
`onError` extensions. Zero Android dependencies — usable in any JVM project.

### `platform/services`

`PlatformService` interface (`start`, `stop`, `dispose`) and `ServiceRegistry` that manages
ordered start and stop of all services with a single call each.

### `platform/native-bridge`

`ImageProcessorEngine` wraps the C++ filter engine via JNI. It allocates and frees the
native object, copies ARGB pixel arrays across the JNI boundary, and returns a filtered
`Bitmap` for each frame.

### `features/camera`

Uses CameraX `ImageAnalysis` to subscribe to YUV_420_888 frames, converts each to an
ARGB_8888 `Bitmap`, corrects rotation, and delivers frames via an `onFrameAvailable`
callback. Target resolution: 640×480.

### `features/filters`

`FilterType` enum (maps to C++ int IDs 0–4) and `FiltersFeature` holding the currently
selected filter. The app layer reads `currentFilter` before each frame is processed.

### `native/core-engine`

C++ `ImageFilter` class with five filter implementations using 3×3 convolution kernels.
Operates in-place on `int32_t` ARGB pixel arrays. No Android or JNI dependencies — pure
C++17 and STL only.

| ID | Filter | Kernel |
|---|---|---|
| 0 | None | pass-through |
| 1 | Grayscale | BT.601 weights (299/587/114) |
| 2 | Blur | 3×3 box (all 1/9) |
| 3 | Sharpen | `[0,-1,0,-1,5,-1,0,-1,0]` |
| 4 | Edge Detect | grayscale → Laplacian `[-1,-1,-1,-1,8,-1,-1,-1,-1]` |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 17) + C++17 |
| Camera | CameraX 1.3.1 (`ImageAnalysis`) |
| NDK | NDK 27.0.12077973 |
| Build system | CMake 3.22.1, Gradle 8.13 |
| UI | ViewBinding, Material Components 1.11.0 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 |

---

## Running the App

See [QUICKSTART.md](QUICKSTART.md) for setup and build instructions.

---

## Making the Case to Your Team

> *"Why not just put everything in one module and call it a day?"*

| Concern | This architecture |
|---|---|
| **Onboarding** | Each module has one job; new devs read one file to understand a feature |
| **Build time** | Only changed modules recompile; modules are small so clean builds are fast |
| **Testability** | `platform/core` and `platform/services` have zero Android deps — test on JVM instantly |
| **Parallel dev** | Two teams can own `features/camera` and `features/filters` with zero merge conflicts on shared code |
| **Performance** | C++ filter pipeline is the same approach used in production camera apps |
| **Refactoring safety** | Strict Gradle boundaries mean `features/camera` literally cannot import `features/filters` — the compiler enforces the contract |
