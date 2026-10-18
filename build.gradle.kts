// Top-level build file for GymTrack.
// Versions are declared here in the plugins blocks of the modules
// (Kotlin 1.9.22, including the kapt plugin with its version stated
// explicitly — required for the GitHub Actions APK build to succeed).

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.22" apply false
}
