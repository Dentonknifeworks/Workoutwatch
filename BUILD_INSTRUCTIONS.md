# Build Instructions for Workout Timer

## Overview

This project contains two applications:
1. **Mobile App** - React Native/Expo app for Android phones
2. **Wear OS App** - Native Kotlin app for Android smartwatches

---

## Mobile App (Expo/React Native)

### Prerequisites

- Node.js 18+
- Yarn package manager
- Expo CLI (`npm install -g expo-cli`)
- EAS CLI (`npm install -g eas-cli`)
- Expo account (https://expo.dev)

### Development Build

```bash
cd /app/frontend
yarn install
yarn start
```

### Production Build for Google Play

```bash
# Login to Expo
eas login

# Build Android App Bundle (AAB) for Play Store
eas build --platform android --profile production

# Or build APK for testing
eas build --platform android --profile preview
```

### Configure for Release

Update `app.json` before release:

```json
{
  "expo": {
    "version": "1.0.0",
    "android": {
      "versionCode": 1,
      "package": "com.workouttimer.app"
    }
  }
}
```

Update `eas.json` for production:

```json
{
  "build": {
    "production": {
      "android": {
        "buildType": "app-bundle"
      }
    }
  }
}
```

---

## Wear OS App (Kotlin/Jetpack Compose)

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- JDK 17
- Gradle 8.2+

### Open Project

1. Open Android Studio
2. File → Open → Select `/app/wearos`
3. Wait for Gradle sync to complete

### Debug Build

```bash
cd /app/wearos
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

#### Step 1: Create Keystore (One Time Only)

```bash
keytool -genkey -v -keystore workout-timer-release.keystore \
  -alias workout-timer \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Workout Timer, OU=Development, O=WorkoutTimer, L=City, ST=State, C=US"
```

**⚠️ IMPORTANT: Keep this keystore file and password secure! You'll need it for all future updates.**

#### Step 2: Configure Signing

Create `keystore.properties` in `/app/wearos/`:

```properties
storeFile=workout-timer-release.keystore
storePassword=your_store_password
keyAlias=workout-timer
keyPassword=your_key_password
```

Update `app/build.gradle.kts`:

```kotlin
import java.util.Properties
import java.io.FileInputStream

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

#### Step 3: Build Release

```bash
# Build App Bundle for Play Store
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab

# Or build signed APK
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

---

## Testing on Devices

### Wear OS Emulator

1. Android Studio → Tools → Device Manager
2. Create Device → Wear OS → Select watch shape
3. Choose API 34 system image
4. Start emulator and install:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Physical Watch

1. Enable Developer Options on watch:
   - Settings → System → About → Tap Build Number 7 times

2. Enable ADB debugging:
   - Settings → Developer options → ADB debugging → ON

3. Connect via WiFi:
```bash
adb connect <watch-ip>:5555
adb install app-debug.apk
```

---

## Google Play Submission

### For Wear OS App

1. Build release AAB: `./gradlew bundleRelease`
2. Go to Google Play Console
3. Create new app (Wear OS)
4. Upload `app-release.aab`
5. Complete store listing
6. Submit for review

### For Mobile App

1. Build with EAS: `eas build --platform android --profile production`
2. Download AAB from Expo dashboard
3. Upload to Play Console
4. Complete store listing
5. Submit for review

---

## Troubleshooting

### Gradle Sync Failed
```bash
./gradlew clean
./gradlew --refresh-dependencies
```

### ADB Device Not Found
```bash
adb kill-server
adb start-server
adb devices
```

### TTS Not Working on Emulator
- Some emulators have limited TTS support
- Test on physical device for accurate voice testing

### App Crashes on Launch
- Check logcat: `adb logcat -s "ActivityManager" "AndroidRuntime"`
- Ensure all Gradle dependencies downloaded
- Try clean rebuild
