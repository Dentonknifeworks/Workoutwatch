import { useEffect, useCallback, useRef } from 'react';
import { Platform } from 'react-native';

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

// Dynamic import for wear connectivity (only works on Android with native module)
let watchEvents: any = null;

const loadWearConnectivity = async () => {
  if (Platform.OS === 'android') {
    try {
      const module = await import('react-native-wear-connectivity');
      watchEvents = module.watchEvents;
      return true;
    } catch (error) {
      console.log('Wear connectivity not available:', error);
      return false;
    }
  }
  return false;
};

export function useWatchControl({
  onStart,
  onPause,
  onStop,
  onSkip,
}: UseWatchControlProps) {
  const unsubscribeRef = useRef<(() => void) | null>(null);
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
    const setupListener = async () => {
      if (isInitializedRef.current) return;

      const isAvailable = await loadWearConnectivity();
      
      if (!isAvailable || !watchEvents) {
        console.log('Watch connectivity not available on this platform');
        return;
      }

      try {
        // Subscribe to messages from the watch
        unsubscribeRef.current = watchEvents.on('message', (message: any) => {
          handleMessage(message);
        });
        
        isInitializedRef.current = true;
        console.log('Watch listener initialized');
      } catch (error) {
        console.error('Failed to setup watch listener:', error);
      }
    };

    setupListener();

    // Cleanup on unmount
    return () => {
      if (unsubscribeRef.current) {
        unsubscribeRef.current();
        unsubscribeRef.current = null;
      }
      isInitializedRef.current = false;
    };
  }, [handleMessage]);

  return {
    isSupported: Platform.OS === 'android',
  };
}
