buildscript {
    dependencies {
        // Kotlin mas nuevo que el que trae AGP 9 (2.2.10). Requerido por Firebase Auth 25.
        // OJO: debe ser la misma version que "kotlin" en gradle/libs.versions.toml.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.services) apply false
}