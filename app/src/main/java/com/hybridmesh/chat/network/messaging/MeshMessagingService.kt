package com.hybridmesh.chat.network.messaging

import android.content.Context
import com.hybridmesh.chat.data.database.MeshDatabase
import com.hybridmesh.chat.data.model.Message
import com.hybridmesh.chat.data.model.MessageStatus
import com.hybridmesh.chat.data.model.TransportType
import com.hybridmesh.chat.network.DiscoveryManager
import com.hybridmesh.chat.network.encryption.EncryptionManager
import com.hybridmesh.chat.network.routing.RoutingEngine
import com.hybridmesh.chat.service.StoreAndForwardManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MeshMessagingService(private val context: Context) {
    
    private val database = MeshDatabase.getDatabase(context)
    private val messageDao = database.messageDao()
    private val deviceDao = database.deviceDao()
    
    private val discoveryManager = DiscoveryManager(context)
    private val routingEngine = RoutingEngine()
    private val encryptionManager = EncryptionManager(context)
    private val storeAndForwardManager = StoreAndForwardManager(messageDao, deviceDao, routingEngine)
    private val cloudSyncManager = CloudSyncManager(context, messageDao, deviceDao)
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _serviceStatus = MutableStateFlow(ServiceStatus())
    val serviceStatus: StateFlow<ServiceStatus> = _serviceStatus.asStateFlow()
    
    data class ServiceStatus(
        val isRunning: Boolean = false,
        val discoveredDevices: Int = 0,
        val pendingMessages: Int = 0,
        val isOnline: Boolean = false,
        val lastMessageTime: Long = 0
    )
    
    init {
        startService()
    }
    
    private fun startService() {
        serviceScope.launch {
            // Start discovery
            discoveryManager.startDiscovery()
            
            // Monitor discovery updates
            discoveryManager.allDiscoveredDevices.collect { devices ->
                updateServiceStatus()
                routingEngine.updateTopology(devices)
                
                // Store devices in database
                deviceDao.insertDevices(devices.values.toList())
            }
            
            // Monitor message status updates
            messageDao.getMessagesByStatuses(listOf(MessageStatus.SENT, MessageStatus.DELIVERED))
                .collect { messages ->
                    cloudSyncManager.queueMessagesForSync(messages)
                }
        }
    }
    
    suspend fun sendMessage(content: String, receiverId: String): Boolean {
        return try {
            // Get receiver's public key
            val receiverPublicKey = encryptionManager.getStoredPublicKey(receiverId)
            if (receiverPublicKey == null) {
                // Try to get public key from device info
                val device = deviceDao.getDeviceById(receiverId)
                if (device?.publicKey != null) {
                    encryptionManager.storePublicKey(receiverId, device.publicKey.toByteArray())
                } else {
                    return false // No public key available
                }
            }
            
            // Create message
            val message = Message(
                content = content,
                senderId = getCurrentDeviceId(),
                receiverId = receiverId,
                transportType = discoveryManager.getBestTransportForDevice(receiverId) ?: TransportType.BLUETOOTH
            )
            
            // Encrypt message
            val encryptedMessage = encryptionManager.encryptMessage(content, receiverPublicKey)
            if (encryptedMessage == null) {
                return false
            }
            
            // Store encrypted message
            val encryptedContent = com.google.gson.Gson().toJson(encryptedMessage)
            val finalMessage = message.copy(content = encryptedContent, isEncrypted = true)
            
            // Queue for delivery
            storeAndForwardManager.queueMessage(finalMessage)
            
            updateServiceStatus()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun receiveMessage(messageId: String): Message? {
        return try {
            val message = messageDao.getMessageById(messageId)
            if (message != null && message.isEncrypted) {
                // Decrypt message
                val encryptedMessage = com.google.gson.Gson().fromJson(
                    message.content, 
                    com.hybridmesh.chat.network.encryption.EncryptionManager.EncryptedMessage::class.java
                )
                
                val decryptedContent = encryptionManager.decryptMessage(encryptedMessage)
                if (decryptedContent != null) {
                    val decryptedMessage = message.copy(
                        content = decryptedContent,
                        isEncrypted = false,
                        status = MessageStatus.DELIVERED
                    )
                    
                    messageDao.updateMessage(decryptedMessage)
                    updateServiceStatus()
                    return decryptedMessage
                }
            }
            
            message
        } catch (e: Exception) {
            null
        }
    }
    
    fun getMessagesForConversation(participantId: String): Flow<List<Message>> {
        return messageDao.getMessagesForReceiver(participantId)
    }
    
    fun getAllMessages(): Flow<List<Message>> {
        return messageDao.getMessagesByStatuses(listOf(MessageStatus.SENT, MessageStatus.DELIVERED))
    }
    
    fun getDiscoveredDevices(): Flow<Map<String, com.hybridmesh.chat.data.model.Device>> {
        return discoveryManager.allDiscoveredDevices
    }
    
    fun getNetworkTopology(): Flow<Map<String, List<String>>> {
        return flow {
            while (true) {
                emit(discoveryManager.getNetworkTopology())
                delay(5000) // Update every 5 seconds
            }
        }
    }
    
    fun getQueueStatus(): Flow<com.hybridmesh.chat.service.StoreAndForwardManager.QueueStatus> {
        return storeAndForwardManager.queueStatus
    }
    
    fun getSyncStatus(): Flow<com.hybridmesh.chat.network.messaging.CloudSyncManager.SyncStatus> {
        return cloudSyncManager.syncStatus
    }
    
    suspend fun sharePublicKey(deviceId: String): Boolean {
        return try {
            val publicKey = encryptionManager.getPublicKeyString()
            if (publicKey != null) {
                // Send public key to device
                val keyMessage = Message(
                    content = "PUBLIC_KEY:$publicKey",
                    senderId = getCurrentDeviceId(),
                    receiverId = deviceId,
                    transportType = TransportType.BLUETOOTH
                )
                
                storeAndForwardManager.queueMessage(keyMessage)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun requestPublicKey(deviceId: String): Boolean {
        return try {
            val requestMessage = Message(
                content = "REQUEST_PUBLIC_KEY",
                senderId = getCurrentDeviceId(),
                receiverId = deviceId,
                transportType = TransportType.BLUETOOTH
            )
            
            storeAndForwardManager.queueMessage(requestMessage)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getCurrentDeviceId(): String {
        return com.hybridmesh.chat.utils.DeviceUtils.getDeviceId(context)
    }
    
    private fun updateServiceStatus() {
        serviceScope.launch {
            val discoveredDevices = discoveryManager.allDiscoveredDevices.value.size
            val pendingMessages = storeAndForwardManager.queueStatus.value.pendingMessages
            val isOnline = cloudSyncManager.syncStatus.value.isOnline
            
            _serviceStatus.value = _serviceStatus.value.copy(
                isRunning = true,
                discoveredDevices = discoveredDevices,
                pendingMessages = pendingMessages,
                isOnline = isOnline,
                lastMessageTime = System.currentTimeMillis()
            )
        }
    }
    
    suspend fun startDiscovery() {
        discoveryManager.startDiscovery()
    }
    
    suspend fun stopDiscovery() {
        discoveryManager.stopDiscovery()
    }
    
    suspend fun forceSync() {
        cloudSyncManager.forceSync()
    }
    
    suspend fun clearAllData() {
        messageDao.deleteOldMessages(MessageStatus.DELIVERED, 0)
        deviceDao.deleteOldDevices(0)
        discoveryManager.clearAllDevices()
        encryptionManager.clearAllStoredKeys()
    }
    
    fun shutdown() {
        serviceScope.cancel()
        discoveryManager.stopDiscovery()
        storeAndForwardManager.shutdown()
        cloudSyncManager.shutdown()
    }
}
