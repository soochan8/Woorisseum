package com.woorisseum

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform