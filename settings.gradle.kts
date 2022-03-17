pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven ( "https://jitpack.io" )
        maven ( "https://s01.oss.sonatype.org/content/groups/public" )
    }
    plugins {
        id("com.android.application") version "7.1.2"
        id("com.android.library") version "7.1.2"
        id("org.jetbrains.kotlin.android") version "1.6.10"
        id("org.jetbrains.kotlin.kapt") version "1.6.10"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven ( "https://jitpack.io" )
        maven ( "https://s01.oss.sonatype.org/content/groups/public" )
    }
}

rootProject.name = "NeteaseViewer"
include (":app")
