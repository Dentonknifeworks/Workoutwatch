# Workout Timer - Wear OS App

A standalone Wear OS workout timer app with interval training, voice prompts, and haptic feedback.

## Features

- ⏱️ **Interval Timer** - Configurable work/rest cycles
- 🗣️ **Voice Prompts** - Audio announcements at key intervals (10s, 5s, 3-2-1 countdown)
- 📳 **Haptic Feedback** - Vibration alerts on your wrist
- ⚙️ **Customizable Settings** - Adjust work time, rest time, and rounds
- 📋 **Preset Workouts** - Quick HIIT, Tabata, Strength, Cardio
- 🌙 **Screen Always On** - Keeps display active during workouts
- 💾 **Persistent Settings** - Saves your preferences locally

## Default Settings

- Work Time: 40 seconds
- Rest Time: 60 seconds
- Rounds: 5

## How to Build

### Prerequisites

1. **Android Studio** (latest version recommended - Hedgehog or newer)
2. **Android SDK** with API 34
3. **Wear OS Emulator** or physical Wear OS device

### Step-by-Step Build Instructions

1. **Open the project in Android Studio:**
   ```
   File → Open → Select the /app/wearos folder
   ```

2. **Wait for Gradle sync to complete**
   - Android Studio will download dependencies automatically
   - This may take a few minutes on first build

3. **Create a Wear OS Emulator (if needed):**
   - Tools → Device Manager → Create Device
   - Select "Wear OS" category
   - Choose "Wear OS Small Round" or "Wear OS Large Round"
   - Select API 34 system image
   - Finish setup

4. **Build the APK:**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```
   
   Or from command line:
   ```bash
   cd /app/wearos
   ./gradlew assembleDebug
   ```

5. **Locate the APK:**
   ```
   /app/wearos/app/build/outputs/apk/debug/app-debug.apk
   ```

### Installing on Watch Emulator

1. **Start the Wear OS emulator**

2. **Install via Android Studio:**
   - Run → Select your Wear OS emulator → Run 'app'

3. **Or install via ADB:**
   ```bash
   adb -s emulator-5554 install app/build/outputs/apk/debug/app-debug.apk
   ```

### Installing on Physical Watch

1. **Enable Developer Options on watch:**
   - Settings → System → About → Tap "Build number" 7 times

2. **Enable ADB Debugging:**
   - Settings → Developer options → ADB debugging → ON

3. **Connect via WiFi or Bluetooth:**
   ```bash
   adb connect <watch-ip-address>:5555
   adb install app-debug.apk
   ```

## App Navigation

### Main Timer Screen
- **▶ Start** - Begin workout
- **⚙ Settings** - Adjust work/rest/rounds
- **📋 Presets** - Load preset workouts

### During Workout
- **⏸ Pause/Resume** - Pause or resume timer
- **⏹ Stop** - End workout
- **Skip→Rest / Next Round** - Skip current phase

### Settings Screen
- Tap **+/-** to adjust values
- Tap **Save** to apply changes
- Swipe right or tap **← Back** to return

### Presets Screen
- Tap a preset card to apply it
- Swipe right or tap **← Back** to return

## Voice Announcements

The app speaks:
- "Starting workout. X rounds. Get ready!" at start
- "10 seconds" when 10 seconds remain
- "5, 4, 3, 2, 1" countdown
- "Rest!" when work phase ends
- "Round X. Go!" when rest ends
- "Workout complete! Great job!" when finished

## Troubleshooting

### No sound on emulator
- Emulators may have limited TTS support
- Test on physical device for full audio experience

### Vibration not working
- Enable haptic feedback in watch settings
- Some emulators don't support vibration

### App crashes on start
- Ensure Wear OS system image is API 30+
- Check that all Gradle dependencies downloaded

## Project Structure

```
wearos/
├── app/
│   ├── build.gradle.kts        # App dependencies
│   └── src/main/
│       ├── AndroidManifest.xml # Wear OS config
│       ├── java/com/workouttimer/wear/
│       │   ├── data/
│       │   │   ├── PreferencesManager.kt  # Settings storage
│       │   │   └── WorkoutPreset.kt       # Preset data model
│       │   └── presentation/
│       │       ├── MainActivity.kt        # Entry point
│       │       ├── TimerScreen.kt         # Main timer UI
│       │       ├── SettingsScreen.kt      # Settings UI
│       │       ├── PresetsScreen.kt       # Presets UI
│       │       └── theme/
│       │           ├── Color.kt           # Color definitions
│       │           └── Theme.kt           # Wear OS theme
│       └── res/
│           └── values/
│               ├── strings.xml
│               └── colors.xml
├── build.gradle.kts            # Project config
├── settings.gradle.kts         # Module settings
└── gradle.properties           # Gradle properties
```

## Technology Stack

- **Kotlin** - Programming language
- **Jetpack Compose for Wear OS** - Modern declarative UI
- **DataStore** - Persistent settings storage
- **TextToSpeech** - Voice announcements
- **Vibrator API** - Haptic feedback
