import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  Alert,
  RefreshControl,
} from 'react-native';
import { Device, createDevice, DeviceCapability, TransportType } from '../models/Device';
import DeviceCard from '../components/DeviceCard';

const DeviceListScreen: React.FC<{ navigation: any }> = ({ navigation }) => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [refreshing, setRefreshing] = useState(false);

  // Demo data generator
  const generateDemoDevices = (): Device[] => {
    return [
      createDevice({
        name: 'iPhone 15 Pro',
        macAddress: 'AA:BB:CC:DD:EE:01',
        ipAddress: '192.168.1.100',
        bluetoothAddress: '00:11:22:33:44:55',
        signalStrength: -45,
        transportTypes: [TransportType.BLUETOOTH, TransportType.WIFI_DIRECT],
        capabilities: [DeviceCapability.BLUETOOTH_LE, DeviceCapability.WIFI_DIRECT],
        isOnline: true,
      }),
      createDevice({
        name: 'Samsung Galaxy S24',
        macAddress: 'AA:BB:CC:DD:EE:02',
        ipAddress: '192.168.1.101',
        signalStrength: -60,
        transportTypes: [TransportType.BLUETOOTH, TransportType.WIFI_DIRECT],
        capabilities: [DeviceCapability.BLUETOOTH_LE, DeviceCapability.WIFI_DIRECT, DeviceCapability.HOTSPOT],
        isOnline: true,
      }),
      createDevice({
        name: 'Mesh Router Node',
        macAddress: 'AA:BB:CC:DD:EE:03',
        ipAddress: '192.168.1.102',
        signalStrength: -30,
        transportTypes: [TransportType.WIFI_DIRECT, TransportType.HOTSPOT],
        capabilities: [DeviceCapability.WIFI_DIRECT, DeviceCapability.HOTSPOT, DeviceCapability.INTERNET_RELAY, DeviceCapability.MESSAGE_STORE],
        isOnline: true,
      }),
      createDevice({
        name: 'Offline Device',
        macAddress: 'AA:BB:CC:DD:EE:04',
        signalStrength: -80,
        transportTypes: [TransportType.BLUETOOTH],
        capabilities: [DeviceCapability.BLUETOOTH_LE],
        isOnline: false,
      }),
    ];
  };

  useEffect(() => {
    loadDevices();
  }, []);

  const loadDevices = () => {
    setDevices(generateDemoDevices());
  };

  const onRefresh = () => {
    setRefreshing(true);
    // Simulate network delay
    setTimeout(() => {
      loadDevices();
      setRefreshing(false);
    }, 1000);
  };

  const handleDevicePress = (device: Device) => {
    if (!device.isOnline) {
      Alert.alert('Device Offline', 'This device is currently offline and cannot be reached.');
      return;
    }
    navigation.navigate('Chat', { device });
  };

  const renderDevice = ({ item }: { item: Device }) => (
    <DeviceCard
      device={item}
      onPress={() => handleDevicePress(item)}
    />
  );

  return (
    <View style={styles.container}>
      <FlatList
        data={devices}
        renderItem={renderDevice}
        keyExtractor={(item) => item.id}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
        contentContainerStyle={styles.listContainer}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  listContainer: {
    padding: 16,
  },
});

export default DeviceListScreen;
