# Google Play Store Submission Checklist

## Workout Timer - Pre-Launch Checklist

### ✅ Completed Items

- [x] **App Name**: "Workout Timer" (unique, descriptive)
- [x] **Package Name**: `com.workouttimer.wear` (Wear OS) / `com.workoutimer.app` (Mobile)
- [x] **Version Code**: 1
- [x] **Version Name**: 1.0
- [x] **Target SDK**: 34 (meets Google Play requirements)
- [x] **Min SDK**: 30 (Wear OS) - supports all modern Wear OS devices
- [x] **Privacy Policy**: Created (`/app/PRIVACY_POLICY.md`)
- [x] **App Icons**: Custom icons present
- [x] **Permissions Justified**: All permissions have clear use cases
- [x] **Standalone Wear App**: Configured in AndroidManifest.xml
- [x] **No Backend Dependency**: App works offline

---

### 🔧 Pre-Submission Tasks

#### 1. App Signing

```bash
# Generate release keystore (ONE TIME ONLY - KEEP SECURE!)
keytool -genkey -v -keystore workout-timer-release.keystore \
  -alias workout-timer -keyalg RSA -keysize 2048 -validity 10000

# Store password securely!
```

#### 2. Update build.gradle for Release

Add to `/app/wearos/app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("workout-timer-release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "workout-timer"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

#### 3. Build Release APK/AAB

```bash
# For Wear OS
cd /app/wearos
./gradlew bundleRelease  # Creates AAB for Play Store
./gradlew assembleRelease # Creates APK for testing

# For Mobile (Expo)
cd /app/frontend
eas build --platform android --profile production
```

---

### 📱 Play Console Requirements

#### Store Listing

| Item | Status | Notes |
|------|--------|-------|
| **App Title** | Ready | "Workout Timer" (max 30 chars) |
| **Short Description** | Needed | Max 80 characters |
| **Full Description** | Needed | Max 4000 characters |
| **App Icon** | Ready | 512x512 PNG |
| **Feature Graphic** | Needed | 1024x500 PNG |
| **Screenshots** | Needed | Min 2 (phone), Min 1 (Wear OS) |
| **App Category** | Health & Fitness | Primary category |
| **Content Rating** | Needed | Complete questionnaire |
| **Privacy Policy URL** | Needed | Host and link |

#### Suggested Short Description

> Interval workout timer with voice prompts, haptic feedback, and Wear OS support. Perfect for HIIT, Tabata, and circuit training.

#### Suggested Full Description

```
🏋️ WORKOUT TIMER - Your Personal Interval Training Coach

Professional interval timer designed for serious athletes and fitness enthusiasts. Get voice-guided workouts with haptic feedback on your wrist!

✨ KEY FEATURES:

⏱️ SMART INTERVAL TIMER
• Customizable work/rest intervals
• Round tracking with progress indicators
• Large, easy-to-read display
• Screen stays on during workouts

🗣️ VOICE GUIDANCE
• "10 seconds remaining" alerts
• "5, 4, 3, 2, 1" countdown
• Phase transition announcements
• Round completion notifications

📳 HAPTIC FEEDBACK
• Vibration alerts on your wrist
• Feel the workout without looking
• Perfect for noisy gym environments

💾 PRESET WORKOUTS
• Quick HIIT (20s/10s)
• Tabata (20s/10s x 8)
• Strength Training (45s/15s)
• Cardio Blast (60s/20s)
• Create your own custom presets!

📊 WORKOUT HISTORY
• Track completed workouts
• View total rounds and minutes
• Monitor your progress over time

❤️ HEART RATE MONITORING (Wear OS)
• Real-time heart rate during workouts
• Average HR tracking
• No external sensors needed

🌙 DESIGNED FOR FITNESS
• Dark theme for all conditions
• Works without internet
• No ads, no subscriptions
• Your data stays on your device

Perfect for:
• HIIT & High-Intensity Training
• Tabata workouts
• Circuit training
• Boxing rounds
• Crossfit WODs
• Yoga flow timing
• Meditation intervals

Download now and transform your workouts! 💪
```

---

### 📋 Content Rating Questionnaire

Expected answers for this app:

| Question | Answer |
|----------|--------|
| Violence | None |
| Sexual Content | None |
| Language | None |
| Controlled Substances | None |
| User Interaction | None |
| Data Sharing | None (all local) |
| Ads | No |
| In-App Purchases | No |

**Expected Rating**: Everyone (E)

---

### 🔐 Data Safety Section

| Question | Answer |
|----------|--------|
| Data collected | Health/Fitness data (heart rate - optional) |
| Data shared | None |
| Data encrypted | Yes (device encryption) |
| Data deletable | Yes (Clear History feature) |
| Follows Play Families Policy | N/A (not for children specifically) |

---

### 📸 Required Screenshots

#### Phone (Required if publishing mobile app)
- Minimum: 2 screenshots
- Size: 16:9 or 9:16
- Resolution: 320-3840px

#### Wear OS (Required for watch app)
- Minimum: 1 screenshot
- Recommended: 4 screenshots showing:
  1. Main timer screen (idle state)
  2. Active workout (work phase)
  3. Settings screen
  4. Presets screen

---

### 🚀 Submission Steps

1. **Create Google Play Developer Account** ($25 one-time fee)
2. **Create new app** in Play Console
3. **Upload AAB** (not APK) for production
4. **Complete store listing** (all fields above)
5. **Complete content rating** questionnaire
6. **Complete data safety** section
7. **Add privacy policy URL**
8. **Submit for review**

---

### ⚠️ Known Issues to Address Before Launch

1. **WearOS Icon**: Currently using solid color - consider adding a proper vector drawable
2. **Package Name Typo**: Mobile app has `workoutimer` (missing 't') vs WearOS has `workouttimer`
3. **ProGuard Rules**: May need app-specific rules to prevent TTS issues

---

### 📁 Files to Host Publicly

- Privacy Policy (create webpage or host on GitHub Pages)
- Terms of Service (optional but recommended)

---

### 💡 Post-Launch Recommendations

1. **Monitor reviews** for bugs and feature requests
2. **Track crash reports** via Play Console
3. **Regular updates** (every 3-6 months)
4. **Consider Google Play App Signing** for key management
5. **Add in-app review prompt** after a few workouts
