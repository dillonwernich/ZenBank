buildscript {
    repositories {
        google() // Make sure you have this
        mavenCentral() // Include if needed
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.4") // Ensure this matches your Android Gradle Plugin version
        classpath("com.google.gms:google-services:4.4.2") // Google services plugin
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.4" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
