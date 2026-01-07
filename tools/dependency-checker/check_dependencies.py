#!/usr/bin/env python3
"""
Dependency checker tool to enforce module isolation rules.

This tool validates that:
1. Features never depend on other features
2. Features only depend on platform modules
3. Platform modules follow dependency rules

This demonstrates tooling for developer experience - a key aspect of
platform engineering at Spotify.
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Set, Tuple

# Define allowed dependencies
ALLOWED_DEPENDENCIES = {
    "app": {
        "platform": ["core", "state", "services", "native-bridge"],
        "features": ["playback", "library"]
    },
    "features:playback": {
        "platform": ["core", "state", "services", "native-bridge"],
        "features": []  # Features cannot depend on other features
    },
    "features:library": {
        "platform": ["core", "state"],
        "features": []  # Features cannot depend on other features
    },
    "platform:core": {
        "platform": [],
        "features": []
    },
    "platform:state": {
        "platform": ["core"],
        "features": []
    },
    "platform:services": {
        "platform": ["core"],
        "features": []
    },
    "platform:native-bridge": {
        "platform": ["core"],
        "features": []
    }
}


def parse_build_gradle(build_file: Path) -> List[str]:
    """Extract project dependencies from build.gradle.kts file."""
    dependencies = []
    
    if not build_file.exists():
        return dependencies
    
    with open(build_file, 'r') as f:
        content = f.read()
    
    # Match project dependencies: project(":module:name")
    pattern = r'project\(":([^"]+)"\)'
    matches = re.findall(pattern, content)
    
    return matches


def check_module_dependencies(module_path: Path) -> Tuple[bool, List[str]]:
    """Check if a module's dependencies are allowed."""
    build_file = module_path / "build.gradle.kts"
    
    if not build_file.exists():
        return True, []  # No build file, skip
    
    # Determine module identifier
    parts = module_path.parts
    if "app" in parts:
        module_id = "app"
    elif "features" in parts and "playback" in parts:
        module_id = "features:playback"
    elif "features" in parts and "library" in parts:
        module_id = "features:library"
    elif "platform" in parts:
        platform_name = parts[parts.index("platform") + 1]
        module_id = f"platform:{platform_name}"
    else:
        return True, []  # Unknown module, skip
    
    if module_id not in ALLOWED_DEPENDENCIES:
        return True, []  # No rules for this module
    
    dependencies = parse_build_gradle(build_file)
    violations = []
    
    allowed = ALLOWED_DEPENDENCIES[module_id]
    
    for dep in dependencies:
        # Determine dependency type
        if dep.startswith("platform:"):
            dep_name = dep.split(":")[1]
            if dep_name not in allowed["platform"]:
                violations.append(
                    f"{module_id} -> {dep} (not allowed: platform dependencies must be in {allowed['platform']})"
                )
        elif dep.startswith("features:"):
            dep_name = dep.split(":")[1]
            if dep_name not in allowed["features"]:
                violations.append(
                    f"{module_id} -> {dep} (not allowed: features cannot depend on other features)"
                )
        elif dep.startswith("native:"):
            # Native dependencies are allowed
            pass
        else:
            violations.append(f"{module_id} -> {dep} (unknown dependency type)")
    
    return len(violations) == 0, violations


def main():
    """Main entry point for dependency checker."""
    project_root = Path(__file__).parent.parent.parent
    
    print("🔍 Checking module dependencies...")
    print(f"Project root: {project_root}\n")
    
    all_violations = []
    modules_checked = 0
    
    # Check app module
    app_path = project_root / "app"
    if app_path.exists():
        is_valid, violations = check_module_dependencies(app_path)
        if violations:
            all_violations.extend(violations)
        modules_checked += 1
    
    # Check platform modules
    platform_path = project_root / "platform"
    if platform_path.exists():
        for module_dir in platform_path.iterdir():
            if module_dir.is_dir():
                is_valid, violations = check_module_dependencies(module_dir)
                if violations:
                    all_violations.extend(violations)
                modules_checked += 1
    
    # Check feature modules
    features_path = project_root / "features"
    if features_path.exists():
        for module_dir in features_path.iterdir():
            if module_dir.is_dir():
                is_valid, violations = check_module_dependencies(module_dir)
                if violations:
                    all_violations.extend(violations)
                modules_checked += 1
    
    # Report results
    print(f"✅ Checked {modules_checked} modules\n")
    
    if all_violations:
        print("❌ Dependency violations found:\n")
        for violation in all_violations:
            print(f"  • {violation}")
        print("\n💡 Fix these violations to maintain module isolation.")
        sys.exit(1)
    else:
        print("✅ All dependencies are valid!")
        print("\n📋 Module isolation rules:")
        print("  • Features never depend on other features")
        print("  • Features only depend on platform modules")
        print("  • Platform modules follow strict dependency rules")
        sys.exit(0)


if __name__ == "__main__":
    main()

