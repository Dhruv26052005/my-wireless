package com.hybridmesh.chat.network.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.hybridmesh.chat.data.model.Device
import com.hybridmesh.chat.data.model.DeviceCapability
import com.hybridmesh.chat.data.model.TransportType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class BluetoothDiscovery(private val context: Context) {
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    private val _discoveredDevices = MutableStateFlow<Map<String, Device>>(emptyMap())
    val discoveredDevices: StateFlow<Map<String, Device>> = _discoveredDevices.asStateFlow()
    
    private val deviceCache = ConcurrentHashMap<String, Device>()
    private var isScanning = false
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi
            
            // Check if this is our mesh device (look for specific service UUID)
            val meshDevice = createMeshDevice(device, rssi)
            if (meshDevice != null) {
                deviceCache[device.address] = meshDevice
                updateDiscoveredDevices()
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            // Handle scan failure
            stopScanning()
        }
    }
    
    fun startScanning(): Boolean {
        if (!isBluetoothAvailable() || isScanning) {
            return false
        }
        
        return try {
            bluetoothLeScanner?.startScan(scanCallback)
            isScanning = true
            true
        } catch (e: SecurityException) {
            false
        }
    }
    
    fun stopScanning() {
        if (isScanning) {
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
        }
    }
    
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }
    
    private fun createMeshDevice(bluetoothDevice: BluetoothDevice, rssi: Int): Device? {
        val deviceName = bluetoothDevice.name ?: "Unknown Device"
        
        // For demo purposes, accept any device with a name
        // In production, you'd check for specific service UUIDs or manufacturer data
        if (deviceName == "Unknown Device" && bluetoothDevice.address.isBlank()) {
            return null
        }
        
        return Device(
            id = bluetoothDevice.address,
            name = if (deviceName.contains("HybridMesh", ignoreCase = true)) deviceName else "$deviceName (HybridMesh)",
            macAddress = bluetoothDevice.address,
            bluetoothAddress = bluetoothDevice.address,
            signalStrength = rssi,
            transportTypes = listOf(TransportType.BLUETOOTH),
            capabilities = listOf(DeviceCapability.BLUETOOTH_LE),
            lastSeen = System.currentTimeMillis()
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
}
