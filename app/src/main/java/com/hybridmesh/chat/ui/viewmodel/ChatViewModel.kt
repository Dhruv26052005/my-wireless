package com.hybridmesh.chat.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hybridmesh.chat.data.model.Device
import com.hybridmesh.chat.data.model.Message
import com.hybridmesh.chat.network.messaging.MeshMessagingService
import com.hybridmesh.chat.utils.DeviceUtils
import com.hybridmesh.chat.utils.DemoDataGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val meshService: MeshMessagingService,
    private val context: Context
) : ViewModel() {
    
    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice.asStateFlow()
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    init {
        observeMeshService()
        loadDemoData()
    }
    
    private fun observeMeshService() {
        viewModelScope.launch {
            // Observe discovered devices
            meshService.getDiscoveredDevices().collect { devices ->
                _discoveredDevices.value = devices.values.toList()
            }
        }
        
        viewModelScope.launch {
            // Observe service status
            meshService.serviceStatus.collect { status ->
                _isConnected.value = status.isRunning && status.discoveredDevices > 0
            }
        }
    }
    
    fun selectDevice(device: Device) {
        _selectedDevice.value = device
        loadMessagesForDevice(device.id)
    }
    
    private fun loadMessagesForDevice(deviceId: String) {
        viewModelScope.launch {
            meshService.getMessagesForConversation(deviceId).collect { messages ->
                _messages.value = messages
            }
        }
    }
    
    fun sendMessage(content: String) {
        val device = _selectedDevice.value ?: return
        if (content.isBlank()) return
        
        viewModelScope.launch {
            _isSending.value = true
            try {
                val success = meshService.sendMessage(content, device.id)
                if (success) {
                    // Message sent successfully
                } else {
                    // Handle send failure
                }
            } finally {
                _isSending.value = false
            }
        }
    }
    
    fun startDiscovery() {
        viewModelScope.launch {
            meshService.startDiscovery()
        }
    }
    
    fun stopDiscovery() {
        viewModelScope.launch {
            meshService.stopDiscovery()
        }
    }
    
    fun sharePublicKey(device: Device) {
        viewModelScope.launch {
            meshService.sharePublicKey(device.id)
        }
    }
    
    fun requestPublicKey(device: Device) {
        viewModelScope.launch {
            meshService.requestPublicKey(device.id)
        }
    }
    
    fun forceSync() {
        viewModelScope.launch {
            meshService.forceSync()
        }
    }
    
    private fun loadDemoData() {
        viewModelScope.launch {
            // Add demo messages for testing
            val demoMessages = DemoDataGenerator.generateDemoMessages()
            _messages.value = demoMessages
        }
    }
}
