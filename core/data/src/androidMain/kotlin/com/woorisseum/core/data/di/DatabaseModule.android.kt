package com.woorisseum.core.data.di

import android.content.Context
import androidx.room3.Room
import com.woorisseum.core.data.local.DATABASE_NAME
import com.woorisseum.core.data.local.WoorisseumDatabase
import com.woorisseum.core.data.local.buildDatabase
import org.koin.dsl.module

/** Activity 대신 Application Context를 보관한다. DB는 앱 DI 안에서 한 번 생성된다. */
fun databaseModule(context: Context) = module {
    val appContext = context.applicationContext
    single {
        buildDatabase(Room.databaseBuilder<WoorisseumDatabase>(
            context = appContext,
            name = appContext.getDatabasePath(DATABASE_NAME).absolutePath,
        ))
    }
}
