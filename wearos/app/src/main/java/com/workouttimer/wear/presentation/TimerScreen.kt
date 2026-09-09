package com.workouttimer.wear.presentation

import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    onNavigateToPhoneControl: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val audioFocusRequest = remember {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
                    .setOnAudioFocusChangeListener { }
            .setWillPauseWhenDucked(false)
            .build()
    }
    
    // Settings from DataStore
    val workTime by preferencesManager.workTime.collectAsState(initial = 35)
    val restTime by preferencesManager.restTime.collectAsState(initial = 60)
    val totalRounds by preferencesManager.rounds.collectAsState(initial = 4)
    
    // Timer state
    var timerState by remember { mutableStateOf(TimerState.IDLE) }
    var previousState by remember { mutableStateOf(TimerState.WORK) }
    var currentRound by remember { mutableIntStateOf(1) }
    var timeLeft by remember { mutableIntStateOf(workTime) }
    var totalTimeLeft by remember { mutableIntStateOf(0) }
    var totalTimerRunning by remember { mutableStateOf(false) }
    var workoutSession by remember { mutableIntStateOf(0) }
    var lastVibrationSecond by remember { mutableIntStateOf(-1) }
    
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
    
    // Check phone connection
    LaunchedEffect(Unit) {
        preferencesManager.migrateRequestedDefaults()
        preferencesManager.ensureDefaultTotalMinutes()
        phoneConnected = phoneCommunicator.isPhoneConnected()
    }

    // Keep the timer screen awake while this screen is visible.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
            heartRateManager.stopMonitoring()
        }
    }
    
    // Request HR permission on first launch
    LaunchedEffect(Unit) {
        if (!hrPermissionGranted && heartRateManager.hasSensor()) {
            permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
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
            // Start heart rate monitoring
            if (hrPermissionGranted) {
                heartRateManager.startMonitoring()
            }
            
            while (timeLeft > 0 && (timerState == TimerState.WORK || timerState == TimerState.REST)) {
                delay(1000)
                if (timerState == TimerState.WORK || timerState == TimerState.REST) {
                    timeLeft--
                    
                    // Haptic countdown cues only
                    when (timeLeft) {
                        10 -> {
                            if (lastVibrationSecond != 10) {
                                vibrate()
                                lastVibrationSecond = 10
                            }
                        }
                        5 -> {
                            if (lastVibrationSecond != 5) {
                                vibrate()
                                lastVibrationSecond = 5
                            }
                        }
                        4, 3, 2, 1 -> {
                            if (lastVibrationSecond != timeLeft) {
                                vibrate()
                                lastVibrationSecond = timeLeft
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
                                lastVibrationSecond = -1
                            }
                            TimerState.REST -> {
                                if (currentRound >= totalRounds) {
                                    // Workout complete
                                    timerState = TimerState.IDLE
                                    heartRateManager.stopMonitoring()
                                    // Save workout history
                                    scope.launch {
                                        preferencesManager.recordWorkout(currentRound, workTime, restTime)
                                    }
                                    currentRound = 1
                                    timeLeft = workTime
                                    lastVibrationSecond = -1
                                } else {
                                    currentRound++
                                    timerState = TimerState.WORK
                                    timeLeft = workTime
                                    lastVibrationSecond = -1
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        } else if (timerState == TimerState.IDLE) {
            heartRateManager.stopMonitoring()
        }
    }
    
    // Start workout
    fun startWorkout() {
        heartRateManager.resetAverage()
        workoutSession++
        totalTimeLeft = 0
        timerState = TimerState.WORK
        totalTimerRunning = true
        currentRound = 1
        timeLeft = workTime
        vibrateHeavy()
        lastVibrationSecond = -1
        
        // Check phone connection
        scope.launch {
            phoneConnected = phoneCommunicator.isPhoneConnected()
        }
    }
    
    // Pause/Resume
    fun togglePause() {
        if (timerState == TimerState.PAUSED) {
            timerState = previousState
            if (hrPermissionGranted) {
                heartRateManager.startMonitoring()
            }
        } else {
            previousState = timerState
            timerState = TimerState.PAUSED
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
        totalTimerRunning = false
        workoutSession++
        currentRound = 1
        timeLeft = workTime
        totalTimeLeft = 0
        vibrate(longArrayOf(0, 100, 50, 100))
        lastVibrationSecond = -1
        heartRateManager.stopMonitoring()
    }
    
    // Skip to next phase
    fun skipPhase() {
        when (timerState) {
            TimerState.WORK -> {
                timerState = TimerState.REST
                timeLeft = restTime
                vibrateHeavy()
                lastVibrationSecond = -1
            }
            TimerState.REST -> {
                if (currentRound >= totalRounds) {
                    scope.launch {
                        preferencesManager.recordWorkout(currentRound, workTime, restTime)
                    }
                    timerState = TimerState.IDLE
                    currentRound = 1
                    timeLeft = workTime
                    lastVibrationSecond = -1
                } else {
                    currentRound++
                    timerState = TimerState.WORK
                    timeLeft = workTime
                    vibrateHeavy()
                    lastVibrationSecond = -1
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
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                // Time display (large) with HR on sides
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left side - Current HR
                    if (timerState != TimerState.IDLE && hrPermissionGranted) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(40.dp)
                        ) {
                            Text(
                                text = "❤",
                                color = RedStop,
                                fontSize = 10.sp
                            )
                            Text(
                                text = if (currentHR > 0) "$currentHR" else "--",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(40.dp))
                    }
                    
                    // Center - phase timer only
                    Box(
                        modifier = Modifier.size(112.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatTime(timeLeft),
                            color = TextWhite,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Right side - Average HR
                    if (timerState != TimerState.IDLE && hrPermissionGranted) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(40.dp)
                        ) {
                            Text(
                                text = "avg",
                                color = TextGray,
                                fontSize = 8.sp
                            )
                            Text(
                                text = if (averageHR > 0) "$averageHR" else "--",
                                color = OrangeRest,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(40.dp))
                    }
                }

                // Keep the status and round counter centered directly below the timer.
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = circleColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " • ",
                        color = TextGray,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "$currentRound/$totalRounds",
                        color = TextGray,
                        fontSize = 10.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))

                // Control buttons
                if (timerState == TimerState.IDLE) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { startWorkout() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = CyanPrimary),
                            modifier = Modifier
                                .height(40.dp)
                                .width(64.dp)
                        ) {
                            Text(
                                text = "▶",
                                fontSize = 20.sp,
                                color = DarkBackground
                            )
                        }

                        Button(
                            onClick = {
                                val activity = context as? android.app.Activity
                                activity?.finish()
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = RedStop),
                            modifier = Modifier
                                .height(40.dp)
                                .width(64.dp)
                        ) {
                            Text(
                                text = "EXIT",
                                fontSize = 11.sp,
                                color = TextWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground),
                            modifier = Modifier
                                .height(34.dp)
                                .fillMaxWidth(0.8f)
                        ) {
                            Text(
                                text = "EDIT SETTINGS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanPrimary
                            )
                        }

                        Button(
                            onClick = onNavigateToPresets,
                            colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground),
                            modifier = Modifier
                                .height(34.dp)
                                .fillMaxWidth(0.8f)
                        ) {
                            Text(
                                text = "PRESETS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeRest
                            )
                        }

                        Button(
                            onClick = onNavigateToPhoneControl,
                            colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground),
                            modifier = Modifier
                                .height(34.dp)
                                .fillMaxWidth(0.8f)
                        ) {
                            Text(
                                text = "PHONE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGray
                            )
                        }
                    }
                } else {
                    // Active workout controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { togglePause() },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (timerState == TimerState.PAUSED) GreenSuccess else YellowPause
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .width(52.dp)
                        ) {
                            Text(
                                text = if (timerState == TimerState.PAUSED) "▶" else "⏸",
                                fontSize = 16.sp,
                                color = DarkBackground
                            )
                        }

                        Button(
                            onClick = { stopWorkout() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = RedStop),
                            modifier = Modifier
                                .height(36.dp)
                                .width(52.dp)
                        ) {
                            Text(
                                text = "⏹",
                                fontSize = 12.sp,
                                color = TextWhite
                            )
                        }

                        if (timerState != TimerState.PAUSED) {
                            Button(
                                onClick = { skipPhase() },
                                colors = ButtonDefaults.buttonColors(backgroundColor = GreenSuccess),
                                modifier = Modifier
                                    .height(36.dp)
                                    .width(52.dp)
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
}
