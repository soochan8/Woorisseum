plugins {
    id("woorisseum.compose.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
            binaryOption("bundleId", "com.woorisseum.shared")
        }
    }
    android {
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:data"))
            implementation(project(":core:designsystem"))
            implementation(project(":feature:auth"))
            implementation(project(":feature:household"))
            implementation(project(":feature:transaction"))
            implementation(project(":feature:budget"))
            implementation(project(":feature:shopping"))
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.compose)
        }
        commonTest.dependencies { implementation(libs.koin.test) }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.testRunner)
        }
    }
}
