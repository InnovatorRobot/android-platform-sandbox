# Module Dependency Rules

## Overview

This document defines the strict dependency rules that enforce module isolation in the Android Platform Sandbox.

## Core Rules

### Rule 1: Features Never Depend on Other Features

**Enforcement**: Build-time dependency checker

**Rationale**:
- Features must be developed and tested in isolation
- Prevents feature-to-feature coupling
- Enables parallel development

**Example Violations**:
```kotlin
// ❌ FORBIDDEN
dependencies {
    implementation(project(":features:playback"))
}
```

### Rule 2: Features Only Depend on Platform Modules

**Enforcement**: Build-time dependency checker

**Rationale**:
- Features depend on stable platform interfaces
- Platform provides clear contracts
- Features can evolve independently

**Allowed Dependencies**:
```kotlin
// ✅ ALLOWED
dependencies {
    implementation(project(":platform:core"))
    implementation(project(":platform:state"))
    implementation(project(":platform:services"))
}
```

### Rule 3: Platform Modules Follow Strict Hierarchy

**Enforcement**: Build-time dependency checker

**Rationale**:
- Prevents circular dependencies
- Clear module responsibilities
- Predictable build order

## Dependency Matrix

| Module | Can Depend On |
|--------|---------------|
| `app` | All platform modules, all features |
| `features:playback` | `platform:core`, `platform:state`, `platform:services`, `platform:native-bridge` |
| `features:library` | `platform:core`, `platform:state` |
| `platform:core` | None |
| `platform:state` | `platform:core` |
| `platform:services` | `platform:core` |
| `platform:native-bridge` | `platform:core`, `native:core-engine` |
| `native:core-engine` | None |

## Dependency Checker

The dependency checker (`tools/dependency-checker/check_dependencies.py`) validates these rules at build time.

### Running the Checker

```bash
./gradlew checkDependencies
```

or

```bash
python3 tools/dependency-checker/check_dependencies.py
```

### What It Checks

1. Features don't depend on other features
2. Features only depend on allowed platform modules
3. Platform modules follow dependency hierarchy
4. No circular dependencies

### Example Output

**Success**:
```
🔍 Checking module dependencies...
Project root: /path/to/project

✅ Checked 6 modules

✅ All dependencies are valid!

📋 Module isolation rules:
  • Features never depend on other features
  • Features only depend on platform modules
  • Platform modules follow strict dependency rules
```

**Failure**:
```
🔍 Checking module dependencies...
Project root: /path/to/project

✅ Checked 6 modules

❌ Dependency violations found:

  • features:playback -> features:library (not allowed: features cannot depend on other features)

💡 Fix these violations to maintain module isolation.
```

## Adding New Modules

When adding a new module, update:

1. `settings.gradle.kts` - Include the module
2. `tools/dependency-checker/check_dependencies.py` - Add dependency rules
3. This document - Update dependency matrix

## Common Patterns

### Feature Needs Another Feature's Functionality

**Problem**: Feature A needs functionality from Feature B.

**Solution**: Extract shared functionality to a platform module.

**Example**:
- If `playback` needs library functionality, create `platform:library-interface`
- Both features depend on the platform interface
- Implementation can be in either feature or platform

### Platform Module Needs Feature Functionality

**Problem**: Platform module needs something from a feature.

**Solution**: This should not happen. Platform modules should be feature-agnostic.

**If it does happen**: Reconsider the design. The functionality might belong in platform, or the dependency might be inverted.

## Enforcement

### Build-Time
- Dependency checker runs as part of `checkDependencies` task
- Can be integrated into CI/CD pipeline
- Fails build if violations found

### Code Review
- Reviewers should check dependency rules
- Dependency checker output should be reviewed
- New dependencies should be justified

## Exceptions

Exceptions to these rules should be:
1. Documented with rationale
2. Approved by architecture review
3. Added to dependency checker as allowed exceptions
4. Reviewed periodically

## Benefits

### Development Speed
- Parallel development of features
- Reduced coordination overhead
- Faster builds (only changed modules rebuild)

### Code Quality
- Clear module boundaries
- Reduced coupling
- Easier to test

### Maintainability
- Features can be removed without affecting others
- Platform changes are intentional
- Clear ownership of code

