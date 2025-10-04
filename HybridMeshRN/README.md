# HybridMesh React Native App

A React Native implementation of the HybridMesh chat application, ported from the original Android Kotlin version.

## Features

- **Mesh Networking**: Bluetooth LE and Wi-Fi Direct discovery and communication
- **Encrypted Messaging**: End-to-end encrypted messages with hop routing
- **Device Management**: Real-time device discovery and status monitoring
- **Cross-Platform**: Runs on both iOS and Android

## Architecture

### Models
- `Device.ts` - Device information and capabilities
- `Message.ts` - Message structure with encryption and routing
- `Route.ts` - Network routing and topology management

### Screens
- `DeviceListScreen.tsx` - Shows discovered mesh devices
- `ChatScreen.tsx` - Chat interface with a selected device

### Components
- `DeviceCard.tsx` - Device information display card
- `MessageBubble.tsx` - Chat message bubble with status indicators

### Services
- `MeshService.ts` - Core mesh networking logic
- `BluetoothService.ts` - Bluetooth LE discovery and communication

## Setup

### Prerequisites
- Node.js 16+
- React Native CLI
- Android Studio (for Android development)
- Xcode (for iOS development)

### Installation

1. Install dependencies:
```bash
cd HybridMeshRN
npm install
```

2. For Android:
```bash
npx react-native run-android
```

3. For iOS:
```bash
cd ios && pod install && cd ..
npx react-native run-ios
```

## Dependencies

### Core React Native
- `@react-navigation/native` - Navigation
- `@react-navigation/stack` - Stack navigation
- `react-native-screens` - Native screen optimization
- `react-native-safe-area-context` - Safe area handling
- `react-native-gesture-handler` - Gesture handling

### Mesh Networking
- `react-native-ble-plx` - Bluetooth LE functionality
- `react-native-wifi-reborn` - Wi-Fi Direct support
- `react-native-permissions` - Permission management

### Data Storage
- `react-native-sqlite-storage` - Local SQLite database

### Security
- `react-native-crypto-js` - Encryption utilities

## Migration from Android

This React Native app is a port of the original Android Kotlin application. Key differences:

### Data Models
- Kotlin data classes → TypeScript interfaces
- Room database annotations → Manual SQLite setup
- Kotlin enums → TypeScript enums

### UI Components
- Jetpack Compose → React Native components
- Material Design → Custom styling
- ViewModels → React hooks and state management

### Services
- Android Services → React Native services with native modules
- BluetoothAdapter → react-native-ble-plx
- WifiP2pManager → react-native-wifi-reborn

## Development Status

- ✅ Basic app structure and navigation
- ✅ TypeScript models ported from Kotlin
- ✅ UI components and screens
- ✅ Service architecture setup
- 🔄 Bluetooth integration (in progress)
- 🔄 Wi-Fi Direct integration (pending)
- 🔄 Encryption implementation (pending)
- 🔄 SQLite database setup (pending)
- 🔄 Native module development (pending)

## Next Steps

1. Implement native modules for Bluetooth and Wi-Fi Direct
2. Set up SQLite database with proper schema
3. Implement encryption using crypto-js
4. Add proper error handling and logging
5. Implement store-and-forward messaging
6. Add network topology visualization
7. Implement message routing algorithms

## Contributing

This is a work in progress. The current implementation provides a solid foundation for the mesh networking features, but requires additional native development for full functionality.
