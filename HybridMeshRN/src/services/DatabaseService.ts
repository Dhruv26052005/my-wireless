import SQLite from 'react-native-sqlite-storage';
import { Device } from '../models/Device';
import { Message } from '../models/Message';

export class DatabaseService {
  private static instance: DatabaseService;
  private db: SQLite.SQLiteDatabase | null = null;

  static getInstance(): DatabaseService {
    if (!DatabaseService.instance) {
      DatabaseService.instance = new DatabaseService();
    }
    return DatabaseService.instance;
  }

  async initialize(): Promise<boolean> {
    try {
      console.log('Initializing database...');
      
      // Open database
      this.db = await SQLite.openDatabase({
        name: 'HybridMesh.db',
        location: 'default',
      });

      // Create tables
      await this.createTables();
      
      console.log('Database initialized successfully');
      return true;
    } catch (error) {
      console.error('Failed to initialize database:', error);
      return false;
    }
  }

  private async createTables(): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    // Create devices table
    await this.db.executeSql(`
      CREATE TABLE IF NOT EXISTS devices (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        macAddress TEXT NOT NULL,
        ipAddress TEXT,
        bluetoothAddress TEXT,
        lastSeen INTEGER NOT NULL,
        signalStrength INTEGER NOT NULL,
        transportTypes TEXT NOT NULL,
        isOnline INTEGER NOT NULL,
        publicKey TEXT,
        capabilities TEXT NOT NULL
      )
    `);

    // Create messages table
    await this.db.executeSql(`
      CREATE TABLE IF NOT EXISTS messages (
        id TEXT PRIMARY KEY,
        content TEXT NOT NULL,
        senderId TEXT NOT NULL,
        receiverId TEXT NOT NULL,
        timestamp INTEGER NOT NULL,
        isEncrypted INTEGER NOT NULL,
        hopCount INTEGER NOT NULL,
        maxHops INTEGER NOT NULL,
        status TEXT NOT NULL,
        route TEXT NOT NULL,
        transportType TEXT NOT NULL
      )
    `);

    // Create routes table
    await this.db.executeSql(`
      CREATE TABLE IF NOT EXISTS routes (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        destination TEXT NOT NULL,
        path TEXT NOT NULL,
        totalCost INTEGER NOT NULL,
        estimatedDeliveryTime INTEGER NOT NULL,
        reliability REAL NOT NULL,
        created_at INTEGER NOT NULL
      )
    `);

    console.log('Database tables created successfully');
  }

  // Device operations
  async saveDevice(device: Device): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    const transportTypes = JSON.stringify(device.transportTypes);
    const capabilities = JSON.stringify(device.capabilities);

    await this.db.executeSql(`
      INSERT OR REPLACE INTO devices (
        id, name, macAddress, ipAddress, bluetoothAddress, lastSeen,
        signalStrength, transportTypes, isOnline, publicKey, capabilities
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `, [
      device.id,
      device.name,
      device.macAddress,
      device.ipAddress || null,
      device.bluetoothAddress || null,
      device.lastSeen,
      device.signalStrength,
      transportTypes,
      device.isOnline ? 1 : 0,
      device.publicKey || null,
      capabilities
    ]);

    console.log(`Device saved: ${device.name}`);
  }

  async getDevices(): Promise<Device[]> {
    if (!this.db) throw new Error('Database not initialized');

    const [results] = await this.db.executeSql(`
      SELECT * FROM devices ORDER BY lastSeen DESC
    `);

    const devices: Device[] = [];
    for (let i = 0; i < results.rows.length; i++) {
      const row = results.rows.item(i);
      devices.push({
        id: row.id,
        name: row.name,
        macAddress: row.macAddress,
        ipAddress: row.ipAddress,
        bluetoothAddress: row.bluetoothAddress,
        lastSeen: row.lastSeen,
        signalStrength: row.signalStrength,
        transportTypes: JSON.parse(row.transportTypes),
        isOnline: row.isOnline === 1,
        publicKey: row.publicKey,
        capabilities: JSON.parse(row.capabilities)
      });
    }

    return devices;
  }

  async deleteDevice(deviceId: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    await this.db.executeSql(`
      DELETE FROM devices WHERE id = ?
    `, [deviceId]);

    console.log(`Device deleted: ${deviceId}`);
  }

  // Message operations
  async saveMessage(message: Message): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    const route = JSON.stringify(message.route);

    await this.db.executeSql(`
      INSERT OR REPLACE INTO messages (
        id, content, senderId, receiverId, timestamp, isEncrypted,
        hopCount, maxHops, status, route, transportType
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `, [
      message.id,
      message.content,
      message.senderId,
      message.receiverId,
      message.timestamp,
      message.isEncrypted ? 1 : 0,
      message.hopCount,
      message.maxHops,
      message.status,
      route,
      message.transportType
    ]);

    console.log(`Message saved: ${message.id}`);
  }

  async getMessages(deviceId?: string): Promise<Message[]> {
    if (!this.db) throw new Error('Database not initialized');

    let query = 'SELECT * FROM messages';
    let params: any[] = [];

    if (deviceId) {
      query += ' WHERE senderId = ? OR receiverId = ?';
      params = [deviceId, deviceId];
    }

    query += ' ORDER BY timestamp DESC';

    const [results] = await this.db.executeSql(query, params);

    const messages: Message[] = [];
    for (let i = 0; i < results.rows.length; i++) {
      const row = results.rows.item(i);
      messages.push({
        id: row.id,
        content: row.content,
        senderId: row.senderId,
        receiverId: row.receiverId,
        timestamp: row.timestamp,
        isEncrypted: row.isEncrypted === 1,
        hopCount: row.hopCount,
        maxHops: row.maxHops,
        status: row.status,
        route: JSON.parse(row.route),
        transportType: row.transportType
      });
    }

    return messages;
  }

  async updateMessageStatus(messageId: string, status: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    await this.db.executeSql(`
      UPDATE messages SET status = ? WHERE id = ?
    `, [status, messageId]);

    console.log(`Message status updated: ${messageId} -> ${status}`);
  }

  async deleteMessage(messageId: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    await this.db.executeSql(`
      DELETE FROM messages WHERE id = ?
    `, [messageId]);

    console.log(`Message deleted: ${messageId}`);
  }

  // Cleanup operations
  async cleanupOldData(daysOld: number = 30): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    const cutoffTime = Date.now() - (daysOld * 24 * 60 * 60 * 1000);

    // Clean up old messages
    await this.db.executeSql(`
      DELETE FROM messages WHERE timestamp < ?
    `, [cutoffTime]);

    // Clean up offline devices
    await this.db.executeSql(`
      DELETE FROM devices WHERE isOnline = 0 AND lastSeen < ?
    `, [cutoffTime]);

    console.log(`Cleaned up data older than ${daysOld} days`);
  }

  async close(): Promise<void> {
    if (this.db) {
      await this.db.close();
      this.db = null;
      console.log('Database closed');
    }
  }
}

