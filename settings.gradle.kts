pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
    plugins {
        id("com.android.application") version "7.3.0-beta04"
        id("com.android.library") version "7.3.0-beta04"
        id("org.jetbrains.kotlin.android") version "1.6.21"
        id("org.jetbrains.kotlin.kapt") version "1.6.21"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.google.com")
        maven("https://dl.google.com/dl/android/maven2/")
        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

rootProject.name = "NeteaseViewer"
include(":app")
