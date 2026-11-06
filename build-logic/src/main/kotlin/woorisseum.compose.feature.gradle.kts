import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("woorisseum.compose.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:designsystem"))
            implementation(catalog.findLibrary("kotlinx-coroutines-core").get())
            implementation(catalog.findLibrary("kotlinx-serialization-json").get())
            implementation(catalog.findLibrary("androidx-lifecycle-viewmodelCompose").get())
            implementation(catalog.findLibrary("androidx-lifecycle-runtimeCompose").get())
            implementation(catalog.findLibrary("koin-compose").get())
            implementation(catalog.findLibrary("koin-compose-viewmodel").get())
        }
        commonTest.dependencies {
            implementation(catalog.findLibrary("koin-test").get())
        }
    }
}
