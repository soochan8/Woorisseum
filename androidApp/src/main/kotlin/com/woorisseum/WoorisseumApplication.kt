package com.woorisseum

import android.app.Application
import com.woorisseum.di.initializeAndroidApp

class WoorisseumApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeAndroidApp(this)
    }
}
