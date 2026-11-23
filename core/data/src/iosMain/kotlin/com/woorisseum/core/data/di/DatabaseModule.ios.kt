package com.woorisseum.core.data.di

import androidx.room3.Room
import com.woorisseum.core.data.local.DATABASE_NAME
import com.woorisseum.core.data.local.WoorisseumDatabase
import com.woorisseum.core.data.local.buildDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** 지워질 수 있는 캐시가 아닌 Application Support에 저장한다. */
@OptIn(ExperimentalForeignApi::class)
fun databaseModule() = module {
    single {
        val directory = requireNotNull(NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )?.path) { "DB storage directory unavailable" }
        buildDatabase(Room.databaseBuilder<WoorisseumDatabase>(name = "$directory/$DATABASE_NAME"))
    }
}
