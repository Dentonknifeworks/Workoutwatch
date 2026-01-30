package com.workouttimer.wear.presentation

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.workouttimer.wear.data.PreferencesManager
import com.workouttimer.wear.presentation.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

enum class TimerState {
    IDLE, WORK, REST, PAUSED
}

@Composable
fun TimerScreen(
    preferencesManager: PreferencesManager,
    onNavigateToSettings: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onKeepAwake: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Settings from DataStore
    val workTime by preferencesManager.workTime.collectAsState(initial = 40)
    val restTime by preferencesManager.restTime.collectAsState(initial = 60)
    val totalRounds by preferencesManager.rounds.collectAsState(initial = 5)
    
    // Timer state
    var timerState by remember { mutableStateOf(TimerState.IDLE) }
    var previousState by remember { mutableStateOf(TimerState.WORK) }
    var currentRound by remember { mutableIntStateOf(1) }
    var timeLeft by remember { mutableIntStateOf(workTime) }
    var lastSpokenSecond by remember { mutableIntStateOf(-1) }
    
    // Text-to-Speech
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    
    // Initialize TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }
    
    // Cleanup TTS
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }
    
    // Helper functions
    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    fun vibrate(pattern: LongArray = longArrayOf(0, 100)) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
    
    fun vibrateHeavy() {
        vibrate(longArrayOf(0, 200, 100, 200))
    }
    
    // Update timeLeft when settings change and timer is idle
    LaunchedEffect(workTime) {
        if (timerState == TimerState.IDLE) {
            timeLeft = workTime
        }
    }
    
    // Timer logic
    LaunchedEffect(timerState) {
        if (timerState == TimerState.WORK || timerState == TimerState.REST) {
            onKeepAwake(true)
            while (timeLeft > 0 && (timerState == TimerState.WORK || timerState == TimerState.REST)) {
                delay(1000)
                if (timerState == TimerState.WORK || timerState == TimerState.REST) {
                    timeLeft--
                    
                    // Voice announcements
                    when (timeLeft) {
                        10 -> {
                            if (lastSpokenSecond != 10) {
                                speak("10 seconds")
                                vibrate()
                                lastSpokenSecond = 10
                            }
                        }
                        5 -> {
                            if (lastSpokenSecond != 5) {
                                speak("5")
                                vibrate()
                                lastSpokenSecond = 5
                            }
                        }
                        3, 2, 1 -> {
                            if (lastSpokenSecond != timeLeft) {
                                speak(timeLeft.toString())
                                vibrate()
                                lastSpokenSecond = timeLeft
                            }
                        }
                    }
                    
                    // Phase complete
                    if (timeLeft == 0) {
                        vibrateHeavy()
                        
                        when (timerState) {
                            TimerState.WORK -> {
                                timerState = TimerState.REST
                                timeLeft = restTime
                                speak("Rest!")
                                lastSpokenSecond = -1
                            }
                            TimerState.REST -> {
                                if (currentRound >= totalRounds) {
                                    // Workout complete
                                    timerState = TimerState.IDLE
                                    speak("Workout complete! Great job!")
                                    onKeepAwake(false)
                                    // Save workout history
                                    scope.launch {
                                        preferencesManager.recordWorkout(currentRound, workTime, restTime)
                                    }
                                    currentRound = 1
                                    timeLeft = workTime
                                    lastSpokenSecond = -1
                                } else {
                                    currentRound++
                                    timerState = TimerState.WORK
                                    timeLeft = workTime
                                    speak("Round $currentRound. Go!")
                                    lastSpokenSecond = -1
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        } else {
            onKeepAwake(false)
        }
    }
    
    // Start workout
    fun startWorkout() {
        timerState = TimerState.WORK
        currentRound = 1
        timeLeft = workTime
        speak("Starting workout. $totalRounds rounds. Get ready!")
        vibrateHeavy()
        lastSpokenSecond = -1
    }
    
    // Pause/Resume
    fun togglePause() {
        if (timerState == TimerState.PAUSED) {
            timerState = previousState
            speak("Resuming")
        } else {
            previousState = timerState
            timerState = TimerState.PAUSED
            speak("Paused")
        }
        vibrate()
    }
    
    // Stop workout
    fun stopWorkout() {
        // Save partial workout
        if (currentRound > 1) {
            scope.launch {
                preferencesManager.recordWorkout(currentRound - 1, workTime, restTime)
            }
        }
        timerState = TimerState.IDLE
        currentRound = 1
        timeLeft = workTime
        speak("Workout stopped")
        vibrate(longArrayOf(0, 100, 50, 100))
        lastSpokenSecond = -1
        onKeepAwake(false)
    }
    
    // Skip to next phase
    fun skipPhase() {
        when (timerState) {
            TimerState.WORK -> {
                timerState = TimerState.REST
                timeLeft = restTime
                speak("Rest time!")
                vibrateHeavy()
                lastSpokenSecond = -1
            }
            TimerState.REST -> {
                if (currentRound >= totalRounds) {
                    stopWorkout()
                } else {
                    currentRound++
                    timerState = TimerState.WORK
                    timeLeft = workTime
                    speak("Round $currentRound. Go!")
                    vibrateHeavy()
                    lastSpokenSecond = -1
                }
            }
            else -> {}
        }
    }
    
    // Format time display
    fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%d:%02d".format(mins, secs)
    }
    
    // Get circle color based on state
    val circleColor = when (timerState) {
        TimerState.WORK -> CyanPrimary
        TimerState.REST -> OrangeRest
        TimerState.PAUSED -> YellowPause
        TimerState.IDLE -> TextGray
    }
    
    val statusText = when (timerState) {
        TimerState.IDLE -> "READY"
        TimerState.WORK -> "WORK"
        TimerState.REST -> "REST"
        TimerState.PAUSED -> "PAUSED"
    }
    
    // UI
    Scaffold(
        timeText = {
            TimeText(
                timeTextStyle = TimeTextDefaults.timeTextStyle(
                    color = TextGray
                )
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                // Status badge
                Text(
                    text = statusText,
                    color = circleColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Round counter
                Text(
                    text = "Round $currentRound/$totalRounds",
                    color = TextGray,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Time display (large, circular feel)
                Text(
                    text = formatTime(timeLeft),
                    color = TextWhite,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Control buttons
                if (timerState == TimerState.IDLE) {
                    // Start and Settings buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start button
                        Button(
                            onClick = { startWorkout() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = CyanPrimary),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Text(
                                text = "▶",
                                fontSize = 24.sp,
                                color = DarkBackground
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Settings and Presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactButton(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground)
                        ) {
                            Text("⚙", fontSize = 16.sp, color = TextWhite)
                        }
                        
                        CompactButton(
                            onClick = onNavigateToPresets,
                            colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground)
                        ) {
                            Text("📋", fontSize = 16.sp, color = TextWhite)
                        }
                    }
                } else {
                    // Active workout controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pause/Resume button
                        Button(
                            onClick = { togglePause() },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (timerState == TimerState.PAUSED) GreenSuccess else YellowPause
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text(
                                text = if (timerState == TimerState.PAUSED) "▶" else "⏸",
                                fontSize = 18.sp,
                                color = DarkBackground
                            )
                        }
                        
                        // Stop button
                        Button(
                            onClick = { stopWorkout() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = RedStop),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text(
                                text = "⏹",
                                fontSize = 18.sp,
                                color = TextWhite
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Skip button
                    if (timerState != TimerState.PAUSED) {
                        CompactButton(
                            onClick = { skipPhase() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = GreenSuccess)
                        ) {
                            Text(
                                text = if (timerState == TimerState.WORK) "Skip→Rest" else "Next Round",
                                fontSize = 10.sp,
                                color = DarkBackground
                            )
                        }
                    }
                }
            }
        }
    }
}
