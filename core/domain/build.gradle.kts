plugins {
    id("woorisseum.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Repository/model contracts can expose Flow and LocalDate.
            // Keep Compose, DI and data-source dependencies out of domain.
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
