# Workout Timer - Product Requirements Document

## Original Problem Statement
User asked: "Is this good to go on Google Play" for the Workout Timer app.

## Application Overview

### What It Is
A professional interval workout timer application with:
- **Mobile App**: React Native/Expo for Android phones
- **Wear OS App**: Native Kotlin/Jetpack Compose for Android smartwatches

### Core Features
- Customizable work/rest interval timer
- Voice announcements (TTS) at key intervals
- Haptic feedback alerts
- Preset workouts (HIIT, Tabata, Strength, Cardio)
- Workout history tracking
- Heart rate monitoring (Wear OS only)
- Offline-first - no backend required

## User Personas

### Primary: Fitness Enthusiast
- Uses interval training regularly
- Values hands-free operation during workouts
- Owns an Android phone and/or Wear OS watch
- Prefers customizable workout configurations

### Secondary: Casual Exerciser
- New to interval training
- Uses pre-built presets
- Values simplicity and voice guidance

## Tech Stack
- **Mobile**: React Native, Expo SDK 54, TypeScript
- **Watch**: Kotlin, Jetpack Compose for Wear OS, DataStore
- **Storage**: AsyncStorage (mobile), DataStore (watch) - all local

---

## What's Been Implemented

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
| Version | 1.0 (versionCode: 1) |
| Target SDK | 34 |
| Privacy Policy | Created |
| Terms of Service | Created |
| App Icons | Present |
| ProGuard Rules | Configured |
| Build Config | Production-ready |

### ⚠️ User Action Required
| Item | Action Needed |
|------|---------------|
| Release Keystore | Generate with keytool |
| Feature Graphic | Create 1024x500 PNG |
| Screenshots | Capture from device/emulator |
| Play Developer Account | Create ($25 fee) |
| Privacy Policy URL | Host publicly |
| Content Rating | Complete questionnaire |
| Data Safety | Complete in Play Console |

---

## Prioritized Backlog

### P0 - Critical (Before Launch)
- [ ] Generate release keystore
- [ ] Create store screenshots
- [ ] Host privacy policy URL
- [ ] Complete Play Console setup

### P1 - High Priority (Post-Launch)
- [ ] Add in-app review prompt
- [ ] Create feature graphic
- [ ] Monitor crash reports
- [ ] Add app update mechanism

### P2 - Nice to Have
- [ ] Custom app icon (currently solid color for WearOS)
- [ ] More preset workouts
- [ ] Workout sharing/export
- [ ] Multiple language support

---

## Next Steps

1. **Generate Keystore**: Run keytool command from BUILD_INSTRUCTIONS.md
2. **Build Release**: `./gradlew bundleRelease` for Wear OS
3. **Create Screenshots**: Use emulator or physical device
4. **Host Privacy Policy**: GitHub Pages or similar
5. **Submit to Play Console**: Follow GOOGLE_PLAY_CHECKLIST.md

---

## Files Reference

```
/app/
├── PRIVACY_POLICY.md          # Required for Play Store
├── TERMS_OF_SERVICE.md        # Recommended
├── GOOGLE_PLAY_CHECKLIST.md   # Complete submission guide
├── BUILD_INSTRUCTIONS.md      # Build and release guide
├── frontend/                  # Mobile app (Expo)
│   ├── app.json              # App config (package name fixed)
│   └── eas.json              # Build config (app-bundle)
└── wearos/                   # Wear OS app
    ├── app/build.gradle.kts  # Release signing configured
    ├── proguard-rules.pro    # Obfuscation rules
    └── app/src/main/AndroidManifest.xml  # Permissions documented
```
