# FilterLens

A real-time audio-reactive camera filter app for Android.
The microphone drives which image filter is applied to the live camera feed —
quiet is soft (blur), loud is intense (edge detect) — while an animated
16-band equaliser overlay shows the frequency spectrum in real time.
Both the audio analysis and image processing run in C++ via JNI.

---

## What It Does

### Audio-reactive filtering (automatic mode)
The microphone is analysed every ~93 ms. The C++ engine computes an RMS dB
level and maps it to a filter:

| dBFS level | Filter applied |
|---|---|
| < −50 (silence) | Original — no processing |
| −50 to −35 (quiet room) | Blur |
| −35 to −20 (conversation) | Grayscale |
| −20 to −10 (loud music) | Sharpen |
| > −10 (very loud) | Edge Detect |

### Manual override
Tap any chip at the bottom to lock a specific filter regardless of volume.

### Visualiser overlay
A semi-transparent 16-band equaliser strip sits just above the chip row,
drawn directly on a custom `AudioVisualizerView` Canvas from FFT data
computed in C++.

### FPS & dB readout
Top bar shows live frames-per-second and the current microphone dBFS level.

---

## Architecture

Two independent C++ pipelines run on separate background threads and feed
a single activity.

```
┌─────────────────────────────────────────────────────┐
│                      app/                           │
│  permissions · chip UI · FPS · dB label             │
└───────────────────┬─────────────────────────────────┘
                    │
     ┌──────────────┼──────────────┐
     │              │              │
features/camera  features/audio  features/filters
CameraX frames   mic PCM capture  FilterType + state
YUV → Bitmap     ShortArray cb    (auto or manual)
     │              │              │
     └──────────────┼──────────────┘
                    │
     ┌──────────────▼────────────────────┐
     │            platform/              │
     │  core · services · native-bridge  │
     │  ImageProcessorEngine (JNI)       │
     │  AudioProcessorEngine  (JNI)      │
     └──────────────┬────────────────────┘
                    │
     ┌──────────────▼──────────────┐
     │         native/             │
     │       core-engine           │
     │  image_filter.cpp (C++)     │
     │  audio_processor.cpp (C++)  │
     └─────────────────────────────┘
```

### Module isolation rule
Features never depend on other features. Features only depend on platform
modules. Enforced at Gradle level — the compiler rejects illegal imports.

---

## Module Reference

### `platform/core`
Pure Kotlin. `Result<T>` sealed class with `map`, `onSuccess`, `onError`.
Zero Android dependencies.

### `platform/services`
`PlatformService` interface and `ServiceRegistry` — ordered start/stop of
all services with a single call.

### `platform/native-bridge`

**`ImageProcessorEngine`** — Kotlin JNI wrapper for the C++ image filter.
Allocates/frees the native object, copies ARGB pixel arrays across the JNI
boundary per frame.

**`AudioProcessorEngine`** — Kotlin JNI wrapper for the C++ audio analyser.
Exposes `computeDb()`, `filterForDb()`, and `computeBands()`.

### `features/camera`
CameraX `ImageAnalysis` subscriber. Converts YUV_420_888 → ARGB_8888 Bitmap,
corrects rotation, delivers frames via `onFrameAvailable` callback.
Target resolution: 640×480.

### `features/audio`
Starts an `AudioRecord` (44.1 kHz mono 16-bit PCM) on a daemon thread.
Delivers 4096-sample `ShortArray` buffers via `onAudioFrame` callback.
Stops and releases the recorder cleanly on `stop()`.

### `features/filters`
`FilterType` enum (int IDs 0–4 matching C++ enum) and `FiltersFeature`
holding the currently selected filter.

### `native/core-engine`

**`image_filter.h/.cpp`** — `ImageFilter` class with five 3×3 convolution
kernels operating in-place on `int32_t` ARGB pixel arrays.

**`audio_processor.h/.cpp`** — `AudioProcessor` class:
- `computeDb()` — RMS power → dBFS (returns −100 for silence)
- `filterForDb()` — maps dB level to filter int ID
- `computeBands()` — Hann-windowed radix-2 FFT → log-spaced magnitude bands,
  normalised to [0, 1]

---

## C++ Filter Kernels

| ID | Filter | Kernel / Algorithm |
|---|---|---|
| 0 | None | pass-through |
| 1 | Grayscale | BT.601 weights (299/587/114) |
| 2 | Blur | 3×3 box (all 1/9) |
| 3 | Sharpen | `[0,−1,0,−1,5,−1,0,−1,0]` |
| 4 | Edge Detect | grayscale → Laplacian `[−1,−1,−1,−1,8,−1,−1,−1,−1]` |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 17) + C++17 |
| Camera | CameraX 1.3.1 (`ImageAnalysis`) |
| Audio | `AudioRecord` (44.1 kHz, mono, 16-bit PCM) |
| FFT | Iterative Cooley-Tukey radix-2 (STL only, no FFTW) |
| NDK | 27.0.12077973 |
| Build | CMake 3.22.1, Gradle 8.13 |
| UI | ViewBinding, Material Components 1.11.0, custom Canvas view |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 |

---

## Permissions

| Permission | Required for |
|---|---|
| `CAMERA` | Live camera feed |
| `RECORD_AUDIO` | Audio-reactive mode (optional — app works without it) |

Both are requested at runtime on first launch. If microphone is denied,
the app runs in manual filter mode with the visualiser hidden.

---

## Running the App

See [QUICKSTART.md](QUICKSTART.md) for setup and build instructions.

---

## Why Two C++ Pipelines Are Justified

| Concern | This app |
|---|---|
| **Image processing** | 640×480 × 9 multiply-adds per pixel × 15 fps ≈ 41M ops/sec — JVM drops frames |
| **Audio FFT** | 4096-sample Hann-windowed FFT at 44.1 kHz callback rate — latency-critical |
| **Independence** | Audio and camera run on separate threads; C++ ensures neither blocks the other |
| **Portability** | `core-engine` has zero Android/JNI deps — can be unit-tested on any machine |
