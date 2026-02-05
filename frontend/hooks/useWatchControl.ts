import { useEffect, useCallback, useRef } from 'react';
import { Platform, DeviceEventEmitter } from 'react-native';

// Type definitions for watch messages
interface WatchMessage {
  command: string;
  timestamp: number;
  payload?: Record<string, unknown>;
}

interface UseWatchControlProps {
  onStart: () => void;
  onPause: () => void;
  onStop: () => void;
  onSkip: () => void;
}

export function useWatchControl({
  onStart,
  onPause,
  onStop,
  onSkip,
}: UseWatchControlProps) {
  const isInitializedRef = useRef(false);

  const handleMessage = useCallback(
    (message: WatchMessage | string) => {
      console.log('Received watch message:', message);

      let parsedMessage: WatchMessage;

      // Parse message if it's a string
      if (typeof message === 'string') {
        try {
          parsedMessage = JSON.parse(message);
        } catch (e) {
          console.error('Failed to parse watch message:', e);
          return;
        }
      } else {
        parsedMessage = message as WatchMessage;
      }

      // Handle commands
      const { command } = parsedMessage;

      switch (command) {
        case 'start':
          console.log('Watch command: START');
          onStart();
          break;
        case 'pause':
          console.log('Watch command: PAUSE');
          onPause();
          break;
        case 'stop':
          console.log('Watch command: STOP');
          onStop();
          break;
        case 'skip':
          console.log('Watch command: SKIP');
          onSkip();
          break;
        default:
          console.log('Unknown watch command:', command);
      }
    },
    [onStart, onPause, onStop, onSkip]
  );

  useEffect(() => {
    if (Platform.OS !== 'android') {
      console.log('Watch connectivity only available on Android');
      return;
    }
    
    if (isInitializedRef.current) return;

    // Listen for messages from the HeadlessJS task
    const subscription = DeviceEventEmitter.addListener('message', (data) => {
      console.log('DeviceEventEmitter received message:', data);
      handleMessage(data);
    });
    
    isInitializedRef.current = true;
    console.log('Watch listener initialized successfully');

    // Cleanup on unmount
    return () => {
      subscription.remove();
      isInitializedRef.current = false;
    };
  }, [handleMessage]);

  return {
    isSupported: Platform.OS === 'android',
  };
}
