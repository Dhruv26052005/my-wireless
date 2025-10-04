import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
} from 'react-native';
import { Device, DeviceCapability, TransportType } from '../models/Device';

interface DeviceCardProps {
  device: Device;
  onPress: () => void;
}

const DeviceCard: React.FC<DeviceCardProps> = ({ device, onPress }) => {
  const getSignalStrengthColor = (strength: number): string => {
    if (strength > -50) return '#4CAF50'; // Green
    if (strength > -70) return '#FF9800'; // Orange
    return '#F44336'; // Red
  };

  const getTransportIcon = (transport: TransportType): string => {
    switch (transport) {
      case TransportType.BLUETOOTH:
        return '📶';
      case TransportType.WIFI_DIRECT:
        return '📡';
      case TransportType.HOTSPOT:
        return '🔥';
      case TransportType.INTERNET:
        return '🌐';
      default:
        return '📱';
    }
  };

  const getCapabilityIcon = (capability: DeviceCapability): string => {
    switch (capability) {
      case DeviceCapability.BLUETOOTH_LE:
        return '🔵';
      case DeviceCapability.WIFI_DIRECT:
        return '📡';
      case DeviceCapability.HOTSPOT:
        return '🔥';
      case DeviceCapability.INTERNET_RELAY:
        return '🌐';
      case DeviceCapability.MESSAGE_STORE:
        return '💾';
      default:
        return '📱';
    }
  };

  return (
    <TouchableOpacity style={styles.container} onPress={onPress}>
      <View style={styles.header}>
        <View style={styles.nameContainer}>
          <Text style={styles.deviceName}>{device.name}</Text>
          <View style={styles.statusContainer}>
            <View style={[
              styles.statusDot,
              { backgroundColor: device.isOnline ? '#4CAF50' : '#F44336' }
            ]} />
            <Text style={styles.statusText}>
              {device.isOnline ? 'Online' : 'Offline'}
            </Text>
          </View>
        </View>
        <Text style={[
          styles.signalStrength,
          { color: getSignalStrengthColor(device.signalStrength) }
        ]}>
          {device.signalStrength} dBm
        </Text>
      </View>

      <View style={styles.details}>
        <Text style={styles.macAddress}>{device.macAddress}</Text>
        {device.ipAddress && (
          <Text style={styles.ipAddress}>IP: {device.ipAddress}</Text>
        )}
      </View>

      <View style={styles.transports}>
        <Text style={styles.transportsLabel}>Transports:</Text>
        <View style={styles.transportIcons}>
          {device.transportTypes.map((transport, index) => (
            <Text key={index} style={styles.transportIcon}>
              {getTransportIcon(transport)}
            </Text>
          ))}
        </View>
      </View>

      <View style={styles.capabilities}>
        <Text style={styles.capabilitiesLabel}>Capabilities:</Text>
        <View style={styles.capabilityIcons}>
          {device.capabilities.map((capability, index) => (
            <Text key={index} style={styles.capabilityIcon}>
              {getCapabilityIcon(capability)}
            </Text>
          ))}
        </View>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  nameContainer: {
    flex: 1,
  },
  deviceName: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 4,
  },
  statusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statusDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 6,
  },
  statusText: {
    fontSize: 14,
    color: '#666',
  },
  signalStrength: {
    fontSize: 14,
    fontWeight: 'bold',
  },
  details: {
    marginBottom: 8,
  },
  macAddress: {
    fontSize: 14,
    color: '#666',
    fontFamily: 'monospace',
  },
  ipAddress: {
    fontSize: 14,
    color: '#666',
    marginTop: 2,
  },
  transports: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  transportsLabel: {
    fontSize: 14,
    color: '#666',
    marginRight: 8,
  },
  transportIcons: {
    flexDirection: 'row',
  },
  transportIcon: {
    fontSize: 16,
    marginRight: 4,
  },
  capabilities: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  capabilitiesLabel: {
    fontSize: 14,
    color: '#666',
    marginRight: 8,
  },
  capabilityIcons: {
    flexDirection: 'row',
  },
  capabilityIcon: {
    fontSize: 16,
    marginRight: 4,
  },
});

export default DeviceCard;
