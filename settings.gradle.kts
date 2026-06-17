pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "android-platform-sandbox"

include(":app")

// Platform modules
include(":platform:core")
include(":platform:state")
include(":platform:services")
include(":platform:native-bridge")

// Feature modules
include(":features:camera")
include(":features:filters")

// Native module
include(":native:core-engine")

