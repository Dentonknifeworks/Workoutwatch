import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  TextInput,
  Modal,
  Platform,
  Linking,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as Speech from 'expo-speech';
import * as Haptics from 'expo-haptics';
import { activateKeepAwakeAsync, deactivateKeepAwake } from 'expo-keep-awake';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Notifications from 'expo-notifications';

// Configure notifications to show when app is in foreground
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: false,
    shouldSetBadge: false,
  }),
});

type TimerState = 'idle' | 'work' | 'rest' | 'paused';

interface WorkoutSettings {
  workTime: number;
  restTime: number;
  rounds: number;
}

// Notification channel for Android
const CHANNEL_ID = 'workout-timer';

export default function TimerScreen() {
  const [timerState, setTimerState] = useState<TimerState>('idle');
  const [previousState, setPreviousState] = useState<'work' | 'rest'>('work');
  const [currentRound, setCurrentRound] = useState(1);
  const [timeLeft, setTimeLeft] = useState(30);
  const [settings, setSettings] = useState<WorkoutSettings>({
    workTime: 30,
    restTime: 10,
    rounds: 5,
  });
  const [showSettings, setShowSettings] = useState(false);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const lastSpokenSecond = useRef<number>(-1);
  const notificationListener = useRef<any>();
  const responseListener = useRef<any>();

  useEffect(() => {
    loadSettings();
    setupNotifications();
    
    // Listen for notification responses (button taps)
    responseListener.current = Notifications.addNotificationResponseReceivedListener(response => {
      const action = response.actionIdentifier;
      if (action === 'PAUSE') {
        handlePauseFromNotification();
      } else if (action === 'STOP') {
        handleStopFromNotification();
      }
    });

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
      try {
        deactivateKeepAwake();
      } catch (error) {
        console.log('Keep awake cleanup not needed');
      }
      Notifications.dismissAllNotificationsAsync();
      if (responseListener.current) {
        Notifications.removeNotificationSubscription(responseListener.current);
      }
    };
  }, []);

  const setupNotifications = async () => {
    // Request permissions
    const { status } = await Notifications.requestPermissionsAsync();
    if (status !== 'granted') {
      console.log('Notification permission not granted');
      return;
    }

    // Create notification channel for Android
    if (Platform.OS === 'android') {
      await Notifications.setNotificationChannelAsync(CHANNEL_ID, {
        name: 'Workout Timer',
        importance: Notifications.AndroidImportance.HIGH,
        vibrationPattern: [0, 250, 250, 250],
        lightColor: '#00D9FF',
        lockscreenVisibility: Notifications.AndroidNotificationVisibility.PUBLIC,
        bypassDnd: true,
      });

      // Set up notification categories with action buttons
      await Notifications.setNotificationCategoryAsync('workout', [
        {
          identifier: 'PAUSE',
          buttonTitle: '⏸ Pause',
          options: { opensAppToForeground: false },
        },
        {
          identifier: 'STOP',
          buttonTitle: '⏹ Stop',
          options: { opensAppToForeground: false },
        },
      ]);
    }
  };

  // Refs to store current state for notification handlers
  const timerStateRef = useRef(timerState);
  const previousStateRef = useRef(previousState);
  
  useEffect(() => {
    timerStateRef.current = timerState;
    previousStateRef.current = previousState;
  }, [timerState, previousState]);

  const handlePauseFromNotification = () => {
    if (timerStateRef.current === 'paused') {
      setTimerState(previousStateRef.current);
      speak('Resuming');
      updateNotification(previousStateRef.current, timeLeft, currentRound);
    } else if (timerStateRef.current === 'work' || timerStateRef.current === 'rest') {
      setPreviousState(timerStateRef.current as 'work' | 'rest');
      setTimerState('paused');
      speak('Paused');
      updateNotification('paused', timeLeft, currentRound);
    }
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
  };

  const handleStopFromNotification = () => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }
    setTimerState('idle');
    setCurrentRound(1);
    setTimeLeft(settings.workTime);
    speak('Workout stopped');
    Notifications.dismissAllNotificationsAsync();
    try {
      deactivateKeepAwake();
    } catch (error) {
      console.log('Keep awake deactivate not needed');
    }
    Haptics.notificationAsync(Haptics.NotificationFeedbackType.Warning);
    lastSpokenSecond.current = -1;
  };

  const showWorkoutNotification = async (state: TimerState, time: number, round: number) => {
    const statusText = state === 'work' ? '💪 WORK' : state === 'rest' ? '😮‍💨 REST' : '⏸ PAUSED';
    const mins = Math.floor(time / 60);
    const secs = time % 60;
    const timeText = `${mins}:${secs.toString().padStart(2, '0')}`;

    await Notifications.scheduleNotificationAsync({
      content: {
        title: `${statusText} - Round ${round}/${settings.rounds}`,
        body: `⏱ ${timeText} remaining\nTap to open app`,
        data: { state, time, round },
        categoryIdentifier: 'workout',
        sticky: true,
        autoDismiss: false,
        priority: Notifications.AndroidNotificationPriority.MAX,
        vibrate: [0, 100],
        sound: false,
      },
      trigger: null,
    });
  };

  const updateNotification = async (state: TimerState, time: number, round: number) => {
    await Notifications.dismissAllNotificationsAsync();
    await showWorkoutNotification(state, time, round);
  };

  const loadSettings = async () => {
    try {
      const saved = await AsyncStorage.getItem('lastWorkoutSettings');
      if (saved) {
        setSettings(JSON.parse(saved));
      }
    } catch (error) {
      console.error('Error loading settings:', error);
    }
  };

  const saveSettings = async (newSettings: WorkoutSettings) => {
    try {
      await AsyncStorage.setItem('lastWorkoutSettings', JSON.stringify(newSettings));
      setSettings(newSettings);
    } catch (error) {
      console.error('Error saving settings:', error);
    }
  };

  const speak = (text: string) => {
    if (Speech.isSpeakingAsync()) {
      Speech.stop();
    }
    Speech.speak(text, {
      language: 'en-US',
      pitch: 1.0,
      rate: 0.9,
    });
  };

  const startWorkout = async () => {
    // Ask for notification permission first
    const { status: existingStatus } = await Notifications.getPermissionsAsync();
    let finalStatus = existingStatus;
    
    if (existingStatus !== 'granted') {
      const { status } = await Notifications.requestPermissionsAsync();
      finalStatus = status;
    }
    
    if (finalStatus !== 'granted') {
      Alert.alert(
        'Enable Notifications',
        'Notifications are needed to control your workout from your watch. Please enable them in Settings.',
        [
          { text: 'Cancel', style: 'cancel' },
          { 
            text: 'Open Settings', 
            onPress: () => Linking.openSettings()
          }
        ]
      );
      return; // Don't start workout without permissions
    }
    
    try {
      await activateKeepAwakeAsync();
    } catch (error) {
      console.log('Keep awake not supported on this platform:', error);
    }
    setTimerState('work');
    setCurrentRound(1);
    setTimeLeft(settings.workTime);
    speak(`Start the workout. ${settings.rounds} rounds.`);
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);
    lastSpokenSecond.current = -1;
    
    // Show notification with controls
    await showWorkoutNotification('work', settings.workTime, 1);
  };

  const pauseWorkout = () => {
    if (timerState === 'paused') {
      setTimerState(previousState);
      speak('Resuming');
      updateNotification(previousState, timeLeft, currentRound);
    } else {
      setPreviousState(timerState as 'work' | 'rest');
      setTimerState('paused');
      speak('Paused');
      updateNotification('paused', timeLeft, currentRound);
    }
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
  };

  const stopWorkout = async () => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }
    setTimerState('idle');
    setCurrentRound(1);
    setTimeLeft(settings.workTime);
    speak('Workout stopped');
    await Notifications.dismissAllNotificationsAsync();
    try {
      await deactivateKeepAwake();
    } catch (error) {
      console.log('Keep awake deactivate not needed on this platform');
    }
    Haptics.notificationAsync(Haptics.NotificationFeedbackType.Warning);
    lastSpokenSecond.current = -1;
  };

  const skipToRest = () => {
    if (timerState === 'work') {
      setTimerState('rest');
      setTimeLeft(settings.restTime);
      speak('Rest time!');
      Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      lastSpokenSecond.current = -1;
      updateNotification('rest', settings.restTime, currentRound);
    } else if (timerState === 'rest') {
      if (currentRound >= settings.rounds) {
        stopWorkout();
      } else {
        const nextRound = currentRound + 1;
        setCurrentRound(nextRound);
        setTimerState('work');
        setTimeLeft(settings.workTime);
        speak(`Round ${nextRound}. Go!`);
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);
        lastSpokenSecond.current = -1;
        updateNotification('work', settings.workTime, nextRound);
      }
    }
  };

  const saveWorkoutHistory = async () => {
    try {
      const history = await AsyncStorage.getItem('workoutHistory');
      const historyArray = history ? JSON.parse(history) : [];
      historyArray.unshift({
        date: new Date().toISOString(),
        rounds: currentRound - 1,
        totalRounds: settings.rounds,
        workTime: settings.workTime,
        restTime: settings.restTime,
      });
      await AsyncStorage.setItem('workoutHistory', JSON.stringify(historyArray.slice(0, 50)));
    } catch (error) {
      console.error('Error saving history:', error);
    }
  };

  // Update notification every 5 seconds to show current time
  const notificationUpdateRef = useRef(0);

  useEffect(() => {
    if (timerState === 'work' || timerState === 'rest') {
      intervalRef.current = setInterval(() => {
        setTimeLeft((prev) => {
          const newTime = prev - 1;

          // Update notification every 5 seconds
          notificationUpdateRef.current++;
          if (notificationUpdateRef.current >= 5) {
            notificationUpdateRef.current = 0;
            updateNotification(timerState, newTime, currentRound);
          }

          // Voice announcements at specific intervals
          if (newTime === 10 && lastSpokenSecond.current !== 10) {
            speak('10 seconds');
            lastSpokenSecond.current = 10;
            Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
            updateNotification(timerState, newTime, currentRound);
          } else if (newTime === 5 && lastSpokenSecond.current !== 5) {
            speak('5');
            lastSpokenSecond.current = 5;
            Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
          } else if (newTime <= 3 && newTime > 0 && lastSpokenSecond.current !== newTime) {
            speak(newTime.toString());
            lastSpokenSecond.current = newTime;
            Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
          }

          if (newTime <= 0) {
            handlePhaseComplete();
            return 0;
          }
          return newTime;
        });
      }, 1000);

      return () => {
        if (intervalRef.current) {
          clearInterval(intervalRef.current);
        }
      };
    }
  }, [timerState, currentRound]);

  const handlePhaseComplete = () => {
    Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);

    if (timerState === 'work') {
      setTimerState('rest');
      setTimeLeft(settings.restTime);
      speak('Rest!');
      lastSpokenSecond.current = -1;
      updateNotification('rest', settings.restTime, currentRound);
    } else if (timerState === 'rest') {
      if (currentRound >= settings.rounds) {
        setTimerState('idle');
        speak('Workout complete! Great job!');
        Notifications.dismissAllNotificationsAsync();
        try {
          deactivateKeepAwake();
        } catch (error) {
          console.log('Keep awake deactivate not needed on this platform');
        }
        saveWorkoutHistory();
        lastSpokenSecond.current = -1;
      } else {
        const nextRound = currentRound + 1;
        setCurrentRound(nextRound);
        setTimerState('work');
        setTimeLeft(settings.workTime);
        speak(`Round ${nextRound}. Go!`);
        lastSpokenSecond.current = -1;
        updateNotification('work', settings.workTime, nextRound);
      }
    }
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Workout Timer</Text>
        <TouchableOpacity
          style={styles.settingsButton}
          onPress={() => setShowSettings(true)}
        >
          <Ionicons name="settings" size={28} color="#00D9FF" />
        </TouchableOpacity>
      </View>

      <View style={styles.timerContainer}>
        <View style={[
          styles.statusBadge,
          timerState === 'work' && styles.workBadge,
          timerState === 'rest' && styles.restBadge,
          timerState === 'paused' && styles.pausedBadge,
        ]}>
          <Text style={styles.statusText}>
            {timerState === 'idle' && 'Ready'}
            {timerState === 'work' && 'WORK'}
            {timerState === 'rest' && 'REST'}
            {timerState === 'paused' && 'PAUSED'}
          </Text>
        </View>

        <Text style={styles.roundText}>
          Round {currentRound} / {settings.rounds}
        </Text>

        <View
          style={[
            styles.timeCircle,
            timerState === 'work' && styles.workCircle,
            timerState === 'rest' && styles.restCircle,
            timerState === 'paused' && styles.pausedCircle,
          ]}
        >
          <Text style={styles.timeText}>{formatTime(timeLeft)}</Text>
        </View>

        <View style={styles.controlsContainer}>
          {timerState === 'idle' ? (
            <TouchableOpacity style={styles.startButton} onPress={startWorkout}>
              <Ionicons name="play" size={40} color="#fff" />
              <Text style={styles.buttonText}>Start</Text>
            </TouchableOpacity>
          ) : (
            <>
              <View style={styles.activeControls}>
                <TouchableOpacity
                  style={styles.pauseButton}
                  onPress={pauseWorkout}
                >
                  <Ionicons
                    name={timerState === 'paused' ? 'play' : 'pause'}
                    size={32}
                    color="#fff"
                  />
                </TouchableOpacity>
                <TouchableOpacity style={styles.stopButton} onPress={stopWorkout}>
                  <Ionicons name="stop" size={32} color="#fff" />
                </TouchableOpacity>
              </View>
              <TouchableOpacity 
                style={styles.completeButton} 
                onPress={skipToRest}
              >
                <Ionicons 
                  name="play-skip-forward"
                  size={24} 
                  color="#fff" 
                />
                <Text style={styles.completeButtonText}>
                  {timerState === 'work' ? 'Skip to Rest' : 'Next Round'}
                </Text>
              </TouchableOpacity>
            </>
          )}
        </View>
        
        {timerState !== 'idle' && (
          <View style={styles.watchHint}>
            <Ionicons name="watch-outline" size={16} color="#888" />
            <Text style={styles.watchHintText}>
              Controls available on watch notification
            </Text>
          </View>
        )}
      </View>

      <View style={styles.infoCard}>
        <View style={styles.infoRow}>
          <Ionicons name="fitness" size={24} color="#00D9FF" />
          <Text style={styles.infoLabel}>Work Time</Text>
          <Text style={styles.infoValue}>{settings.workTime}s</Text>
        </View>
        <View style={styles.infoRow}>
          <Ionicons name="pause-circle" size={24} color="#FF9500" />
          <Text style={styles.infoLabel}>Rest Time</Text>
          <Text style={styles.infoValue}>{settings.restTime}s</Text>
        </View>
        <View style={styles.infoRow}>
          <Ionicons name="repeat" size={24} color="#4CD964" />
          <Text style={styles.infoLabel}>Rounds</Text>
          <Text style={styles.infoValue}>{settings.rounds}</Text>
        </View>
      </View>

      <Modal
        visible={showSettings}
        animationType="slide"
        transparent={true}
        onRequestClose={() => setShowSettings(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Workout Settings</Text>
              <TouchableOpacity onPress={() => setShowSettings(false)}>
                <Ionicons name="close" size={28} color="#fff" />
              </TouchableOpacity>
            </View>

            <View style={styles.settingItem}>
              <Text style={styles.settingLabel}>Work Time (seconds)</Text>
              <TextInput
                style={styles.input}
                keyboardType="number-pad"
                value={settings.workTime.toString()}
                onChangeText={(text) =>
                  setSettings({ ...settings, workTime: parseInt(text) || 0 })
                }
              />
            </View>

            <View style={styles.settingItem}>
              <Text style={styles.settingLabel}>Rest Time (seconds)</Text>
              <TextInput
                style={styles.input}
                keyboardType="number-pad"
                value={settings.restTime.toString()}
                onChangeText={(text) =>
                  setSettings({ ...settings, restTime: parseInt(text) || 0 })
                }
              />
            </View>

            <View style={styles.settingItem}>
              <Text style={styles.settingLabel}>Number of Rounds</Text>
              <TextInput
                style={styles.input}
                keyboardType="number-pad"
                value={settings.rounds.toString()}
                onChangeText={(text) =>
                  setSettings({ ...settings, rounds: parseInt(text) || 0 })
                }
              />
            </View>

            <TouchableOpacity
              style={styles.saveButton}
              onPress={() => {
                saveSettings(settings);
                setShowSettings(false);
                setTimeLeft(settings.workTime);
              }}
            >
              <Text style={styles.saveButtonText}>Save Settings</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0c0c0c',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    paddingTop: 40,
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#fff',
  },
  settingsButton: {
    padding: 8,
  },
  timerContainer: {
    alignItems: 'center',
    paddingVertical: 40,
  },
  statusBadge: {
    backgroundColor: '#00D9FF',
    paddingHorizontal: 24,
    paddingVertical: 8,
    borderRadius: 20,
    marginBottom: 16,
  },
  workBadge: {
    backgroundColor: '#00D9FF',
  },
  restBadge: {
    backgroundColor: '#FF9500',
  },
  pausedBadge: {
    backgroundColor: '#FFD60A',
  },
  statusText: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#000',
  },
  roundText: {
    fontSize: 20,
    color: '#888',
    marginBottom: 24,
  },
  timeCircle: {
    width: 280,
    height: 280,
    borderRadius: 140,
    backgroundColor: '#1a1a1a',
    borderWidth: 8,
    borderColor: '#333',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 40,
  },
  workCircle: {
    borderColor: '#00D9FF',
  },
  restCircle: {
    borderColor: '#FF9500',
  },
  pausedCircle: {
    borderColor: '#FFD60A',
  },
  timeText: {
    fontSize: 72,
    fontWeight: 'bold',
    color: '#fff',
  },
  controlsContainer: {
    marginTop: 16,
  },
  startButton: {
    backgroundColor: '#00D9FF',
    width: 120,
    height: 120,
    borderRadius: 60,
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 8,
    shadowColor: '#00D9FF',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
  },
  buttonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
    marginTop: 4,
  },
  activeControls: {
    flexDirection: 'row',
    gap: 24,
  },
  pauseButton: {
    backgroundColor: '#FFD60A',
    width: 80,
    height: 80,
    borderRadius: 40,
    justifyContent: 'center',
    alignItems: 'center',
  },
  stopButton: {
    backgroundColor: '#FF3B30',
    width: 80,
    height: 80,
    borderRadius: 40,
    justifyContent: 'center',
    alignItems: 'center',
  },
  completeButton: {
    backgroundColor: '#4CD964',
    borderRadius: 12,
    paddingVertical: 16,
    paddingHorizontal: 24,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    marginTop: 20,
    minWidth: 200,
  },
  completeButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  watchHint: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: 20,
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: '#1a1a1a',
    borderRadius: 20,
  },
  watchHintText: {
    color: '#888',
    fontSize: 12,
  },
  infoCard: {
    margin: 20,
    backgroundColor: '#1a1a1a',
    borderRadius: 16,
    padding: 20,
    gap: 16,
  },
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  infoLabel: {
    flex: 1,
    fontSize: 16,
    color: '#fff',
  },
  infoValue: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#fff',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    justifyContent: 'center',
    padding: 20,
  },
  modalContent: {
    backgroundColor: '#1a1a1a',
    borderRadius: 20,
    padding: 24,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
  },
  modalTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#fff',
  },
  settingItem: {
    marginBottom: 20,
  },
  settingLabel: {
    fontSize: 16,
    color: '#fff',
    marginBottom: 8,
  },
  input: {
    backgroundColor: '#2a2a2a',
    borderRadius: 12,
    padding: 16,
    fontSize: 18,
    color: '#fff',
    borderWidth: 1,
    borderColor: '#333',
  },
  saveButton: {
    backgroundColor: '#00D9FF',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  saveButtonText: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#000',
  },
});
