package com.hybridmesh.chat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val macAddress: String,
    val ipAddress: String? = null,
    val bluetoothAddress: String? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    val signalStrength: Int = 0, // RSSI for Bluetooth, signal level for Wi-Fi
    val transportTypes: List<TransportType> = emptyList(),
    val isOnline: Boolean = true,
    val publicKey: String? = null, // For encryption
    val capabilities: List<DeviceCapability> = emptyList()
)

enum class DeviceCapability {
    BLUETOOTH_LE,
    WIFI_DIRECT,
    HOTSPOT,
    INTERNET_RELAY,
    MESSAGE_STORE
}
