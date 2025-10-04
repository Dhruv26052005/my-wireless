export interface Device {
  id: string;
  name: string;
  macAddress: string;
  ipAddress?: string;
  bluetoothAddress?: string;
  lastSeen: number;
  signalStrength: number; // RSSI for Bluetooth, signal level for Wi-Fi
  transportTypes: TransportType[];
  isOnline: boolean;
  publicKey?: string; // For encryption
  capabilities: DeviceCapability[];
}

export enum DeviceCapability {
  BLUETOOTH_LE = 'BLUETOOTH_LE',
  WIFI_DIRECT = 'WIFI_DIRECT',
  HOTSPOT = 'HOTSPOT',
  INTERNET_RELAY = 'INTERNET_RELAY',
  MESSAGE_STORE = 'MESSAGE_STORE'
}

export enum TransportType {
  BLUETOOTH = 'BLUETOOTH',
  WIFI_DIRECT = 'WIFI_DIRECT',
  HOTSPOT = 'HOTSPOT',
  INTERNET = 'INTERNET'
}

export const createDevice = (data: Partial<Device>): Device => ({
  id: data.id || generateUUID(),
  name: data.name || '',
  macAddress: data.macAddress || '',
  ipAddress: data.ipAddress,
  bluetoothAddress: data.bluetoothAddress,
  lastSeen: data.lastSeen || Date.now(),
  signalStrength: data.signalStrength || 0,
  transportTypes: data.transportTypes || [],
  isOnline: data.isOnline ?? true,
  publicKey: data.publicKey,
  capabilities: data.capabilities || []
});

const generateUUID = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
};
