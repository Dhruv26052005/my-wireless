package com.hybridmesh.chat

import android.app.Application
import com.hybridmesh.chat.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class HybridMeshApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@HybridMeshApplication)
            modules(appModule)
        }
    }
}
