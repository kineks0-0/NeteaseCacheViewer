

// Top-level build file where you can add configuration options common to all sub-projects/modules.

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

buildscript {
    @Suppress("LocalVariableName")
    val compose_version by extra("1.2.0-alpha07")
}
