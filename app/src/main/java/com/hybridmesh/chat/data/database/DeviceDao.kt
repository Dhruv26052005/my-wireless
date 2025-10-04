package com.hybridmesh.chat.data.database

import androidx.room.*
import com.hybridmesh.chat.data.model.Device
import com.hybridmesh.chat.data.model.TransportType
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    
    @Query("SELECT * FROM devices ORDER BY lastSeen DESC")
    fun getAllDevices(): Flow<List<Device>>
    
    @Query("SELECT * FROM devices WHERE isOnline = 1 ORDER BY lastSeen DESC")
    fun getOnlineDevices(): Flow<List<Device>>
    
    @Query("SELECT * FROM devices WHERE id = :deviceId")
    suspend fun getDeviceById(deviceId: String): Device?
    
    @Query("SELECT * FROM devices WHERE macAddress = :macAddress")
    suspend fun getDeviceByMacAddress(macAddress: String): Device?
    
    @Query("SELECT * FROM devices WHERE :transportType IN (SELECT value FROM json_each(transportTypes))")
    fun getDevicesByTransportType(transportType: String): Flow<List<Device>>
    
    @Query("SELECT * FROM devices WHERE lastSeen > :cutoffTime ORDER BY signalStrength DESC")
    fun getRecentDevices(cutoffTime: Long): Flow<List<Device>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<Device>)
    
    @Update
    suspend fun updateDevice(device: Device)
    
    @Query("UPDATE devices SET isOnline = :isOnline, lastSeen = :lastSeen WHERE id = :deviceId")
    suspend fun updateDeviceStatus(deviceId: String, isOnline: Boolean, lastSeen: Long)
    
    @Query("UPDATE devices SET signalStrength = :signalStrength, lastSeen = :lastSeen WHERE id = :deviceId")
    suspend fun updateDeviceSignal(deviceId: String, signalStrength: Int, lastSeen: Long)
    
    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun deleteDevice(deviceId: String)
    
    @Query("DELETE FROM devices WHERE lastSeen < :cutoffTime")
    suspend fun deleteOldDevices(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM devices WHERE isOnline = 1")
    suspend fun getOnlineDeviceCount(): Int
}
