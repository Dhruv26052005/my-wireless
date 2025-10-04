package com.hybridmesh.chat.utils

import com.hybridmesh.chat.data.model.Device
import com.hybridmesh.chat.data.model.DeviceCapability
import com.hybridmesh.chat.data.model.Message
import com.hybridmesh.chat.data.model.MessageStatus
import com.hybridmesh.chat.data.model.TransportType
import java.util.*

object DemoDataGenerator {
    
    fun generateDemoDevices(): List<Device> {
        return listOf(
            Device(
                id = "demo_device_1",
                name = "Samsung Galaxy (HybridMesh)",
                macAddress = "AA:BB:CC:DD:EE:01",
                bluetoothAddress = "AA:BB:CC:DD:EE:01",
                signalStrength = -45,
                transportTypes = listOf(TransportType.BLUETOOTH, TransportType.WIFI_DIRECT),
                capabilities = listOf(DeviceCapability.BLUETOOTH_LE, DeviceCapability.WIFI_DIRECT),
                lastSeen = System.currentTimeMillis(),
                isOnline = true
            ),
            Device(
                id = "demo_device_2", 
                name = "iPhone 15 (HybridMesh)",
                macAddress = "AA:BB:CC:DD:EE:02",
                bluetoothAddress = "AA:BB:CC:DD:EE:02",
                signalStrength = -55,
                transportTypes = listOf(TransportType.BLUETOOTH),
                capabilities = listOf(DeviceCapability.BLUETOOTH_LE),
                lastSeen = System.currentTimeMillis() - 30000,
                isOnline = true
            ),
            Device(
                id = "demo_device_3",
                name = "Pixel 8 (HybridMesh)", 
                macAddress = "AA:BB:CC:DD:EE:03",
                ipAddress = "192.168.1.100",
                signalStrength = -65,
                transportTypes = listOf(TransportType.WIFI_DIRECT, TransportType.HOTSPOT),
                capabilities = listOf(DeviceCapability.WIFI_DIRECT, DeviceCapability.HOTSPOT),
                lastSeen = System.currentTimeMillis() - 60000,
                isOnline = true
            )
        )
    }
    
    fun generateDemoMessages(): List<Message> {
        return listOf(
            Message(
                id = "msg_1",
                content = "Hello! This is a test message from the mesh network.",
                senderId = "demo_device_1",
                receiverId = "current_device",
                timestamp = System.currentTimeMillis() - 300000,
                status = MessageStatus.DELIVERED,
                hopCount = 1,
                transportType = TransportType.BLUETOOTH
            ),
            Message(
                id = "msg_2",
                content = "How's the mesh network working?",
                senderId = "current_device",
                receiverId = "demo_device_2", 
                timestamp = System.currentTimeMillis() - 180000,
                status = MessageStatus.SENT,
                hopCount = 0,
                transportType = TransportType.BLUETOOTH
            ),
            Message(
                id = "msg_3",
                content = "The hybrid mesh is working great! Messages are being relayed through multiple devices.",
                senderId = "demo_device_3",
                receiverId = "current_device",
                timestamp = System.currentTimeMillis() - 120000,
                status = MessageStatus.DELIVERED,
                hopCount = 2,
                transportType = TransportType.WIFI_DIRECT
            )
        )
    }
    
    fun generateRandomDeviceName(): String {
        val adjectives = listOf("Swift", "Bright", "Quick", "Smart", "Fast", "Cool", "Sharp", "Bold", "Agile", "Nimble")
        val nouns = listOf("Phoenix", "Eagle", "Tiger", "Wolf", "Hawk", "Lion", "Bear", "Fox", "Panther", "Falcon")
        val random = Random()
        val adjective = adjectives[random.nextInt(adjectives.size)]
        val noun = nouns[random.nextInt(nouns.size)]
        return "$adjective$noun (HybridMesh)"
    }
    
    fun generateRandomMacAddress(): String {
        val random = Random()
        return String.format(
            "%02X:%02X:%02X:%02X:%02X:%02X",
            random.nextInt(256),
            random.nextInt(256), 
            random.nextInt(256),
            random.nextInt(256),
            random.nextInt(256),
            random.nextInt(256)
        )
    }
}
