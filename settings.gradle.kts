pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven ( "https://jitpack.io" )
        maven ( "https://s01.oss.sonatype.org/content/groups/public" )
    }
    plugins {
        id("com.android.application") version "7.1.3"
        id("com.android.library") version "7.1.3"
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
        maven ( "https://oss.sonatype.org/content/repositories/snapshots" )
    }
}

rootProject.name = "NeteaseViewer"
include (":app")
