import { NativeModules, NativeEventEmitter } from 'react-native';

// Bluetooth Module Interface
export interface BluetoothModuleInterface {
  isBluetoothEnabled(): Promise<boolean>;
  startScan(): Promise<boolean>;
  stopScan(): Promise<boolean>;
  isScanning(): Promise<boolean>;
  getPairedDevices(): Promise<DeviceInfo[]>;
  connectToDevice(deviceId: string): Promise<boolean>;
  disconnectFromDevice(deviceId: string): Promise<boolean>;
  sendData(deviceId: string, data: string): Promise<boolean>;
}

// Wi-Fi Direct Module Interface
export interface WifiDirectModuleInterface {
  isWifiEnabled(): Promise<boolean>;
  startDiscovery(): Promise<boolean>;
  stopDiscovery(): Promise<boolean>;
  isScanning(): Promise<boolean>;
  connectToDevice(deviceId: string): Promise<boolean>;
  disconnectFromDevice(deviceId: string): Promise<boolean>;
  sendData(deviceId: string, data: string): Promise<boolean>;
}

// Device Info from Native Modules
export interface DeviceInfo {
  id: string;
  name: string;
  macAddress: string;
  signalStrength?: number;
  bluetoothAddress?: string;
  isOnline: boolean;
  lastSeen: number;
  hasMeshService?: boolean;
  deviceStatus?: string;
}

// Event Emitters
export interface BluetoothEvents {
  onDeviceDiscovered: (device: DeviceInfo) => void;
  onScanFailed: () => void;
}

export interface WifiDirectEvents {
  onDiscoveryStarted: () => void;
  onDiscoveryStopped: () => void;
  onDiscoveryFailed: () => void;
  onPeersDiscovered: (devices: DeviceInfo[]) => void;
}

// Get Native Modules
export const BluetoothModule = NativeModules.BluetoothModule as BluetoothModuleInterface;
export const WifiDirectModule = NativeModules.WifiDirectModule as WifiDirectModuleInterface;

// Create Event Emitters
export const bluetoothEventEmitter = new NativeEventEmitter(BluetoothModule as any);
export const wifiDirectEventEmitter = new NativeEventEmitter(WifiDirectModule as any);

