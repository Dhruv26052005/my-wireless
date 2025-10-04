# Hybrid Mesh Chat - Setup Guide

## 🚀 Quick Start

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Physical Android device (Bluetooth/Wi-Fi features don't work well in emulator)

### Installation Steps

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd hybrid-mesh-chat
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the project folder and select it

3. **Configure Cloud Server (Optional)**
   - Edit `app/src/main/java/com/hybridmesh/chat/network/messaging/CloudSyncManager.kt`
   - Update the `cloudServerUrl` and `apiKey` variables
   - If you don't have a cloud server, the app will work offline-only

4. **Build and Run**
   - Connect your Android device via USB
   - Enable Developer Options and USB Debugging
   - Click "Run" in Android Studio

## 🔧 Configuration

### Cloud Server Setup (Optional)

If you want cloud sync functionality, you'll need to set up a simple REST API server:

#### Required Endpoints:
```
POST /api/messages/sync
- Body: Array of messages
- Response: Success/failure status

GET /api/messages/download?since={timestamp}
- Response: Array of new messages

POST /api/devices/sync
- Body: Array of devices
- Response: Success/failure status
```

#### Example Node.js Server:
```javascript
const express = require('express');
const app = express();

app.use(express.json());

// Store messages in memory (use database in production)
let messages = [];
let devices = [];

app.post('/api/messages/sync', (req, res) => {
    messages.push(...req.body);
    res.json({ success: true });
});

app.get('/api/messages/download', (req, res) => {
    const since = parseInt(req.query.since) || 0;
    const newMessages = messages.filter(m => m.timestamp > since);
    res.json(newMessages);
});

app.post('/api/devices/sync', (req, res) => {
    devices = req.body;
    res.json({ success: true });
});

app.listen(3000, () => {
    console.log('Cloud server running on port 3000');
});
```

### App Configuration

#### Message Expiration Time
Edit `app/src/main/java/com/hybridmesh/chat/service/StoreAndForwardManager.kt`:
```kotlin
private val messageExpiryMs = 300000L // 5 minutes (adjust as needed)
```

#### Max Hop Count
Edit `app/src/main/java/com/hybridmesh/chat/data/model/Message.kt`:
```kotlin
val maxHops: Int = 5 // Maximum number of relay hops
```

#### Discovery Interval
Edit `app/src/main/java/com/hybridmesh/chat/network/DiscoveryManager.kt`:
```kotlin
delay(30000) // 30 seconds between discovery cycles
```

## 📱 Testing the App

### Single Device Test
1. Install the app on one device
2. The app will show "No devices found" (expected)
3. Check that discovery is running (refresh button should work)

### Two Device Test
1. Install the app on two devices
2. Place devices within 10 meters of each other
3. Both devices should discover each other
4. Select a device and send a test message
5. Message should be delivered directly via Bluetooth

### Multi-Hop Test
1. Install the app on three or more devices
2. Place devices in a line: A - B - C (where A and C can't reach each other directly)
3. Send a message from A to C
4. Message should be relayed through B
5. Check hop count in message details

### Range Test
1. Test Bluetooth range (up to 50m in open space)
2. Test Wi-Fi Direct range (up to 150m in open space)
3. Test with obstacles (walls, buildings)
4. Verify automatic transport switching

## 🔍 Troubleshooting

### Common Issues

#### "No devices found"
- **Cause**: Discovery not working or devices too far apart
- **Solution**: 
  - Check Bluetooth and Wi-Fi are enabled
  - Ensure location permission is granted
  - Move devices closer together
  - Restart discovery (refresh button)

#### "Message failed to send"
- **Cause**: No route to destination or encryption issues
- **Solution**:
  - Check if destination device is online
  - Verify public key exchange
  - Check network topology
  - Try sending to a different device

#### "Permission denied"
- **Cause**: Missing Android permissions
- **Solution**:
  - Go to Settings > Apps > Hybrid Mesh Chat > Permissions
  - Enable all required permissions
  - Restart the app

#### High battery usage
- **Cause**: Continuous discovery scanning
- **Solution**:
  - The app is optimized for battery life
  - Discovery automatically pauses when not needed
  - Consider reducing discovery frequency in settings

### Debug Mode

Enable debug logging by adding this to your `build.gradle`:
```gradle
android {
    buildTypes {
        debug {
            buildConfigField "boolean", "DEBUG_MODE", "true"
        }
    }
}
```

### Network Topology Debug

The app includes a network topology viewer:
1. Go to device list screen
2. Long-press on a device
3. Select "View Network Topology"
4. See the mesh network graph

## 🛠️ Development

### Adding New Features

#### New Transport Type
1. Add enum value to `TransportType`
2. Implement discovery in appropriate discovery class
3. Add routing logic in `RoutingEngine`
4. Update UI to show new transport type

#### New Message Type
1. Add field to `Message` model
2. Update database schema
3. Modify encryption/decryption if needed
4. Update UI components

#### New Encryption Algorithm
1. Implement new encryption class
2. Add to `EncryptionManager`
3. Update key exchange protocol
4. Add migration for existing messages

### Code Structure

```
app/src/main/java/com/hybridmesh/chat/
├── data/
│   ├── database/          # Room database and DAOs
│   └── model/            # Data models
├── di/                   # Dependency injection
├── network/
│   ├── bluetooth/        # Bluetooth discovery
│   ├── encryption/       # Encryption/decryption
│   ├── messaging/        # Core messaging service
│   ├── routing/          # Routing algorithms
│   └── wifi/            # Wi-Fi Direct discovery
├── service/              # Background services
└── ui/                   # UI components and screens
```

### Testing

#### Unit Tests
```bash
./gradlew test
```

#### Integration Tests
```bash
./gradlew connectedAndroidTest
```

#### Manual Testing Checklist
- [ ] Device discovery works
- [ ] Message sending/receiving works
- [ ] Multi-hop routing works
- [ ] Encryption/decryption works
- [ ] Store-and-forward works
- [ ] Cloud sync works (if configured)
- [ ] UI is responsive
- [ ] Battery usage is reasonable

## 📊 Performance Optimization

### Battery Life
- Discovery scanning is optimized for minimal battery drain
- Messages are batched to reduce radio usage
- Background processing is minimized

### Memory Usage
- Device cache is limited to prevent memory leaks
- Old messages are automatically cleaned up
- Images and large files are not supported (by design)

### Network Efficiency
- Messages are compressed before transmission
- Duplicate messages are filtered out
- Route caching reduces computation overhead

## 🔒 Security Considerations

### Key Management
- Private keys are stored in Android Keystore
- Public keys are exchanged securely
- Keys are rotated periodically

### Message Security
- All messages are encrypted end-to-end
- Relays cannot read message content
- Perfect forward secrecy is maintained

### Network Security
- Device authentication prevents spoofing
- Message integrity is verified
- Replay attacks are prevented

## 📈 Monitoring and Analytics

### Built-in Metrics
- Message delivery success rate
- Average hop count
- Discovery success rate
- Battery usage statistics

### Logging
- All network operations are logged
- Encryption operations are logged
- Error conditions are logged
- Performance metrics are collected

## 🚀 Deployment

### Production Build
```bash
./gradlew assembleRelease
```

### App Signing
1. Generate signing key
2. Configure in `build.gradle`
3. Build release APK
4. Test thoroughly before distribution

### Distribution
- Upload to Google Play Store
- Or distribute APK directly
- Include setup instructions for users

## 📞 Support

### Getting Help
- Check this documentation first
- Search existing GitHub issues
- Create new issue with detailed description
- Include device information and logs

### Contributing
- Fork the repository
- Create feature branch
- Make changes with tests
- Submit pull request

---

**Happy meshing! 🌐**
