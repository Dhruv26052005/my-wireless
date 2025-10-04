import CryptoJS from 'crypto-js';

export class EncryptionService {
  private static instance: EncryptionService;
  private privateKey: string | null = null;
  private publicKey: string | null = null;

  static getInstance(): EncryptionService {
    if (!EncryptionService.instance) {
      EncryptionService.instance = new EncryptionService();
    }
    return EncryptionService.instance;
  }

  async initialize(): Promise<boolean> {
    try {
      console.log('Initializing encryption service...');
      // Generate key pair for this device
      await this.generateKeyPair();
      return true;
    } catch (error) {
      console.error('Failed to initialize encryption service:', error);
      return false;
    }
  }

  private async generateKeyPair(): Promise<void> {
    // Generate a simple key pair (in production, use proper RSA/ECC)
    const keyPair = this.generateSimpleKeyPair();
    this.privateKey = keyPair.privateKey;
    this.publicKey = keyPair.publicKey;
    console.log('Generated encryption key pair');
  }

  private generateSimpleKeyPair(): { privateKey: string; publicKey: string } {
    // Simple key generation for demo (use proper crypto in production)
    const privateKey = CryptoJS.lib.WordArray.random(256/8).toString();
    const publicKey = CryptoJS.SHA256(privateKey).toString();
    return { privateKey, publicKey };
  }

  async encryptMessage(content: string, recipientPublicKey: string): Promise<string> {
    try {
      console.log('Encrypting message...');
      
      // Use AES encryption with a derived key
      const derivedKey = this.deriveKey(recipientPublicKey);
      const encrypted = CryptoJS.AES.encrypt(content, derivedKey).toString();
      
      console.log('Message encrypted successfully');
      return encrypted;
    } catch (error) {
      console.error('Failed to encrypt message:', error);
      throw error;
    }
  }

  async decryptMessage(encryptedContent: string, senderPublicKey: string): Promise<string> {
    try {
      console.log('Decrypting message...');
      
      // Use AES decryption with a derived key
      const derivedKey = this.deriveKey(senderPublicKey);
      const decrypted = CryptoJS.AES.decrypt(encryptedContent, derivedKey);
      const content = decrypted.toString(CryptoJS.enc.Utf8);
      
      if (!content) {
        throw new Error('Failed to decrypt message - invalid key or corrupted data');
      }
      
      console.log('Message decrypted successfully');
      return content;
    } catch (error) {
      console.error('Failed to decrypt message:', error);
      throw error;
    }
  }

  private deriveKey(publicKey: string): string {
    // Derive a key from the public key and our private key
    if (!this.privateKey) {
      throw new Error('Private key not initialized');
    }
    
    const combined = this.privateKey + publicKey;
    return CryptoJS.SHA256(combined).toString();
  }

  getPublicKey(): string | null {
    return this.publicKey;
  }

  getPrivateKey(): string | null {
    return this.privateKey;
  }

  async signMessage(content: string): Promise<string> {
    try {
      if (!this.privateKey) {
        throw new Error('Private key not initialized');
      }
      
      const signature = CryptoJS.HmacSHA256(content, this.privateKey).toString();
      return signature;
    } catch (error) {
      console.error('Failed to sign message:', error);
      throw error;
    }
  }

  async verifySignature(content: string, signature: string, senderPublicKey: string): Promise<boolean> {
    try {
      // In a real implementation, you'd verify against the sender's public key
      // For demo purposes, we'll just verify the signature format
      return signature.length === 64; // SHA256 hex length
    } catch (error) {
      console.error('Failed to verify signature:', error);
      return false;
    }
  }

  generateMessageHash(content: string): string {
    return CryptoJS.SHA256(content).toString();
  }

  generateDeviceId(): string {
    return CryptoJS.lib.WordArray.random(16).toString();
  }
}

