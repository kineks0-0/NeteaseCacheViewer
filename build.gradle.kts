// Top-level build file where you can add configuration options common to all sub-projects/modules.

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

buildscript {

    val composeVersion by extra("1.3.0-alpha02")

    val accompanistVersion by extra("0.26.0-alpha")

    val pagingVersion by extra("3.1.1")

    val appCenterSdkVersion by extra("4.4.5")

}