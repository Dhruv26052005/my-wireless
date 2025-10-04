import { TransportType } from './Device';

export interface Message {
  id: string;
  content: string;
  senderId: string;
  receiverId: string;
  timestamp: number;
  isEncrypted: boolean;
  hopCount: number;
  maxHops: number;
  status: MessageStatus;
  route: string[]; // Path of device IDs that relayed this message
  transportType: TransportType;
}

export enum MessageStatus {
  PENDING = 'PENDING',
  SENT = 'SENT',
  DELIVERED = 'DELIVERED',
  FAILED = 'FAILED',
  EXPIRED = 'EXPIRED'
}

export const createMessage = (data: Partial<Message>): Message => ({
  id: data.id || generateUUID(),
  content: data.content || '',
  senderId: data.senderId || '',
  receiverId: data.receiverId || '',
  timestamp: data.timestamp || Date.now(),
  isEncrypted: data.isEncrypted ?? true,
  hopCount: data.hopCount || 0,
  maxHops: data.maxHops || 5,
  status: data.status || MessageStatus.PENDING,
  route: data.route || [],
  transportType: data.transportType || TransportType.BLUETOOTH
});

const generateUUID = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
};
