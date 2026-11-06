plugins { `kotlin-dsl` }

kotlin { jvmToolchain(21) }

dependencies {
    implementation(libs.gradle.kotlin)
    implementation(libs.gradle.android)
    implementation(libs.gradle.compose)
    implementation(libs.gradle.composeCompiler)
    implementation(libs.gradle.serialization)
}
