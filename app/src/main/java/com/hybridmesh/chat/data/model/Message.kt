package com.hybridmesh.chat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val senderId: String,
    val receiverId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = true,
    val hopCount: Int = 0,
    val maxHops: Int = 5,
    val status: MessageStatus = MessageStatus.PENDING,
    val route: List<String> = emptyList(), // Path of device IDs that relayed this message
    val transportType: TransportType = TransportType.BLUETOOTH
)

enum class MessageStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    EXPIRED
}

enum class TransportType {
    BLUETOOTH,
    WIFI_DIRECT,
    HOTSPOT,
    INTERNET
}
