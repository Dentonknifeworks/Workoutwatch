# Workoutwatch

Workoutwatch is an interval workout timer with a React Native phone app and a standalone Wear OS app.

## Current Behavior

- Configure work seconds, rest seconds, and rounds.
- Default Wear settings are 35 seconds work, 60 seconds rest, and 4 rounds.
- The Wear app uses vibration alerts at phase changes and 10, 5, 4, 3, 2, and 1 seconds remaining.
- The Wear app has editable settings and workout presets.
- Settings are stored locally on each device.

## Project Areas

- `frontend/` - Expo React Native phone app.
- `wearos/` - Kotlin and Jetpack Compose standalone Wear OS app.
- `backend/` - Optional FastAPI backend.

## Documentation

- [Phone app guide](WORKOUT_TIMER_README.md)
- [Wear OS build and install guide](wearos/README.md)
- [Build instructions](BUILD_INSTRUCTIONS.md)

## Quick Start

Phone app:

```bash
cd frontend
npm install
npx expo start
```

Wear OS app:

```bash
cd wearos
./gradlew assembleDebug
```

Install the resulting `wearos/app/build/outputs/apk/debug/app-debug.apk` with Android Studio or ADB. Use an in-place install for updates so locally saved settings are preserved.
