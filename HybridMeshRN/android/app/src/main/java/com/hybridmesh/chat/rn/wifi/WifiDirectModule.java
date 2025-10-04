package com.hybridmesh.chat.rn.wifi;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WifiDirectModule extends ReactContextBaseJavaModule {
    private static final String TAG = "WifiDirectModule";
    
    private WifiP2pManager wifiP2pManager;
    private Channel channel;
    private boolean isScanning = false;

    public WifiDirectModule(ReactApplicationContext reactContext) {
        super(reactContext);
        
        wifiP2pManager = (WifiP2pManager) reactContext.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(reactContext, reactContext.getMainLooper(), null);
    }

    @Override
    public String getName() {
        return "WifiDirectModule";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        return constants;
    }

    @ReactMethod
    public void isWifiEnabled(Promise promise) {
        try {
            // TODO: Check if Wi-Fi is enabled
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("WIFI_ERROR", "Failed to check Wi-Fi status", e);
        }
    }

    @ReactMethod
    public void startDiscovery(Promise promise) {
        try {
            if (isScanning) {
                promise.resolve(true);
                return;
            }

            isScanning = true;
            wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Wi-Fi Direct discovery started");
                    sendEvent("onDiscoveryStarted", Arguments.createMap());
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Wi-Fi Direct discovery failed: " + reason);
                    isScanning = false;
                    sendEvent("onDiscoveryFailed", Arguments.createMap());
                }
            });

            // Request peer list
            wifiP2pManager.requestPeers(channel, new PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peers) {
                    WritableArray devices = Arguments.createArray();
                    Collection<WifiP2pDevice> deviceList = peers.getDeviceList();
                    
                    for (WifiP2pDevice device : deviceList) {
                        WritableMap deviceMap = Arguments.createMap();
                        deviceMap.putString("id", device.deviceAddress);
                        deviceMap.putString("name", device.deviceName != null ? device.deviceName : "Unknown Device");
                        deviceMap.putString("macAddress", device.deviceAddress);
                        deviceMap.putInt("signalStrength", 0); // Wi-Fi Direct doesn't provide RSSI directly
                        deviceMap.putBoolean("isOnline", device.status == WifiP2pDevice.CONNECTED);
                        deviceMap.putLong("lastSeen", System.currentTimeMillis());
                        deviceMap.putString("deviceStatus", getDeviceStatusString(device.status));
                        devices.pushMap(deviceMap);
                    }
                    
                    sendEvent("onPeersDiscovered", devices);
                }
            });

            promise.resolve(true);
        } catch (Exception e) {
            isScanning = false;
            promise.reject("WIFI_ERROR", "Failed to start Wi-Fi Direct discovery", e);
        }
    }

    @ReactMethod
    public void stopDiscovery(Promise promise) {
        try {
            if (!isScanning) {
                promise.resolve(true);
                return;
            }

            isScanning = false;
            wifiP2pManager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Wi-Fi Direct discovery stopped");
                    sendEvent("onDiscoveryStopped", Arguments.createMap());
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Failed to stop Wi-Fi Direct discovery: " + reason);
                }
            });

            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("WIFI_ERROR", "Failed to stop Wi-Fi Direct discovery", e);
        }
    }

    @ReactMethod
    public void isScanning(Promise promise) {
        promise.resolve(isScanning);
    }

    @ReactMethod
    public void connectToDevice(String deviceId, Promise promise) {
        try {
            // TODO: Implement Wi-Fi Direct connection
            Log.d(TAG, "Connecting to Wi-Fi Direct device: " + deviceId);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("WIFI_ERROR", "Failed to connect to device", e);
        }
    }

    @ReactMethod
    public void disconnectFromDevice(String deviceId, Promise promise) {
        try {
            // TODO: Implement Wi-Fi Direct disconnection
            Log.d(TAG, "Disconnecting from Wi-Fi Direct device: " + deviceId);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("WIFI_ERROR", "Failed to disconnect from device", e);
        }
    }

    @ReactMethod
    public void sendData(String deviceId, String data, Promise promise) {
        try {
            // TODO: Implement Wi-Fi Direct data transmission
            Log.d(TAG, "Sending data via Wi-Fi Direct to device " + deviceId + ": " + data);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("WIFI_ERROR", "Failed to send data", e);
        }
    }

    private String getDeviceStatusString(int status) {
        switch (status) {
            case WifiP2pDevice.AVAILABLE:
                return "AVAILABLE";
            case WifiP2pDevice.INVITED:
                return "INVITED";
            case WifiP2pDevice.CONNECTED:
                return "CONNECTED";
            case WifiP2pDevice.FAILED:
                return "FAILED";
            case WifiP2pDevice.UNAVAILABLE:
                return "UNAVAILABLE";
            default:
                return "UNKNOWN";
        }
    }

    private void sendEvent(String eventName, Object params) {
        getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }
}

