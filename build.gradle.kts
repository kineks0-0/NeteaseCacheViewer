// Top-level build file where you can add configuration options common to all sub-projects/modules.

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

buildscript {

    val composeVersion by extra("1.3.3")

    val accompanistVersion by extra("0.29.1-alpha")

    val appCenterSdkVersion by extra("5.0.0")

}