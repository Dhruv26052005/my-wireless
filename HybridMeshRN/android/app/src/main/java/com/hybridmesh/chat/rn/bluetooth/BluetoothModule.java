package com.hybridmesh.chat.rn.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BluetoothModule extends ReactContextBaseJavaModule {
    private static final String TAG = "BluetoothModule";
    private static final String MESH_SERVICE_UUID = "12345678-1234-1234-1234-123456789ABC";
    
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;
    private ScanCallback scanCallback;

    public BluetoothModule(ReactApplicationContext reactContext) {
        super(reactContext);
        
        BluetoothManager bluetoothManager = (BluetoothManager) reactContext.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        
        setupScanCallback();
    }

    @Override
    public String getName() {
        return "BluetoothModule";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("MESH_SERVICE_UUID", MESH_SERVICE_UUID);
        return constants;
    }

    private void setupScanCallback() {
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                WritableMap deviceMap = Arguments.createMap();
                
                deviceMap.putString("id", device.getAddress());
                deviceMap.putString("name", device.getName() != null ? device.getName() : "Unknown Device");
                deviceMap.putString("macAddress", device.getAddress());
                deviceMap.putInt("signalStrength", result.getRssi());
                deviceMap.putString("bluetoothAddress", device.getAddress());
                deviceMap.putBoolean("isOnline", true);
                deviceMap.putLong("lastSeen", System.currentTimeMillis());
                
                // Check if device has our mesh service
                boolean hasMeshService = false;
                if (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
                    for (ParcelUuid uuid : result.getScanRecord().getServiceUuids()) {
                        if (MESH_SERVICE_UUID.equals(uuid.toString())) {
                            hasMeshService = true;
                            break;
                        }
                    }
                }
                deviceMap.putBoolean("hasMeshService", hasMeshService);
                
                sendEvent("onDeviceDiscovered", deviceMap);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "BLE scan failed with error: " + errorCode);
                sendEvent("onScanFailed", Arguments.createMap());
            }
        };
    }

    @ReactMethod
    public void isBluetoothEnabled(Promise promise) {
        try {
            boolean isEnabled = bluetoothAdapter != null && bluetoothAdapter.isEnabled();
            promise.resolve(isEnabled);
        } catch (Exception e) {
            promise.reject("BLUETOOTH_ERROR", "Failed to check Bluetooth status", e);
        }
    }

    @ReactMethod
    public void startScan(Promise promise) {
        try {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                promise.reject("BLUETOOTH_ERROR", "Bluetooth is not enabled");
                return;
            }

            if (isScanning) {
                promise.resolve(true);
                return;
            }

            isScanning = true;
            bluetoothLeScanner.startScan(scanCallback);
            promise.resolve(true);
        } catch (Exception e) {
            isScanning = false;
            promise.reject("BLUETOOTH_ERROR", "Failed to start BLE scan", e);
        }
    }

    @ReactMethod
    public void stopScan(Promise promise) {
        try {
            if (!isScanning) {
                promise.resolve(true);
                return;
            }

            isScanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("BLUETOOTH_ERROR", "Failed to stop BLE scan", e);
        }
    }

    @ReactMethod
    public void isScanning(Promise promise) {
        promise.resolve(isScanning);
    }

    @ReactMethod
    public void getPairedDevices(Promise promise) {
        try {
            WritableArray devices = Arguments.createArray();
            
            if (bluetoothAdapter != null) {
                for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                    WritableMap deviceMap = Arguments.createMap();
                    deviceMap.putString("id", device.getAddress());
                    deviceMap.putString("name", device.getName() != null ? device.getName() : "Unknown Device");
                    deviceMap.putString("macAddress", device.getAddress());
                    deviceMap.putString("bluetoothAddress", device.getAddress());
                    deviceMap.putBoolean("isOnline", true);
                    deviceMap.putLong("lastSeen", System.currentTimeMillis());
                    devices.pushMap(deviceMap);
                }
            }
            
            promise.resolve(devices);
        } catch (Exception e) {
            promise.reject("BLUETOOTH_ERROR", "Failed to get paired devices", e);
        }
    }

    @ReactMethod
    public void connectToDevice(String deviceId, Promise promise) {
        try {
            // TODO: Implement BLE connection logic
            // This would involve connecting to the device and establishing a GATT connection
            Log.d(TAG, "Connecting to device: " + deviceId);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("BLUETOOTH_ERROR", "Failed to connect to device", e);
        }
    }

    @ReactMethod
    public void disconnectFromDevice(String deviceId, Promise promise) {
        try {
            // TODO: Implement BLE disconnection logic
            Log.d(TAG, "Disconnecting from device: " + deviceId);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("BLUETOOTH_ERROR", "Failed to disconnect from device", e);
        }
    }

    @ReactMethod
    public void sendData(String deviceId, String data, Promise promise) {
        try {
            // TODO: Implement BLE data transmission
            Log.d(TAG, "Sending data to device " + deviceId + ": " + data);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("BLUETOOTH_ERROR", "Failed to send data", e);
        }
    }

    private void sendEvent(String eventName, WritableMap params) {
        getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }
}

