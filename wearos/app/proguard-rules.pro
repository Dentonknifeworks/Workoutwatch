# Workout Timer App ProGuard Rules

# Keep TTS classes for voice announcements
-keep class android.speech.tts.** { *; }
-keep interface android.speech.tts.** { *; }

# Keep Vibrator for haptic feedback
-keep class android.os.Vibrator { *; }
-keep class android.os.VibrationEffect { *; }

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.runtime.**

# Keep Wear Compose
-keep class androidx.wear.compose.** { *; }
-dontwarn androidx.wear.compose.**

# Keep Health Services for heart rate
-keep class androidx.health.services.** { *; }
-dontwarn androidx.health.services.**

# Keep DataStore for preferences
-keep class androidx.datastore.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Keep Play Services Wearable
-keep class com.google.android.gms.wearable.** { *; }

# Keep app data classes
-keep class com.workouttimer.wear.data.** { *; }

# Keep app presentation classes
-keep class com.workouttimer.wear.presentation.** { *; }

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Prevent stripping of themed attributes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep BuildConfig
-keep class com.workouttimer.wear.BuildConfig { *; }
