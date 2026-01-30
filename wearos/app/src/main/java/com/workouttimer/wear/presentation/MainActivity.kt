package com.workouttimer.wear.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.workouttimer.wear.data.PreferencesManager
import com.workouttimer.wear.presentation.theme.WorkoutTimerTheme

class MainActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferencesManager = PreferencesManager(this)
        
        setContent {
            WorkoutTimerTheme {
                WearApp(preferencesManager = preferencesManager) { keepAwake ->
                    if (keepAwake) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }
    }
}

@Composable
fun WearApp(
    preferencesManager: PreferencesManager,
    onKeepAwake: (Boolean) -> Unit
) {
    val navController = rememberSwipeDismissableNavController()
    
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "timer"
    ) {
        composable("timer") {
            TimerScreen(
                preferencesManager = preferencesManager,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToPresets = { navController.navigate("presets") },
                onKeepAwake = onKeepAwake
            )
        }
        composable("settings") {
            SettingsScreen(
                preferencesManager = preferencesManager,
                onBack = { navController.popBackStack() }
            )
        }
        composable("presets") {
            PresetsScreen(
                preferencesManager = preferencesManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
