// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
}

allprojects {
    configurations.all {
        resolutionStrategy {
            force("org.apache.commons:commons-compress:1.26.2")
        }
    }
}