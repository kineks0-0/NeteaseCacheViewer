pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.google.com")
        maven("https://dl.google.com/dl/android/maven2/")
        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
    plugins {
        id("com.android.application") version "7.2.2"
        id("com.android.library") version "7.2.2"
        id("org.jetbrains.kotlin.android") version "1.7.10"
        id("org.jetbrains.kotlin.kapt") version "1.7.10"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.google.com")
        maven("https://dl.google.com/dl/android/maven2/")
        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

rootProject.name = "NeteaseViewer"
include(":app")
