package com.hybridmesh.chat.network.messaging

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.hybridmesh.chat.data.database.MessageDao
import com.hybridmesh.chat.data.database.DeviceDao
import com.hybridmesh.chat.data.model.Message
import com.hybridmesh.chat.data.model.MessageStatus
import com.hybridmesh.chat.data.model.Device
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue

class CloudSyncManager(
    private val context: Context,
    private val messageDao: MessageDao,
    private val deviceDao: DeviceDao
) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val pendingSyncQueue = ConcurrentLinkedQueue<Message>()
    private var isOnline = false
    private var isSyncing = false
    
    // Cloud server configuration (in a real app, these would be configurable)
    private val cloudServerUrl = "https://your-cloud-server.com/api"
    private val apiKey = "your-api-key" // Should be stored securely
    
    data class SyncStatus(
        val isOnline: Boolean = false,
        val isSyncing: Boolean = false,
        val lastSyncTime: Long = 0,
        val pendingMessages: Int = 0,
        val syncedMessages: Int = 0,
        val failedMessages: Int = 0
    )
    
    init {
        startNetworkMonitoring()
        startSyncProcessing()
    }
    
    private fun startNetworkMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
                updateSyncStatus()
                startSync()
            }
            
            override fun onLost(network: Network) {
                isOnline = false
                updateSyncStatus()
            }
        })
        
        // Check initial network state
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        isOnline = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        updateSyncStatus()
    }
    
    private fun startSyncProcessing() {
        syncScope.launch {
            while (isActive) {
                try {
                    if (isOnline && !isSyncing) {
                        processSyncQueue()
                    }
                    delay(30000) // Check every 30 seconds
                } catch (e: Exception) {
                    // Log error and continue
                }
            }
        }
    }
    
    suspend fun queueForSync(message: Message) {
        if (message.status == MessageStatus.SENT || message.status == MessageStatus.DELIVERED) {
            pendingSyncQueue.offer(message)
            updateSyncStatus()
        }
    }
    
    suspend fun queueMessagesForSync(messages: List<Message>) {
        val syncableMessages = messages.filter { 
            it.status == MessageStatus.SENT || it.status == MessageStatus.DELIVERED 
        }
        syncableMessages.forEach { pendingSyncQueue.offer(it) }
        updateSyncStatus()
    }
    
    private suspend fun startSync() {
        if (isSyncing || !isOnline) return
        
        syncScope.launch {
            isSyncing = true
            updateSyncStatus()
            
            try {
                // Sync pending messages
                syncPendingMessages()
                
                // Sync device information
                syncDeviceInfo()
                
                // Download messages from cloud
                downloadMessagesFromCloud()
                
            } catch (e: Exception) {
                // Handle sync error
            } finally {
                isSyncing = false
                updateSyncStatus()
            }
        }
    }
    
    private suspend fun processSyncQueue() {
        val messagesToSync = mutableListOf<Message>()
        
        // Collect messages from queue
        while (pendingSyncQueue.isNotEmpty()) {
            pendingSyncQueue.poll()?.let { messagesToSync.add(it) }
        }
        
        if (messagesToSync.isNotEmpty()) {
            syncMessagesToCloud(messagesToSync)
        }
    }
    
    private suspend fun syncPendingMessages() {
        val pendingMessages = messageDao.getMessagesByStatuses(
            listOf(MessageStatus.SENT, MessageStatus.DELIVERED)
        ).first()
        
        if (pendingMessages.isNotEmpty()) {
            syncMessagesToCloud(pendingMessages)
        }
    }
    
    private suspend fun syncMessagesToCloud(messages: List<Message>) {
        try {
            val response = makeCloudRequest("POST", "/messages/sync", messages)
            
            if (response.isSuccessful) {
                // Mark messages as synced
                messages.forEach { message ->
                    messageDao.updateMessageStatus(message.id, MessageStatus.DELIVERED)
                }
                
                _syncStatus.value = _syncStatus.value.copy(
                    syncedMessages = _syncStatus.value.syncedMessages + messages.size
                )
            } else {
                // Handle sync failure
                _syncStatus.value = _syncStatus.value.copy(
                    failedMessages = _syncStatus.value.failedMessages + messages.size
                )
            }
        } catch (e: Exception) {
            // Handle network error
            _syncStatus.value = _syncStatus.value.copy(
                failedMessages = _syncStatus.value.failedMessages + messages.size
            )
        }
    }
    
    private suspend fun syncDeviceInfo() {
        try {
            val devices = deviceDao.getOnlineDevices().first()
            val response = makeCloudRequest("POST", "/devices/sync", devices)
            
            if (response.isSuccessful) {
                // Device info synced successfully
            }
        } catch (e: Exception) {
            // Handle sync error
        }
    }
    
    private suspend fun downloadMessagesFromCloud() {
        try {
            val lastSyncTime = _syncStatus.value.lastSyncTime
            val response = makeCloudRequest("GET", "/messages/download?since=$lastSyncTime", null)
            
            if (response.isSuccessful) {
                val downloadedMessages = response.data as? List<Message> ?: emptyList()
                
                if (downloadedMessages.isNotEmpty()) {
                    messageDao.insertMessages(downloadedMessages)
                }
                
                _syncStatus.value = _syncStatus.value.copy(
                    lastSyncTime = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            // Handle download error
        }
    }
    
    private suspend fun makeCloudRequest(method: String, endpoint: String, data: Any?): CloudResponse {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$cloudServerUrl$endpoint")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = method
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                if (data != null && (method == "POST" || method == "PUT")) {
                    connection.doOutput = true
                    val json = com.google.gson.Gson().toJson(data)
                    connection.outputStream.use { it.write(json.toByteArray()) }
                }
                
                val responseCode = connection.responseCode
                val responseBody = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: ""
                }
                
                CloudResponse(
                    isSuccessful = responseCode in 200..299,
                    statusCode = responseCode,
                    data = if (responseBody.isNotEmpty()) {
                        com.google.gson.Gson().fromJson(responseBody, Any::class.java)
                    } else null
                )
            } catch (e: IOException) {
                CloudResponse(isSuccessful = false, statusCode = -1, data = null)
            }
        }
    }
    
    private fun updateSyncStatus() {
        _syncStatus.value = _syncStatus.value.copy(
            isOnline = isOnline,
            isSyncing = isSyncing,
            pendingMessages = pendingSyncQueue.size
        )
    }
    
    suspend fun forceSync() {
        if (isOnline) {
            startSync()
        }
    }
    
    suspend fun clearSyncQueue() {
        pendingSyncQueue.clear()
        updateSyncStatus()
    }
    
    fun shutdown() {
        syncScope.cancel()
    }
    
    data class CloudResponse(
        val isSuccessful: Boolean,
        val statusCode: Int,
        val data: Any?
    )
}
