import React, { useEffect, useState } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { StatusBar, View, Text, ActivityIndicator, StyleSheet } from 'react-native';
import DeviceListScreen from './src/screens/DeviceListScreen';
import ChatScreen from './src/screens/ChatScreen';
import { NativeMeshService } from './src/services/NativeMeshService';

const Stack = createStackNavigator();

const App: React.FC = () => {
  const [isInitialized, setIsInitialized] = useState(false);
  const [initializationError, setInitializationError] = useState<string | null>(null);

  useEffect(() => {
    initializeApp();
  }, []);

  const initializeApp = async () => {
    try {
      console.log('Initializing HybridMesh app...');
      const meshService = NativeMeshService.getInstance();
      const success = await meshService.initialize();
      
      if (success) {
        console.log('App initialized successfully');
        setIsInitialized(true);
      } else {
        setInitializationError('Failed to initialize mesh service');
      }
    } catch (error) {
      console.error('App initialization failed:', error);
      setInitializationError('App initialization failed');
    }
  };

  if (!isInitialized) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#2196F3" />
        <Text style={styles.loadingText}>
          {initializationError || 'Initializing HybridMesh...'}
        </Text>
      </View>
    );
  }

  return (
    <NavigationContainer>
      <StatusBar barStyle="dark-content" backgroundColor="#ffffff" />
      <Stack.Navigator
        initialRouteName="DeviceList"
        screenOptions={{
          headerStyle: {
            backgroundColor: '#2196F3',
          },
          headerTintColor: '#fff',
          headerTitleStyle: {
            fontWeight: 'bold',
          },
        }}
      >
        <Stack.Screen 
          name="DeviceList" 
          component={DeviceListScreen} 
          options={{ title: 'Mesh Devices' }}
        />
        <Stack.Screen 
          name="Chat" 
          component={ChatScreen} 
          options={{ title: 'Chat' }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
  },
});

export default App;
