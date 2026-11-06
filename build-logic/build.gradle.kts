plugins { `kotlin-dsl` }

kotlin { jvmToolchain(21) }

gradlePlugin {
    plugins {
        register("moduleRules") {
            id = "woorisseum.module.rules"
            implementationClass = "com.woorisseum.buildlogic.ModuleRulesPlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.kotlin)
    implementation(libs.gradle.android)
    implementation(libs.gradle.compose)
    implementation(libs.gradle.composeCompiler)
    implementation(libs.gradle.serialization)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(gradleTestKit())
}
