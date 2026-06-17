# Quick Start

## Prerequisites

| Requirement | Minimum version |
|---|---|
| Android Studio | Hedgehog 2023.1.1 (Iguana or Jellyfish recommended) |
| Android SDK | API 24+ |
| NDK | 27.0.12077973 |
| CMake | 3.22.1+ |
| JDK | 17+ |

Install NDK and CMake via **Android Studio → Settings → SDK Manager → SDK Tools**.

---

## 1. Open the Project

Open the `android-platform-sandbox` folder in Android Studio.
Let the initial Gradle sync complete.

---

## 2. Connect a Device or Start an Emulator

- **Physical device**: enable USB debugging, connect via USB.
- **Emulator**: create an AVD with API 24+. For a live camera feed, configure
  the AVD with "Webcam" or "Virtual scene" camera in Extended Controls.

> Camera permission is requested at runtime on first launch.

---

## 3. Run

Click **Run ▶** in Android Studio, or from a terminal:

```bash
cd gradle
./gradlew :app:installDebug
```

> Note: the Gradle wrapper (`gradlew`) lives in the `gradle/` subdirectory,
> not the project root.

---

## 4. Using the App

1. Grant camera permission when prompted.
2. The live camera feed fills the screen.
3. Tap a chip at the bottom to switch filters:

   | Chip | Effect |
   |---|---|
   | Original | Raw camera feed |
   | Grayscale | BT.601 luminance conversion |
   | Blur | 3×3 box blur |
   | Sharpen | 3×3 sharpen kernel |
   | Edges | Laplacian edge detection |

4. The FPS counter (top-right) shows frame processing throughput.

---

## 5. Build & Test from Terminal

```bash
cd gradle

# Debug APK
./gradlew assembleDebug

# Run all unit tests
./gradlew test

# Run tests for a specific module
./gradlew :platform:core:test
./gradlew :platform:services:test

# Clean build outputs
./gradlew clean
```

---

## 6. Project Layout

```
android-platform-sandbox/
├── app/                     # Composition root (MainActivity)
├── features/
│   ├── camera/              # CameraX frame capture & delivery
│   └── filters/             # FilterType enum & active filter state
├── platform/
│   ├── core/                # Result<T>, pure Kotlin (no Android deps)
│   ├── services/            # ServiceRegistry & PlatformService interface
│   └── native-bridge/       # JNI wrapper (Kotlin + C++)
├── native/
│   └── core-engine/         # C++ convolution kernels
├── gradle/                  # Gradle wrapper (gradlew lives here)
└── docs/                    # Architecture documentation
```

---

## 7. Troubleshooting

**NDK not found / CMake error**
Install NDK 27.0.12077973 and CMake 3.22.1 via SDK Manager → SDK Tools.
Then in `gradle.properties` / `local.properties` confirm `ndk.dir` if needed.

**Camera permission denied**
Settings → Apps → FilterLens → Permissions → Camera → Allow.

**No camera preview on emulator**
In Android Studio's Device Manager, edit the AVD and set Front/Back camera
to "Webcam0" or "Virtual scene", then cold-boot the emulator.

**Build fails with `JniBridge` errors or duplicate class errors**
Build → Clean Project, then rebuild. These errors indicate stale cached
artifacts from a previous build.

**`gradlew: command not found` from project root**
The wrapper is at `gradle/gradlew`. Run `cd gradle && ./gradlew ...`.
