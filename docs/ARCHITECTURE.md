# Hybrid Mesh Network Architecture

## System Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           HYBRID MESH CHAT APP                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Device A      │    │   Device B      │    │   Device C      │    │   Device D      │
│  (You)          │    │  (Relay)        │    │  (Relay)        │    │  (Friend)       │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │                       │
         │                       │                       │                       │
    ┌────▼────┐              ┌───▼───┐              ┌───▼───┐              ┌────▼────┐
    │Discovery│              │Discovery│              │Discovery│              │Discovery│
    │ Layer   │              │ Layer   │              │ Layer   │              │ Layer   │
    └────┬────┘              └───┬───┘              └───┬───┘              └────┬────┘
         │                       │                       │                       │
    ┌────▼────┐              ┌───▼───┐              ┌───▼───┐              ┌────▼────┐
    │Routing  │              │Routing │              │Routing │              │Routing  │
    │ Engine  │              │ Engine │              │ Engine │              │ Engine  │
    └────┬────┘              └───┬───┘              └───┬───┘              └────┬────┘
         │                       │                       │                       │
    ┌────▼────┐              ┌───▼───┐              ┌───▼───┐              ┌────▼────┐
    │Encryption│              │Encryption│              │Encryption│              │Encryption│
    │ Manager │              │ Manager │              │ Manager │              │ Manager │
    └────┬────┘              └───┬───┘              └───┬───┘              └────┬────┘
         │                       │                       │                       │
    ┌────▼────┐              ┌───▼───┐              ┌───▼───┐              ┌────▼────┐
    │Store &  │              │Store & │              │Store & │              │Store &  │
    │ Forward │              │ Forward│              │ Forward│              │ Forward │
    └─────────┘              └────────┘              └────────┘              └─────────┘
```

## Message Flow Example

```
1. USER SENDS MESSAGE
   ┌─────────────────┐
   │ "Hello Friend!" │
   └─────────┬───────┘
             │
             ▼
   ┌─────────────────┐
   │ Encrypt with    │
   │ Friend's Key    │
   └─────────┬───────┘
             │
             ▼
   ┌─────────────────┐
   │ Find Best Route │
   │ A → B → C → D   │
   └─────────┬───────┘
             │
             ▼
   ┌─────────────────┐
   │ Send via        │
   │ Bluetooth to B  │
   └─────────┬───────┘
             │
             ▼
   ┌─────────────────┐
   │ B forwards via  │
   │ Wi-Fi to C      │
   └─────────┬───────┘
             │
             ▼
   ┌─────────────────┐
   │ C forwards via  │
   │ Wi-Fi to D      │
   └─────────┬───────┘
             │
             ▼
   ┌─────────────────┐
   │ D decrypts and  │
   │ shows message   │
   └─────────────────┘
```

## Transport Selection Logic

```
Distance Check:
├── < 10m → Bluetooth (Low Power, High Reliability)
├── 10-50m → Bluetooth or Wi-Fi Direct (Signal Strength Based)
└── > 50m → Wi-Fi Direct (Higher Range, Faster Transfer)

Signal Strength:
├── Excellent (> -50 dBm) → Any Transport
├── Good (-50 to -60 dBm) → Bluetooth Preferred
├── Fair (-60 to -70 dBm) → Wi-Fi Direct Preferred
└── Poor (< -70 dBm) → Store and Forward
```

## Discovery Process

```
1. START DISCOVERY
   ├── Enable Bluetooth LE Scanner
   ├── Enable Wi-Fi Direct Discovery
   └── Start Periodic Scanning

2. DEVICE FOUND
   ├── Check if Mesh Device (Name contains "HybridMesh")
   ├── Extract Device Information
   ├── Measure Signal Strength
   └── Add to Device Cache

3. KEY EXCHANGE
   ├── Send Public Key to New Device
   ├── Receive Public Key from Device
   ├── Store Public Key Securely
   └── Mark Device as Trusted

4. TOPOLOGY UPDATE
   ├── Recalculate Routing Table
   ├── Update Network Graph
   └── Notify UI of Changes
```

## Encryption Flow

```
1. MESSAGE CREATION
   ┌─────────────────┐
   │ Plain Text      │
   │ "Hello World"   │
   └─────────┬───────┘
             │
             ▼
   ┌─────────────────┐
   │ Generate Random │
   │ AES Key         │
   └─────────┬───────┘
             │
             ▼
   ┌─────────────────┐
   │ Encrypt with    │
   │ AES-GCM         │
   └─────────┬───────┘
             │
             ▼
   ┌─────────────────┐
   │ Encrypt AES Key │
   │ with ECDH       │
   └─────────┬───────┘
             │
             ▼
   ┌─────────────────┐
   │ Package:        │
   │ - Encrypted Data│
   │ - Encrypted Key │
   │ - IV & Tag      │
   └─────────────────┘
```

## Store and Forward Logic

```
1. MESSAGE QUEUE
   ├── Add to Pending Queue
   ├── Store in Database
   └── Start Delivery Attempt

2. DELIVERY ATTEMPT
   ├── Check if Destination Online
   ├── Find Best Route
   ├── Send Message
   └── Wait for Acknowledgment

3. SUCCESS
   ├── Mark as Sent
   ├── Remove from Queue
   └── Update Statistics

4. FAILURE
   ├── Increment Retry Count
   ├── Add to Retry Queue
   └── Schedule Retry

5. MAX RETRIES
   ├── Mark as Failed
   ├── Log Error
   └── Notify User
```

## Cloud Sync Process

```
1. INTERNET AVAILABLE
   ├── Check Network Connection
   ├── Start Sync Process
   └── Upload Pending Messages

2. SYNC MESSAGES
   ├── Get Unsent Messages
   ├── Upload to Cloud Server
   ├── Mark as Synced
   └── Download New Messages

3. CONFLICT RESOLUTION
   ├── Compare Timestamps
   ├── Merge Message History
   └── Update Local Database

4. OFFLINE MODE
   ├── Continue Local Operations
   ├── Queue for Later Sync
   └── Maintain Functionality
```
