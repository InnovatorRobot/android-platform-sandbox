plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.spotify.platform.library"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Features can ONLY depend on platform modules, never on other features
    implementation(project(":platform:core"))
    implementation(project(":platform:state"))
    
    // NO dependencies on :features:playback or any other feature module
}

