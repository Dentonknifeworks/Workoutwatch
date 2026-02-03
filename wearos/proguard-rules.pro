# Workout Timer ProGuard Rules

# Keep TTS classes
-keep class android.speech.tts.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Wear OS
-keep class androidx.wear.** { *; }
-dontwarn androidx.wear.**

# Keep Health Services
-keep class androidx.health.** { *; }
-dontwarn androidx.health.**

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Keep data classes
-keep class com.workouttimer.wear.data.** { *; }

# Keep R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}
