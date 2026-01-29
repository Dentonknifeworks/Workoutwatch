# 🏋️ Workout Timer with Voice Prompts & Android Watch Support

A professional interval workout timer app built with Expo React Native, featuring voice announcements, haptic feedback, notifications, and Android watch integration support.

## ✨ Features

### Core Timer Functionality
- ⏱️ **Interval Timer** - Customizable work/rest cycles with round tracking
- 🎯 **Large Touch Controls** - Thumb-friendly Start, Pause, and Stop buttons (80-120px)
- 📱 **Visual Feedback** - Color-coded states (Work/Rest/Paused) with animated circle
- 🔄 **Smart Reset** - Automatic round progression and completion detection

### Voice & Audio Features
- 🗣️ **Voice Announcements** - Text-to-speech prompts at key intervals:
  - "10 seconds remaining"
  - "5, 4, 3, 2, 1" countdown
  - "Work!" / "Rest!" phase transitions
  - "Round X" announcements
  - "Workout complete!"
- 🔊 **Sound Alerts** - Beep sounds at countdown intervals
- 📳 **Haptic Feedback** - Vibration on button taps and timer events

### Android Watch Integration
- ⌚ **Push Notifications** - Workout state changes sent to watch
- 🔔 **Background Alerts** - Notifications work when app is minimized
- 🎙️ **Voice Control Ready** - Audio permissions configured for voice commands
- 🔓 **Wake Lock** - Keeps device awake during workouts

### Workout Management
- 💾 **Presets System**
  - Quick HIIT (20s work / 10s rest / 8 rounds)
  - Tabata (20s work / 10s rest / 8 rounds)
  - Strength Training (45s work / 15s rest / 6 rounds)
  - Cardio Blast (60s work / 20s rest / 10 rounds)
  - Create custom presets
  - Delete and apply presets to timer

- 📊 **Workout History**
  - Tracks completed workouts automatically
  - Statistics dashboard (total workouts, rounds, minutes)
  - Detailed workout log with timestamps
  - Clear history option

### User Experience
- 🌙 **Dark Theme** - Eye-friendly design for all lighting conditions
- 📲 **Tab Navigation** - Easy switching between Timer, Presets, and History
- ⚙️ **Settings Modal** - Quick adjustments without leaving timer
- 💾 **Auto-Save** - Last settings and presets persist across sessions
- 🎨 **High Contrast** - Readable outdoors and in bright light

## 🛠️ Technical Stack

### Frontend
- **Framework**: Expo React Native (SDK 54)
- **Router**: Expo Router (file-based routing)
- **UI**: React Native components with StyleSheet
- **Icons**: @expo/vector-icons (Ionicons)

### Libraries
- `expo-speech` - Text-to-speech voice announcements
- `expo-audio` - Sound effects and beeps
- `expo-haptics` - Vibration feedback
- `expo-notifications` - Push notifications for watch
- `expo-keep-awake` - Prevent screen sleep during workouts
- `@react-native-async-storage/async-storage` - Local data persistence

### Backend
- **API**: FastAPI (Python)
- **Database**: MongoDB with Motor (async driver)
- **Note**: Backend not required for MVP - app uses local storage

## 📱 App Structure

```
frontend/
├── app/
│   ├── _layout.tsx                 # Root layout
│   ├── index.tsx                   # Splash screen
│   └── (tabs)/
│       ├── _layout.tsx            # Tab navigation
│       ├── timer.tsx              # Main timer screen
│       ├── presets.tsx            # Preset management
│       └── history.tsx            # Workout history
└── app.json                        # Expo config with permissions
```

## 🔐 Permissions

### iOS
- `NSMicrophoneUsageDescription` - Voice prompts
- `NSSpeechRecognitionUsageDescription` - Voice control
- `UIBackgroundModes: ["audio"]` - Background audio

