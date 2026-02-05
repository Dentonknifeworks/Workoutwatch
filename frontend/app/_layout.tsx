import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { AppRegistry, Platform, DeviceEventEmitter } from 'react-native';

// Register HeadlessJS task for Wear OS messages (Android only)
if (Platform.OS === 'android') {
  AppRegistry.registerHeadlessTask('WearConnectivityTask', () => async (data) => {
    console.log('WearConnectivityTask received:', data);
    DeviceEventEmitter.emit('message', data);
  });
}

export default function RootLayout() {
  return (
    <>
      <StatusBar style="light" />
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Screen name="index" />
        <Stack.Screen name="(tabs)" />
      </Stack>
    </>
  );
}
