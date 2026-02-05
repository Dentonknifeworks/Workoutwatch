# Workout Timer - Product Requirements Document

## Original Problem Statement
User asked: "Is this good to go on Google Play" for the Workout Timer app, then requested the ability to control the phone app's timer from the watch because their earbuds don't support Bluetooth multipoint.

## Application Overview

### What It Is
A professional interval workout timer application with:
- **Mobile App**: React Native/Expo for Android phones
- **Wear OS App**: Native Kotlin/Jetpack Compose for Android smartwatches
- **Watch-to-Phone Control**: Control phone timer from watch via MessageClient API

### Core Features
- Customizable work/rest interval timer
- Voice announcements (TTS) at key intervals
- Haptic feedback alerts
- Preset workouts (HIIT, Tabata, Strength, Cardio)
- Workout history tracking
- Heart rate monitoring (Wear OS only)
- **Watch Control**: Start/Pause/Stop/Skip phone timer from watch
- Offline-first - no backend required

## User Personas

### Primary: Fitness Enthusiast
- Uses interval training regularly
- Values hands-free operation during workouts
- Owns an Android phone and/or Wear OS watch
- Prefers customizable workout configurations
- **Uses earbuds without Bluetooth multipoint** (needs watch to control phone)

### Secondary: Casual Exerciser
- New to interval training
- Uses pre-built presets
- Values simplicity and voice guidance

## Tech Stack
- **Mobile**: React Native, Expo SDK 54, TypeScript
- **Watch**: Kotlin, Jetpack Compose for Wear OS, DataStore
- **Communication**: Wear OS MessageClient API (play-services-wearable)
- **Storage**: AsyncStorage (mobile), DataStore (watch) - all local

---

## What's Been Implemented

### December 2025 - Watch Control Feature

#### New Features
- [x] Watch can send timer commands to phone (Start, Pause, Stop, Skip)
- [x] Phone app listens for watch commands via react-native-wear-connectivity
- [x] New "Phone Control" screen on watch with control buttons
- [x] Phone app shows watch indicator icon when running on Android
- [x] Updated PhoneCommunicator.kt with timer command methods

#### Files Modified/Created
- `/app/wearos/app/src/main/java/com/workouttimer/wear/data/PhoneCommunicator.kt` - Extended with timer commands
- `/app/wearos/app/src/main/java/com/workouttimer/wear/presentation/PhoneControlScreen.kt` - New screen
- `/app/wearos/app/src/main/java/com/workouttimer/wear/presentation/MainActivity.kt` - Added navigation
- `/app/wearos/app/src/main/java/com/workouttimer/wear/presentation/TimerScreen.kt` - Added phone button
- `/app/frontend/hooks/useWatchControl.ts` - New hook for watch messages
- `/app/frontend/app/(tabs)/timer.tsx` - Integrated watch control
- `/app/frontend/plugins/withWearConnectivity.js` - Expo config plugin
- `/app/frontend/app.json` - Added plugin, bumped versionCode to 4

### January 2026 - Google Play Readiness Review

#### Created Documents
- [x] `/app/PRIVACY_POLICY.md` - Comprehensive privacy policy
- [x] `/app/TERMS_OF_SERVICE.md` - Terms of service
- [x] `/app/GOOGLE_PLAY_CHECKLIST.md` - Complete submission checklist
- [x] `/app/BUILD_INSTRUCTIONS.md` - Build and release guide

#### Fixed Issues
- [x] Package name typo fixed (`workoutimer` → `workouttimer`)
- [x] EAS config updated for app-bundle (Play Store requirement)
- [x] WearOS build.gradle updated with release signing config
- [x] ProGuard rules added for TTS, Compose, Health Services
- [x] AndroidManifest permission comments added for Data Safety

#### Verified Items
- [x] App icons present and proper size (255KB)
- [x] Target SDK 34 (meets current requirements)
- [x] Standalone Wear OS app configured
- [x] All required permissions justified

---

## Google Play Readiness Status

### ✅ Ready
| Item | Status |
|------|--------|
| Package Name | `com.workouttimer.wear` / `com.workouttimer.app` |
| Version | 1.0 (versionCode: 4) |
| Target SDK | 34 |
| Privacy Policy | Created |
| Terms of Service | Created |
| App Icons | Present |
| ProGuard Rules | Configured |
| Build Config | Production-ready |
| Watch Control | Implemented |

### ⚠️ User Action Required
| Item | Action Needed |
|------|---------------|
| Release Keystore | Generate with keytool |
| Feature Graphic | Create 1024x500 PNG |
| Screenshots | Capture from device/emulator |
| Play Developer Account | Pending verification |
| Privacy Policy URL | Host publicly |
| Content Rating | Complete questionnaire |
| Data Safety | Complete in Play Console |

---

## How Watch Control Works

### Architecture
```
[Watch App]                    [Phone App]
    |                              |
    |-- sendTimerCommand() ------->|
    |   (MessageClient API)        |
    |                              |-- useWatchControl hook
    |                              |-- handles: start/pause/stop/skip
    |                              |-- triggers timer actions
```

### Message Format
```json
{
  "command": "start|pause|stop|skip",
  "timestamp": 1703123456789,
  "payload": {}
}
```

### User Flow
1. Open phone app, start timer or leave it idle
2. On watch, go to Timer screen → tap 📱 button
3. Use watch's Phone Control screen to Start/Pause/Stop/Skip
4. Phone receives command and executes timer action with voice/haptics

---

## Prioritized Backlog

### P0 - Critical (Before Launch)
- [x] ~~Implement watch-to-phone control~~ ✅
- [ ] Generate release keystore
- [ ] Create store screenshots
- [ ] Host privacy policy URL
- [ ] Complete Play Console setup (waiting for verification)

### P1 - High Priority (Post-Launch)
- [ ] Add in-app review prompt
- [ ] Create feature graphic
- [ ] Monitor crash reports
- [ ] Add app update mechanism
- [ ] Bidirectional sync (phone sends state to watch)

### P2 - Nice to Have
- [ ] Custom app icon (currently solid color for WearOS)
- [ ] More preset workouts
- [ ] Workout sharing/export
- [ ] Multiple language support

---

## Build Instructions

### Phone App (with Watch Control)
```bash
cd /app/frontend
npx eas build --platform android --profile preview
```

### Watch App
```bash
cd /app/wearos
./gradlew assembleDebug  # or assembleRelease for Play Store
```

---

## Files Reference

```
/app/
├── PRIVACY_POLICY.md              # Required for Play Store
├── TERMS_OF_SERVICE.md            # Recommended
├── GOOGLE_PLAY_CHECKLIST.md       # Complete submission guide
├── BUILD_INSTRUCTIONS.md          # Build and release guide
├── frontend/                      # Mobile app (Expo)
│   ├── app.json                   # App config (versionCode: 4)
│   ├── eas.json                   # Build config (app-bundle)
│   ├── plugins/
│   │   └── withWearConnectivity.js  # Expo plugin for Wearable API
│   ├── hooks/
│   │   └── useWatchControl.ts     # Hook for watch messages
│   └── app/(tabs)/
│       └── timer.tsx              # Timer screen with watch control
└── wearos/                        # Wear OS app
    ├── app/build.gradle.kts       # Release signing configured
    ├── proguard-rules.pro         # Obfuscation rules
    └── app/src/main/java/com/workouttimer/wear/
        ├── data/
        │   └── PhoneCommunicator.kt  # Timer commands
        └── presentation/
            ├── MainActivity.kt       # Navigation
            ├── TimerScreen.kt        # Timer + phone button
            └── PhoneControlScreen.kt # Control phone from watch
```
