plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "LinksPockets"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":composeApp"))
            implementation(libs.koin.core)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}
