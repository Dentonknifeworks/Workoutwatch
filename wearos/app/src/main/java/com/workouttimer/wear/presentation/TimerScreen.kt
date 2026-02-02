package com.workouttimer.wear.presentation

import android.Manifest
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.workouttimer.wear.data.HeartRateManager
import com.workouttimer.wear.data.PhoneCommunicator
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
    
    // Heart Rate
    val heartRateManager = remember { HeartRateManager(context) }
    val currentHR by heartRateManager.currentHeartRate.collectAsState()
    val averageHR by heartRateManager.averageHeartRate.collectAsState()
    var hrPermissionGranted by remember { mutableStateOf(heartRateManager.hasPermission()) }
    
    // Phone Communication
    val phoneCommunicator = remember { PhoneCommunicator(context) }
    var phoneConnected by remember { mutableStateOf(false) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hrPermissionGranted = isGranted
        if (isGranted) {
            heartRateManager.startMonitoring()
        }
    }
    
    // Text-to-Speech (fallback if phone not connected)
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    
    // Initialize TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        // Check phone connection
        phoneConnected = phoneCommunicator.isPhoneConnected()
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
            heartRateManager.stopMonitoring()
        }
    }
    
    // Request HR permission on first launch
    LaunchedEffect(Unit) {
        if (!hrPermissionGranted && heartRateManager.hasSensor()) {
            permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }
    }
    
    // Speak function - tries phone first, falls back to watch TTS
    fun speak(text: String) {
        scope.launch {
            if (phoneConnected) {
                val sent = phoneCommunicator.sendSpeakMessage(text)
                if (!sent) {
                    // Fallback to local TTS
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } else {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
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
            // Start heart rate monitoring
            if (hrPermissionGranted) {
                heartRateManager.startMonitoring()
            }
            
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
                                    val avgHR = if (averageHR > 0) " Average heart rate: $averageHR" else ""
                                    speak("Workout complete! Great job!$avgHR")
                                    onKeepAwake(false)
                                    heartRateManager.stopMonitoring()
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
        } else if (timerState == TimerState.IDLE) {
            onKeepAwake(false)
            heartRateManager.stopMonitoring()
        }
    }
    
    // Start workout
    fun startWorkout() {
        heartRateManager.resetAverage()
        timerState = TimerState.WORK
        currentRound = 1
        timeLeft = workTime
        speak("Start the workout. $totalRounds rounds.")
        vibrateHeavy()
        lastSpokenSecond = -1
        
        // Check phone connection
        scope.launch {
            phoneConnected = phoneCommunicator.isPhoneConnected()
        }
    }
    
    // Pause/Resume
    fun togglePause() {
        if (timerState == TimerState.PAUSED) {
            timerState = previousState
            speak("Resuming")
            if (hrPermissionGranted) {
                heartRateManager.startMonitoring()
            }
        } else {
            previousState = timerState
            timerState = TimerState.PAUSED
            speak("Paused")
            heartRateManager.stopMonitoring()
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
        heartRateManager.stopMonitoring()
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Round counter
                Text(
                    text = "Round $currentRound/$totalRounds",
                    color = TextGray,
                    fontSize = 10.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Time display (large)
                Text(
                    text = formatTime(timeLeft),
                    color = TextWhite,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                // Heart Rate Display (below timer)
                if (timerState != TimerState.IDLE && hrPermissionGranted) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "❤",
                            color = RedStop,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentHR > 0) "$currentHR" else "--",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "avg:",
                            color = TextGray,
                            fontSize = 10.sp
                        )
                        Text(
                            text = if (averageHR > 0) "$averageHR" else "--",
                            color = OrangeRest,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Phone connection indicator
                if (timerState != TimerState.IDLE && phoneConnected) {
                    Text(
                        text = "📱 Phone",
                        color = GreenSuccess,
                        fontSize = 8.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Control buttons
                if (timerState == TimerState.IDLE) {
                    // Start button - large and prominent
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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Settings and Presets - buttons with labels
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Settings button
                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground),
                            modifier = Modifier
                                .height(36.dp)
                                .width(60.dp)
                        ) {
                            Text(
                                text = "SET",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanPrimary
                            )
                        }
                        
                        // Presets button
                        Button(
                            onClick = onNavigateToPresets,
                            colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground),
                            modifier = Modifier
                                .height(36.dp)
                                .width(60.dp)
                        ) {
                            Text(
                                text = "PRE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeRest
                            )
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
                            modifier = Modifier.size(44.dp)
                        ) {
                            Text(
                                text = if (timerState == TimerState.PAUSED) "▶" else "⏸",
                                fontSize = 16.sp,
                                color = DarkBackground
                            )
                        }
                        
                        // Stop button
                        Button(
                            onClick = { stopWorkout() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = RedStop),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Text(
                                text = "⏹",
                                fontSize = 16.sp,
                                color = TextWhite
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Skip button
                    if (timerState != TimerState.PAUSED) {
                        CompactButton(
                            onClick = { skipPhase() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = GreenSuccess)
                        ) {
                            Text(
                                text = if (timerState == TimerState.WORK) "Skip" else "Next",
                                fontSize = 9.sp,
                                color = DarkBackground
                            )
                        }
                    }
                }
            }
        }
    }
}
