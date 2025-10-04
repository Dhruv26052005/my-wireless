package com.hybridmesh.chat.network

import android.content.Context
import com.hybridmesh.chat.data.model.Device
import com.hybridmesh.chat.data.model.TransportType
import com.hybridmesh.chat.network.bluetooth.BluetoothDiscovery
import com.hybridmesh.chat.network.wifi.WifiDirectDiscovery
import com.hybridmesh.chat.utils.DemoDataGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class DiscoveryManager(private val context: Context) {
    
    private val bluetoothDiscovery = BluetoothDiscovery(context)
    private val wifiDirectDiscovery = WifiDirectDiscovery(context)
    
    private val _allDiscoveredDevices = MutableStateFlow<Map<String, Device>>(emptyMap())
    val allDiscoveredDevices: StateFlow<Map<String, Device>> = _allDiscoveredDevices.asStateFlow()
    
    private val deviceCache = ConcurrentHashMap<String, Device>()
    private var discoveryJob: Job? = null
    private var isDiscoveryActive = false
    private var demoMode = true // Enable demo mode for testing
    
    init {
        // Combine both discovery streams
        CoroutineScope(Dispatchers.IO).launch {
            combine(
                bluetoothDiscovery.discoveredDevices,
                wifiDirectDiscovery.discoveredDevices
            ) { bluetoothDevices, wifiDevices ->
                mergeDevices(bluetoothDevices, wifiDevices)
            }.collect { mergedDevices ->
                _allDiscoveredDevices.value = mergedDevices
            }
        }
    }
    
    fun startDiscovery(): Boolean {
        if (isDiscoveryActive) {
            return true
        }
        
        if (demoMode) {
            // Add demo devices for testing
            addDemoDevices()
            isDiscoveryActive = true
            startPeriodicCleanup()
            return true
        }
        
        val bluetoothStarted = bluetoothDiscovery.startScanning()
        val wifiStarted = wifiDirectDiscovery.startScanning()
        
        if (bluetoothStarted || wifiStarted) {
            isDiscoveryActive = true
            startPeriodicCleanup()
            return true
        }
        
        return false
    }
    
    fun stopDiscovery() {
        bluetoothDiscovery.stopScanning()
        wifiDirectDiscovery.stopScanning()
        isDiscoveryActive = false
        discoveryJob?.cancel()
    }
    
    fun isDiscoveryActive(): Boolean {
        return isDiscoveryActive
    }
    
    fun getBestTransportForDevice(deviceId: String): TransportType? {
        val device = deviceCache[deviceId] ?: return null
        
        // Prioritize transport based on signal strength and capabilities
        return when {
            device.transportTypes.contains(TransportType.BLUETOOTH) && 
            device.signalStrength > -60 -> TransportType.BLUETOOTH
            
            device.transportTypes.contains(TransportType.WIFI_DIRECT) && 
            device.signalStrength > -70 -> TransportType.WIFI_DIRECT
            
            device.transportTypes.contains(TransportType.HOTSPOT) -> TransportType.HOTSPOT
            
            else -> device.transportTypes.firstOrNull()
        }
    }
    
    fun getDevicesByTransport(transportType: TransportType): List<Device> {
        return deviceCache.values.filter { device ->
            device.transportTypes.contains(transportType) && device.isOnline
        }
    }
    
    fun getDeviceById(deviceId: String): Device? {
        return deviceCache[deviceId]
    }
    
    private fun mergeDevices(bluetoothDevices: Map<String, Device>, wifiDevices: Map<String, Device>): Map<String, Device> {
        val mergedDevices = ConcurrentHashMap<String, Device>()
        
        // Add all devices, merging capabilities for devices found on both transports
        bluetoothDevices.forEach { (id, device) ->
            mergedDevices[id] = device
        }
        
        wifiDevices.forEach { (id, wifiDevice) ->
            val existingDevice = mergedDevices[id]
            if (existingDevice != null) {
                // Merge capabilities and transport types
                val mergedDevice = existingDevice.copy(
                    transportTypes = (existingDevice.transportTypes + wifiDevice.transportTypes).distinct(),
                    capabilities = (existingDevice.capabilities + wifiDevice.capabilities).distinct(),
                    signalStrength = maxOf(existingDevice.signalStrength, wifiDevice.signalStrength),
                    lastSeen = maxOf(existingDevice.lastSeen, wifiDevice.lastSeen)
                )
                mergedDevices[id] = mergedDevice
            } else {
                mergedDevices[id] = wifiDevice
            }
        }
        
        deviceCache.putAll(mergedDevices)
        return mergedDevices.toMap()
    }
    
    private fun startPeriodicCleanup() {
        discoveryJob = CoroutineScope(Dispatchers.IO).launch {
            while (isDiscoveryActive) {
                delay(30000) // Clean up every 30 seconds
                cleanupStaleDevices()
            }
        }
    }
    
    private fun cleanupStaleDevices() {
        val currentTime = System.currentTimeMillis()
        val staleThreshold = 60000 // 1 minute
        
        val staleDevices = deviceCache.values.filter { device ->
            currentTime - device.lastSeen > staleThreshold
        }
        
        staleDevices.forEach { device ->
            deviceCache.remove(device.id)
        }
        
        if (staleDevices.isNotEmpty()) {
            _allDiscoveredDevices.value = deviceCache.toMap()
        }
    }
    
    fun clearAllDevices() {
        deviceCache.clear()
        bluetoothDiscovery.clearCache()
        wifiDirectDiscovery.clearCache()
        _allDiscoveredDevices.value = emptyMap()
    }
    
    private fun addDemoDevices() {
        val demoDevices = DemoDataGenerator.generateDemoDevices()
        demoDevices.forEach { device ->
            deviceCache[device.id] = device
        }
        updateDiscoveredDevices()
    }
    
    private fun updateDiscoveredDevices() {
        _allDiscoveredDevices.value = deviceCache.toMap()
    }
    
    fun getNetworkTopology(): Map<String, List<String>> {
        // Return a simple topology map showing which devices can reach which others
        val topology = mutableMapOf<String, List<String>>()
        
        deviceCache.values.forEach { device ->
            val reachableDevices = deviceCache.values
                .filter { otherDevice ->
                    otherDevice.id != device.id && 
                    canReach(device, otherDevice)
                }
                .map { it.id }
            
            topology[device.id] = reachableDevices
        }
        
        return topology
    }
    
    private fun canReach(from: Device, to: Device): Boolean {
        // Simple reachability check based on signal strength and transport types
        return when {
            from.transportTypes.contains(TransportType.BLUETOOTH) && 
            to.transportTypes.contains(TransportType.BLUETOOTH) &&
            from.signalStrength > -70 -> true
            
            from.transportTypes.contains(TransportType.WIFI_DIRECT) && 
            to.transportTypes.contains(TransportType.WIFI_DIRECT) &&
            from.signalStrength > -80 -> true
            
            else -> false
        }
    }
}
