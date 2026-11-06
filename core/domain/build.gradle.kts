plugins { id("woorisseum.kmp.library") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Flow and LocalDate may appear in repository/model contracts.
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
        }
    }
}
