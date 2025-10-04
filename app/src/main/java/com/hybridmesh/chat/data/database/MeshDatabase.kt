package com.hybridmesh.chat.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.hybridmesh.chat.data.model.Device
import com.hybridmesh.chat.data.model.Message

@Database(
    entities = [Message::class, Device::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MeshDatabase : RoomDatabase() {
    
    abstract fun messageDao(): MessageDao
    abstract fun deviceDao(): DeviceDao
    
    companion object {
        @Volatile
        private var INSTANCE: MeshDatabase? = null
        
        fun getDatabase(context: Context): MeshDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeshDatabase::class.java,
                    "mesh_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