### Android
- `VIBRATE` - Haptic feedback
- `WAKE_LOCK` - Keep screen awake
- `RECEIVE_BOOT_COMPLETED` - Notification persistence
- `FOREGROUND_SERVICE` - Background timer
- `POST_NOTIFICATIONS` - Watch notifications

## 🚀 Getting Started

### Installation
```bash
cd /app/frontend
yarn install
```

### Run Development Server
```bash
yarn start
```

### Build for Android Watch Testing
```bash
# Create development build with Expo Go
expo start --android

# For production with watch support
eas build --platform android
```

## 💡 Usage Guide

### Starting a Workout
1. Open the **Timer** tab
2. Click **Settings** (⚙️) to customize work time, rest time, and rounds
3. Tap the large **Start** button
4. Follow voice prompts and on-screen timer

### Using Presets
1. Go to **Presets** tab
2. Browse default presets or create custom ones
3. Tap "Use This Preset" to apply settings
4. Return to Timer tab and start workout

### Viewing History
1. Navigate to **History** tab
2. See total statistics (workouts, rounds, minutes)
3. Scroll through workout log with dates and details
4. Clear history if needed

## 🎯 Android Watch Features

When running on Android devices:
- Notifications automatically appear on paired watches
- Voice announcements play through watch speaker
- Watch will vibrate at key intervals
- Background notifications work even when phone is locked

## 🔧 Customization

### Modify Timer Defaults
Edit `/app/frontend/app/(tabs)/timer.tsx`:
```typescript
const [settings, setSettings] = useState<WorkoutSettings>({
  workTime: 30,    // seconds
  restTime: 10,    // seconds
  rounds: 5,       // number of rounds
});
```

### Add New Presets
Edit `/app/frontend/app/(tabs)/presets.tsx`:
```typescript
const defaultPresets: WorkoutPreset[] = [
  {
    id: '5',
    name: 'Your Custom Preset',
    workTime: 40,
    restTime: 20,
    rounds: 10,
    createdAt: new Date().toISOString(),
  },
];
```

### Adjust Voice Prompts
Edit `/app/frontend/app/(tabs)/timer.tsx`:
```typescript
// Change when voice announcements trigger
if (newTime === 10 && lastSpokenSecond.current !== 10) {
  speak('10 seconds');
}
```

## 📊 Local Data Storage

App uses AsyncStorage to persist:
- **lastWorkoutSettings** - Last used timer configuration
- **workoutPresets** - User-created and default presets
- **workoutHistory** - Last 50 workout sessions

## 🐛 Troubleshooting

### Voice Not Working
- Ensure device volume is up
- Check system text-to-speech is enabled
- Grant microphone permissions in settings

### Notifications Not Appearing on Watch
- Verify watch is paired with phone
- Enable notifications in Android settings
- Check app notification permissions

### Timer Stops When Screen Locks
- App uses keep-awake during workouts
- Check battery optimization settings
- Allow background activity for the app

## 🎨 Design Principles

### Mobile-First
- Minimum 44x44pt touch targets (iOS HIG)
- 8pt grid spacing system
- High contrast ratios (WCAG AA compliant)

### Performance
- Expo-audio for efficient sound playback
- AsyncStorage for instant data access
- Optimized re-renders with React hooks

### Accessibility
- Large, readable fonts (16-72pt)
- Color-blind friendly palette
- Haptic feedback for all interactions
- Voice guidance throughout workout

## 📈 Future Enhancements

Potential additions:
- [ ] Custom exercise names per round
- [ ] Background music integration
- [ ] Workout sharing and export
- [ ] Weekly/monthly statistics
- [ ] Achievement badges
- [ ] Apple Watch support
- [ ] Multi-language support
- [ ] Custom sound uploads

## 📄 License

This project is part of the Emergent AI platform.

## 🤝 Support

For issues or questions:
- Check console logs in development
- Review notification permissions
- Verify audio/haptics are enabled
- Test on physical device for full functionality

---

**Built with ❤️ using Expo and React Native**
