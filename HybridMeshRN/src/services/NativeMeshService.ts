import { Device, createDevice, TransportType, DeviceCapability } from '../models/Device';
import { Message, createMessage, MessageStatus } from '../models/Message';
import { 
  BluetoothModule, 
  WifiDirectModule, 
  bluetoothEventEmitter, 
  wifiDirectEventEmitter,
  DeviceInfo 
} from '../native/NativeModules';
import { EncryptionService } from './EncryptionService';
import { DatabaseService } from './DatabaseService';

export class NativeMeshService {
  private static instance: NativeMeshService;
  private devices: Map<string, Device> = new Map();
  private messages: Map<string, Message> = new Map();
  private encryptionService: EncryptionService;
  private databaseService: DatabaseService;
  private isInitialized: boolean = false;

  static getInstance(): NativeMeshService {
    if (!NativeMeshService.instance) {
      NativeMeshService.instance = new NativeMeshService();
    }
    return NativeMeshService.instance;
  }

  private constructor() {
    this.encryptionService = EncryptionService.getInstance();
    this.databaseService = DatabaseService.getInstance();
    this.setupEventListeners();
  }

  async initialize(): Promise<boolean> {
    try {
      console.log('Initializing Native Mesh Service...');
      
      // Initialize services
      await this.encryptionService.initialize();
      await this.databaseService.initialize();
      
      // Load existing data
      await this.loadDevicesFromDatabase();
      await this.loadMessagesFromDatabase();
      
      this.isInitialized = true;
      console.log('Native Mesh Service initialized successfully');
      return true;
    } catch (error) {
      console.error('Failed to initialize Native Mesh Service:', error);
      return false;
    }
  }

  private setupEventListeners(): void {
    // Bluetooth event listeners
    bluetoothEventEmitter.addListener('onDeviceDiscovered', (deviceInfo: DeviceInfo) => {
      this.handleDeviceDiscovered(deviceInfo, TransportType.BLUETOOTH);
    });

    bluetoothEventEmitter.addListener('onScanFailed', () => {
      console.error('Bluetooth scan failed');
    });

    // Wi-Fi Direct event listeners
    wifiDirectEventEmitter.addListener('onPeersDiscovered', (devices: DeviceInfo[]) => {
      devices.forEach(deviceInfo => {
        this.handleDeviceDiscovered(deviceInfo, TransportType.WIFI_DIRECT);
      });
    });

    wifiDirectEventEmitter.addListener('onDiscoveryFailed', () => {
      console.error('Wi-Fi Direct discovery failed');
    });
  }

  private async handleDeviceDiscovered(deviceInfo: DeviceInfo, transportType: TransportType): Promise<void> {
    try {
      const device = createDevice({
        id: deviceInfo.id,
        name: deviceInfo.name,
        macAddress: deviceInfo.macAddress,
        bluetoothAddress: deviceInfo.bluetoothAddress,
        signalStrength: deviceInfo.signalStrength || 0,
        transportTypes: [transportType],
        isOnline: deviceInfo.isOnline,
        lastSeen: deviceInfo.lastSeen,
        capabilities: this.determineCapabilities(deviceInfo, transportType),
      });

      // Update or add device
      this.devices.set(device.id, device);
      await this.databaseService.saveDevice(device);

      console.log(`Device discovered via ${transportType}: ${device.name}`);
    } catch (error) {
      console.error('Failed to handle discovered device:', error);
    }
  }

  private determineCapabilities(deviceInfo: DeviceInfo, transportType: TransportType): DeviceCapability[] {
    const capabilities: DeviceCapability[] = [];

    if (transportType === TransportType.BLUETOOTH) {
      capabilities.push(DeviceCapability.BLUETOOTH_LE);
    } else if (transportType === TransportType.WIFI_DIRECT) {
      capabilities.push(DeviceCapability.WIFI_DIRECT);
    }

    // Check for additional capabilities based on device info
    if (deviceInfo.hasMeshService) {
      capabilities.push(DeviceCapability.MESSAGE_STORE);
    }

    return capabilities;
  }

