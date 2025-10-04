package com.hybridmesh.chat.di

import android.content.Context
import com.hybridmesh.chat.data.database.MeshDatabase
import com.hybridmesh.chat.network.messaging.MeshMessagingService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    
    // Database
    single { MeshDatabase.getDatabase(androidContext()) }
    single { get<MeshDatabase>().messageDao() }
    single { get<MeshDatabase>().deviceDao() }
    
    // Services
    single { MeshMessagingService(androidContext()) }
    
    // ViewModels
    single { com.hybridmesh.chat.ui.viewmodel.ChatViewModel(get(), androidContext()) }
}
