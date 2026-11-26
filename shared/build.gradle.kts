import java.util.Properties

plugins {
    id("woorisseum.compose.library")
    alias(libs.plugins.kotlinSerialization)
}

compose.resources {
    packageOfResClass = "com.woorisseum.resources"
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
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.testRunner)
        }
    }
}

/** 공개 접속 설정만 생성한다. 인증 토큰·서버 비밀 키를 소스에 생성하지 않는다. */
abstract class GenerateSupabaseConfig : DefaultTask() {
    @get:Input abstract val projectUrl: Property<String>
    @get:Input abstract val publishableKey: Property<String>
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        fun literal(value: String) = "\"" + value.replace("\\", "\\\\")
            .replace("\"", "\\\"").replace("$", "\\$").replace("\n", "\\n").replace("\r", "\\r") + "\""
        val output = outputDirectory.file("com/woorisseum/config/GeneratedSupabaseConfig.kt").get().asFile
        output.parentFile.mkdirs()
        output.writeText("""
            package com.woorisseum.config
            internal object GeneratedSupabaseConfig {
                const val url: String = ${literal(projectUrl.get())}
                const val publishableKey: String = ${literal(publishableKey.get())}
            }
        """.trimIndent())
    }
}

val localSupabaseSettings = providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
    .asText.orElse("").map { contents ->
        Properties().apply { load(contents.reader()) }
    }
val generateSupabaseConfig = tasks.register<GenerateSupabaseConfig>("generateSupabaseConfig") {
    fun setting(name: String) = providers.environmentVariable(name).orElse(
        localSupabaseSettings.map { it.getProperty(name, "").trim().removeSurrounding("\"").removeSurrounding("'") }
    )
    projectUrl.set(setting("SUPABASE_URL"))
    publishableKey.set(setting("SUPABASE_PUBLISHABLE_KEY"))
    outputDirectory.set(layout.buildDirectory.dir("generated/supabase/commonMain"))
}
kotlin.sourceSets.commonMain { kotlin.srcDir(generateSupabaseConfig) }
