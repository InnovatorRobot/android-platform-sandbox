# Quick Start Guide

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 24+
- CMake 3.22.1+ (usually bundled with Android Studio)
- Python 3 (for dependency checker)
- JDK 17+

## Setup

1. **Open in Android Studio**
   ```bash
   # Open the project
   android-studio android-platform-sandbox
   ```

2. **Sync Gradle**
   - Android Studio should automatically sync
   - Or: `File > Sync Project with Gradle Files`

3. **Verify Setup**
   ```bash
   # Run dependency checker
   ./gradlew checkDependencies
   
   # Build the project
   ./gradlew build
   ```

## Running the App

1. **Connect Device or Start Emulator**
   - Minimum API level: 24 (Android 7.0)

2. **Run**
   - Click the Run button in Android Studio
   - Or: `./gradlew installDebug`

## Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :platform:core:test
./gradlew :platform:services:test
./gradlew :features:playback:test
```

## Verifying Architecture

### Check Module Isolation

```bash
# Run dependency checker
./gradlew checkDependencies

# Or directly
python3 tools/dependency-checker/check_dependencies.py
```

### Expected Output

```
🔍 Checking module dependencies...
Project root: /path/to/android-platform-sandbox

✅ Checked 6 modules

✅ All dependencies are valid!

📋 Module isolation rules:
  • Features never depend on other features
  • Features only depend on platform modules
  • Platform modules follow strict dependency rules
```

## Project Structure Overview

```
android-platform-sandbox/
├── app/                    # Composition root
├── platform/               # Platform building blocks
│   ├── core/              # Pure Kotlin utilities
│   ├── state/             # State management
│   ├── services/          # Service lifecycle
│   └── native-bridge/     # JNI layer
├── features/              # Isolated features
│   ├── playback/          # Playback feature
│   └── library/           # Library feature
├── native/                # C++ core
│   └── core-engine/       # Platform-agnostic C++
├── tools/                 # Developer tooling
│   └── dependency-checker/
└── docs/                  # Architecture docs
```

## Key Files to Explore

### Architecture
- `README.md` - Project overview and architecture
- `docs/architecture.md` - Detailed architecture decisions
- `docs/module-rules.md` - Dependency rules
- `docs/lifecycle.md` - Service lifecycle system

### Code Examples
- `app/src/main/java/.../MainActivity.kt` - Composition root
- `platform/services/ServiceRegistry.kt` - Service lifecycle
- `platform/native-bridge/NativeEngine.kt` - JNI bridge
- `native/core-engine/engine.cpp` - C++ core logic
- `features/playback/PlaybackFeature.kt` - Isolated feature

### Tooling
- `tools/dependency-checker/check_dependencies.py` - Isolation enforcer

## Common Tasks

### Add a New Feature Module

1. Create module directory: `features/myfeature/`
2. Add to `settings.gradle.kts`:
   ```kotlin
   include(":features:myfeature")
   ```
3. Create `build.gradle.kts` with platform dependencies only
4. Update dependency checker rules
5. Register in `MainActivity.kt`

### Add a New Platform Module

1. Create module directory: `platform/mymodule/`
2. Add to `settings.gradle.kts`
3. Create `build.gradle.kts` following dependency rules
4. Update dependency checker rules

### Modify Native Code

1. Edit C++ files in `native/core-engine/src/main/cpp/`
2. Edit JNI bridge in `platform/native-bridge/src/main/cpp/`
3. Rebuild: `./gradlew :platform:native-bridge:assemble`

## Troubleshooting

### Build Fails

- **CMake not found**: Install CMake via Android Studio SDK Manager
- **Native compilation errors**: Check C++ standard (C++17 required)
- **JNI errors**: Verify method signatures match between Kotlin and C++

### Dependency Checker Fails

- Review error message for specific violation
- Check `build.gradle.kts` files for forbidden dependencies
- Update `tools/dependency-checker/check_dependencies.py` if adding new modules

### Tests Fail

- Ensure Mockito is available for feature tests
- Check that test dependencies are in `build.gradle.kts`
- Verify test classes are in `src/test/` directory

## Next Steps

1. Read `README.md` for project overview
2. Explore `docs/` for architecture details
3. Review code examples in platform and feature modules
4. Run tests to see testing patterns
5. Try adding a new feature module following isolation rules

## Resources

- [Android NDK Documentation](https://developer.android.com/ndk)
- [JNI Specification](https://docs.oracle.com/javase/8/docs/technotes/guides/jni/)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [CMake Documentation](https://cmake.org/documentation/)