  // Discovery Management
  async startDiscovery(): Promise<void> {
    try {
      console.log('Starting mesh discovery...');
      
      // Start Bluetooth discovery
      const bluetoothEnabled = await BluetoothModule.isBluetoothEnabled();
      if (bluetoothEnabled) {
        await BluetoothModule.startScan();
      }

      // Start Wi-Fi Direct discovery
      const wifiEnabled = await WifiDirectModule.isWifiEnabled();
      if (wifiEnabled) {
        await WifiDirectModule.startDiscovery();
      }

      console.log('Mesh discovery started');
    } catch (error) {
      console.error('Failed to start discovery:', error);
      throw error;
    }
  }

  async stopDiscovery(): Promise<void> {
    try {
      console.log('Stopping mesh discovery...');
      
      await BluetoothModule.stopScan();
      await WifiDirectModule.stopDiscovery();
      
      console.log('Mesh discovery stopped');
    } catch (error) {
      console.error('Failed to stop discovery:', error);
      throw error;
    }
  }

  // Device Management
  getDevices(): Device[] {
    return Array.from(this.devices.values());
  }

  getDevice(deviceId: string): Device | undefined {
    return this.devices.get(deviceId);
  }

  async updateDeviceStatus(deviceId: string, isOnline: boolean): Promise<void> {
    const device = this.devices.get(deviceId);
    if (device) {
      device.isOnline = isOnline;
      device.lastSeen = Date.now();
      this.devices.set(deviceId, device);
      await this.databaseService.saveDevice(device);
    }
  }

  // Message Management
  async sendMessage(message: Message): Promise<boolean> {
    try {
      console.log(`Sending message to ${message.receiverId}: ${message.content}`);
      
      // Encrypt message if needed
      let content = message.content;
      if (message.isEncrypted) {
        const recipient = this.devices.get(message.receiverId);
        if (recipient?.publicKey) {
          content = await this.encryptionService.encryptMessage(message.content, recipient.publicKey);
        }
      }

      // Find the best route
      const route = this.findRoute(message.receiverId);
      if (!route) {
        throw new Error('No route found to destination');
      }

      // Send via appropriate transport
      const success = await this.sendViaTransport(message, content, route);
      
      if (success) {
        message.status = MessageStatus.SENT;
        this.messages.set(message.id, message);
        await this.databaseService.saveMessage(message);
      } else {
        message.status = MessageStatus.FAILED;
      }

      return success;
    } catch (error) {
      console.error('Failed to send message:', error);
      message.status = MessageStatus.FAILED;
      this.messages.set(message.id, message);
      await this.databaseService.saveMessage(message);
      return false;
    }
  }

  private async sendViaTransport(message: Message, content: string, route: any): Promise<boolean> {
    // TODO: Implement actual transport sending
    // This would involve sending via Bluetooth or Wi-Fi Direct based on the route
    console.log(`Sending via transport: ${message.transportType}`);
    return true;
  }

  private findRoute(destinationId: string): any {
    // TODO: Implement mesh routing algorithm
    console.log(`Finding route to ${destinationId}`);
    return { destination: destinationId, path: [], totalCost: 0 };
  }

  getMessages(): Message[] {
    return Array.from(this.messages.values());
  }

  async loadDevicesFromDatabase(): Promise<void> {
    try {
      const devices = await this.databaseService.getDevices();
      devices.forEach(device => {
        this.devices.set(device.id, device);
      });
      console.log(`Loaded ${devices.length} devices from database`);
    } catch (error) {
      console.error('Failed to load devices from database:', error);
    }
  }

  async loadMessagesFromDatabase(): Promise<void> {
    try {
      const messages = await this.databaseService.getMessages();
      messages.forEach(message => {
        this.messages.set(message.id, message);
      });
      console.log(`Loaded ${messages.length} messages from database`);
    } catch (error) {
      console.error('Failed to load messages from database:', error);
    }
  }

  // Network Status
  isNetworkAvailable(): boolean {
    return this.devices.size > 0;
  }

  getNetworkTopology(): any {
    return {
      deviceCount: this.devices.size,
      devices: this.getDevices(),
      isInitialized: this.isInitialized,
    };
  }
}

