package com.hybridmesh.chat.network.wifi

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager.Channel
import com.hybridmesh.chat.data.model.Device
import com.hybridmesh.chat.data.model.DeviceCapability
import com.hybridmesh.chat.data.model.TransportType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class WifiDirectDiscovery(private val context: Context) {
    
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel: Channel = wifiP2pManager.initialize(context, context.mainLooper, null)
    
    private val _discoveredDevices = MutableStateFlow<Map<String, Device>>(emptyMap())
    val discoveredDevices: StateFlow<Map<String, Device>> = _discoveredDevices.asStateFlow()
    
    private val deviceCache = ConcurrentHashMap<String, Device>()
    private var isScanning = false
    
    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val devices = peerList.deviceList
        for (device in devices) {
            val meshDevice = createMeshDevice(device)
            if (meshDevice != null) {
                deviceCache[device.deviceAddress] = meshDevice
            }
        }
        updateDiscoveredDevices()
    }
    
    private val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->
        // Handle connection info updates
        if (info != null) {
            // Update device info with connection details
        }
    }
    
    fun startScanning(): Boolean {
        if (!isWifiDirectAvailable() || isScanning) {
            return false
        }
        
        return try {
            wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    isScanning = true
                    // Request peer list after discovery
                    wifiP2pManager.requestPeers(channel, peerListListener)
                }
                
                override fun onFailure(reasonCode: Int) {
                    isScanning = false
                }
            })
            true
        } catch (e: SecurityException) {
            false
        }
    }
    
    fun stopScanning() {
        if (isScanning) {
            wifiP2pManager.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    isScanning = false
                }
                
                override fun onFailure(reasonCode: Int) {
                    // Handle failure
                }
            })
        }
    }
    
    fun isWifiDirectAvailable(): Boolean {
        return wifiManager.isWifiEnabled && wifiP2pManager != null
    }
    
    fun isWifiDirectSupported(): Boolean {
        return context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_WIFI_DIRECT)
    }
    
    private fun createMeshDevice(wifiP2pDevice: WifiP2pDevice): Device? {
        val deviceName = wifiP2pDevice.deviceName
        
        // For demo purposes, accept any device with a name
        // In production, you'd check for specific service identifiers
        if (deviceName.isBlank()) {
            return null
        }
        
        // Convert signal level to approximate distance
        val signalStrength = when (wifiP2pDevice.status) {
            WifiP2pDevice.AVAILABLE -> -50 // Strong signal
            WifiP2pDevice.INVITED -> -60
            WifiP2pDevice.CONNECTED -> -40
            WifiP2pDevice.FAILED -> -80
            else -> -70
        }
        
        return Device(
            id = wifiP2pDevice.deviceAddress,
            name = if (deviceName.contains("HybridMesh", ignoreCase = true)) deviceName else "$deviceName (HybridMesh)",
            macAddress = wifiP2pDevice.deviceAddress,
            signalStrength = signalStrength,
            transportTypes = listOf(TransportType.WIFI_DIRECT),
            capabilities = listOf(DeviceCapability.WIFI_DIRECT),
            lastSeen = System.currentTimeMillis(),
            isOnline = wifiP2pDevice.status == WifiP2pDevice.AVAILABLE || 
                      wifiP2pDevice.status == WifiP2pDevice.CONNECTED
        )
    }
    
    private fun updateDiscoveredDevices() {
        _discoveredDevices.value = deviceCache.toMap()
    }
    
    fun getDeviceByAddress(address: String): Device? {
        return deviceCache[address]
    }
    
    fun clearCache() {
        deviceCache.clear()
        updateDiscoveredDevices()
    }
    
    fun requestPeers() {
        wifiP2pManager.requestPeers(channel, peerListListener)
    }
}
