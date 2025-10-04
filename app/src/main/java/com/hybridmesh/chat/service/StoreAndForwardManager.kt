package com.hybridmesh.chat.service

import com.hybridmesh.chat.data.database.MessageDao
import com.hybridmesh.chat.data.database.DeviceDao
import com.hybridmesh.chat.data.model.Message
import com.hybridmesh.chat.data.model.MessageStatus
import com.hybridmesh.chat.data.model.Device
import com.hybridmesh.chat.network.routing.RoutingEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentLinkedQueue

class StoreAndForwardManager(
    private val messageDao: MessageDao,
    private val deviceDao: DeviceDao,
    private val routingEngine: RoutingEngine
) {
    
    private val messageQueue = ConcurrentLinkedQueue<Message>()
    private val retryQueue = ConcurrentLinkedQueue<Message>()
    private val processingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _queueStatus = MutableStateFlow(QueueStatus())
    val queueStatus: StateFlow<QueueStatus> = _queueStatus.asStateFlow()
    
    private var isProcessing = false
    private val maxRetries = 3
    private val retryDelayMs = 5000L
    private val messageExpiryMs = 300000L // 5 minutes
    
    data class QueueStatus(
        val pendingMessages: Int = 0,
        val retryMessages: Int = 0,
        val processingMessages: Int = 0,
        val lastProcessedTime: Long = 0
    )
    
    init {
        startProcessing()
        startRetryProcessing()
        startCleanup()
    }
    
    suspend fun queueMessage(message: Message) {
        // Store message in database
        messageDao.insertMessage(message)
        
        // Add to processing queue
        messageQueue.offer(message)
        
        updateQueueStatus()
    }
    
    suspend fun queueMessages(messages: List<Message>) {
        if (messages.isEmpty()) return
        
        // Store messages in database
        messageDao.insertMessages(messages)
        
        // Add to processing queue
        messages.forEach { messageQueue.offer(it) }
        
        updateQueueStatus()
    }
    
    private fun startProcessing() {
        processingScope.launch {
            while (isActive) {
                try {
                    processNextMessage()
                    delay(100) // Small delay to prevent busy waiting
                } catch (e: Exception) {
                    // Log error and continue
                }
            }
        }
    }
    
    private fun startRetryProcessing() {
        processingScope.launch {
            while (isActive) {
                try {
                    processRetryQueue()
                    delay(retryDelayMs)
                } catch (e: Exception) {
                    // Log error and continue
                }
            }
        }
    }
    
    private fun startCleanup() {
        processingScope.launch {
            while (isActive) {
                try {
                    cleanupExpiredMessages()
                    delay(60000) // Cleanup every minute
                } catch (e: Exception) {
                    // Log error and continue
                }
            }
        }
    }
    
    private suspend fun processNextMessage() {
        if (isProcessing) return
        
        val message = messageQueue.poll() ?: return
        isProcessing = true
        
        try {
            val success = attemptDelivery(message)
            
            if (success) {
                messageDao.updateMessageStatus(message.id, MessageStatus.SENT)
            } else {
                handleDeliveryFailure(message)
            }
        } catch (e: Exception) {
            handleDeliveryFailure(message)
        } finally {
            isProcessing = false
            updateQueueStatus()
        }
    }
    
    private suspend fun processRetryQueue() {
        val retryMessages = retryQueue.toList()
        retryQueue.clear()
        
        retryMessages.forEach { message ->
            if (message.hopCount < message.maxHops) {
                messageQueue.offer(message)
            } else {
                // Max hops reached, mark as failed
                messageDao.updateMessageStatus(message.id, MessageStatus.FAILED)
            }
        }
        
        updateQueueStatus()
    }
    
    private suspend fun attemptDelivery(message: Message): Boolean {
        // Check if destination is directly reachable
        val destinationDevice = deviceDao.getDeviceById(message.receiverId)
        if (destinationDevice == null || !destinationDevice.isOnline) {
            return false
        }
        
        // Find best route to destination
        val route = routingEngine.findBestRoute(message.senderId, message.receiverId)
        if (route == null) {
            return false
        }
        
        // Attempt to send message through the route
        return try {
            val success = sendMessageThroughRoute(message, route)
            if (success) {
                messageDao.updateMessageStatus(message.id, MessageStatus.SENT)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun sendMessageThroughRoute(message: Message, route: Route): Boolean {
        // This would integrate with the actual messaging system
        // For now, we'll simulate the sending process
        
        val updatedMessage = message.copy(
            hopCount = message.hopCount + 1,
            route = message.route + route.path.map { it.deviceId }
        )
        
        messageDao.updateMessage(updatedMessage)
        
        // Simulate network delay
        delay(100)
        
        // In a real implementation, this would:
        // 1. Connect to the next hop device
        // 2. Send the encrypted message
        // 3. Wait for acknowledgment
        // 4. Return success/failure
        
        return true // Simulated success
    }
    
    private suspend fun handleDeliveryFailure(message: Message) {
        val updatedMessage = message.copy(hopCount = message.hopCount + 1)
        
        if (updatedMessage.hopCount < updatedMessage.maxHops) {
            // Add to retry queue
            retryQueue.offer(updatedMessage)
            messageDao.updateMessage(updatedMessage)
        } else {
            // Max hops reached, mark as failed
            messageDao.updateMessageStatus(message.id, MessageStatus.FAILED)
        }
    }
    
    private suspend fun cleanupExpiredMessages() {
        val cutoffTime = System.currentTimeMillis() - messageExpiryMs
        val expiredMessages = messageDao.getExpiredMessages(
            cutoffTime, 
            listOf(MessageStatus.PENDING, MessageStatus.SENT)
        )
        
        expiredMessages.forEach { message ->
            messageDao.updateMessageStatus(message.id, MessageStatus.EXPIRED)
        }
        
        // Clean up old delivered messages
        messageDao.deleteOldMessages(MessageStatus.DELIVERED, cutoffTime)
    }
    
    private fun updateQueueStatus() {
        _queueStatus.value = QueueStatus(
            pendingMessages = messageQueue.size,
            retryMessages = retryQueue.size,
            processingMessages = if (isProcessing) 1 else 0,
            lastProcessedTime = System.currentTimeMillis()
        )
    }
    
    suspend fun getPendingMessages(limit: Int = 10): List<Message> {
        return messageDao.getPendingMessages(MessageStatus.PENDING, limit)
    }
    
    suspend fun getRetryMessages(): List<Message> {
        return retryQueue.toList()
    }
    
    suspend fun clearQueue() {
        messageQueue.clear()
        retryQueue.clear()
        updateQueueStatus()
    }
    
    suspend fun pauseProcessing() {
        isProcessing = true
    }
    
    suspend fun resumeProcessing() {
        isProcessing = false
    }
    
    fun shutdown() {
        processingScope.cancel()
    }
}
