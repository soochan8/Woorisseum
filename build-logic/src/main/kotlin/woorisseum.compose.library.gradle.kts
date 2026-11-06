plugins {
    id("woorisseum.kmp.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    android { androidResources { enable = true } }
}
