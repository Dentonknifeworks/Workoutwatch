# Play Store Submission Guide

## Pre-Submission Checklist

### Assets Ready
- [x] App Icon: `icon-512.png` (512×512)
- [x] Feature Graphic: `feature-graphic.png` (1024×500)
- [x] Store Listing Text: `STORE_LISTING.md`
- [x] Release Notes: `WHATS_NEW.md`
- [ ] Screenshots (you need to capture these)

### Required Screenshots

**Wear OS (Required):**
- Minimum: 1 screenshot
- Recommended: 4 screenshots
- Capture from watch or emulator:
  1. Main timer screen (idle)
  2. Active workout (work phase)
  3. Settings screen
  4. Presets screen

**How to take screenshots:**
- On watch: Press both buttons simultaneously
- On emulator: Click camera icon in emulator toolbar
- Via ADB: `adb exec-out screencap -p > screenshot.png`

---

## Step-by-Step Submission

### 1. Create Developer Account
- Go to: https://play.google.com/console
- Pay $25 one-time fee
- Complete identity verification

### 2. Create New App
1. Click "Create app"
2. Select "App" (not game)
3. Choose "Free"
4. App name: "Workout Timer"
5. Select "Health & Fitness" category

### 3. Store Listing
1. Go to "Main store listing"
2. Copy text from `STORE_LISTING.md`
3. Upload `icon-512.png` as app icon
4. Upload `feature-graphic.png`
5. Upload your screenshots

### 4. Content Rating
1. Go to "Content rating"
2. Start questionnaire
3. Answer all questions (all "No" for this app)
4. Expected rating: Everyone (E)

### 5. Data Safety
1. Go to "Data safety"
2. Does your app collect data? → Yes (workout history, heart rate)
3. Is data shared? → No
4. Is data encrypted? → Yes
5. Can users request deletion? → Yes (Clear History feature)

### 6. App Content
1. Privacy Policy URL: (your hosted URL)
2. App access: All functionality available without login
3. Ads: No
4. Target audience: Not specifically for children

### 7. Release
1. Go to "Production"
2. Click "Create new release"
3. Upload your signed AAB file
4. Add release notes from `WHATS_NEW.md`
5. Review and roll out

---

## Building the Release AAB

### For Wear OS (Android Studio)
```bash
cd /path/to/wearos
./gradlew bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`

### For Mobile (Expo)
```bash
cd /path/to/frontend
eas build --platform android --profile production
```
Download AAB from Expo dashboard.

---

## Contact Info for Store

```
Email: support@workouttimer.app
Privacy Policy: [YOUR_URL_HERE]
```

---

## Timeline

- Initial review: 1-3 days
- If rejected: Fix issues and resubmit
- After approval: Live within hours

---

## Common Rejection Reasons

1. **Missing privacy policy** → Host and link it
2. **Broken functionality** → Test thoroughly
3. **Misleading description** → Be accurate
4. **Poor screenshots** → Show actual app
5. **Crashes on launch** → Test on multiple devices
