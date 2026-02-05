import { AppRegistry } from 'react-native';
import { registerRootComponent } from 'expo';
import { ExpoRoot } from 'expo-router';

// Register the HeadlessJS task for Wear OS connectivity
AppRegistry.registerHeadlessTask('WearConnectivityTask', () => async (data) => {
  console.log('WearConnectivityTask received:', data);
  
  // Import the event emitter to forward the message
  const { DeviceEventEmitter } = require('react-native');
  DeviceEventEmitter.emit('message', data);
  
  return;
});

// Must be exported or Fast Refresh won't update the context
export function App() {
  const ctx = require.context('./app');
  return <ExpoRoot context={ctx} />;
}

registerRootComponent(App);
