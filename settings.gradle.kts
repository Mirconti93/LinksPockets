pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LinksPockets"
include(":composeApp", ":androidMain")
project(":androidMain").projectDir = file("composeApp/src/androidMain")
