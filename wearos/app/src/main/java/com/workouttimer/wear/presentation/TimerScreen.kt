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
    onNavigateToPhoneControl: () -> Unit,
    onKeepAwake: (Boolean) -> Unit
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
    val workTime by preferencesManager.workTime.collectAsState(initial = 40)
    val restTime by preferencesManager.restTime.collectAsState(initial = 60)
    val totalRounds by preferencesManager.rounds.collectAsState(initial = 5)
    val totalMinutes by preferencesManager.totalMinutes.collectAsState(initial = 90)
    
    // Timer state
    var timerState by remember { mutableStateOf(TimerState.IDLE) }
    var previousState by remember { mutableStateOf(TimerState.WORK) }
    var currentRound by remember { mutableIntStateOf(1) }
    var timeLeft by remember { mutableIntStateOf(workTime) }
    var totalTimeLeft by remember { mutableIntStateOf(totalMinutes * 60) }
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
        preferencesManager.ensureDefaultTotalMinutes()
        phoneConnected = phoneCommunicator.isPhoneConnected()
    }

    // Keep the timer screen awake while it is active.
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

    LaunchedEffect(totalMinutes) {
        if (timerState == TimerState.IDLE) {
            totalTimeLeft = totalMinutes * 60
        }
    }

    // Total workout time runs independently of each work/rest phase.
    LaunchedEffect(workoutSession, totalTimerRunning) {
        if (totalTimerRunning) {
            onKeepAwake(true)
            var remainingTotalTime = totalTimeLeft
            while (remainingTotalTime > 0 && totalTimerRunning) {
                delay(1000)
                remainingTotalTime--
                totalTimeLeft = remainingTotalTime
            }

            if (remainingTotalTime <= 0 && totalTimerRunning) {
                totalTimerRunning = false
                vibrateHeavy()
                timerState = TimerState.IDLE
                onKeepAwake(false)
                heartRateManager.stopMonitoring()
                scope.launch {
                    preferencesManager.recordWorkout(
                        (currentRound - 1).coerceAtLeast(0),
                        workTime,
                        restTime
                    )
                }
                currentRound = 1
                timeLeft = workTime
                lastVibrationSecond = -1
            }
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
                                    onKeepAwake(false)
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
            if (!totalTimerRunning) {
                onKeepAwake(false)
            }
            heartRateManager.stopMonitoring()
        }
    }
    
    // Start workout
    fun startWorkout() {
        heartRateManager.resetAverage()
        workoutSession++
        totalTimeLeft = totalMinutes * 60
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
        totalTimeLeft = totalMinutes * 60
        vibrate(longArrayOf(0, 100, 50, 100))
        lastVibrationSecond = -1
        onKeepAwake(false)
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

    val totalWorkoutSeconds = (totalMinutes * 60).coerceAtLeast(1)
    val workoutProgress = (((totalWorkoutSeconds - totalTimeLeft).toFloat() / totalWorkoutSeconds) * 100)
        .coerceIn(0f, 100f)
    
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
                    
                    // Center - phase timer with total workout progress ring
                    Box(
                        modifier = Modifier.size(112.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 7.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(
                                (size.width - diameter) / 2,
                                (size.height - diameter) / 2
                            )
                            drawArc(
                                color = CardBackground,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = circleColor,
                                startAngle = -90f,
                                sweepAngle = 360f * (workoutProgress / 100f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                        }
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
                    // Start button - large and prominent
                    Button(
                        onClick = { startWorkout() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = CyanPrimary),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text(
                            text = "▶",
                            fontSize = 24.sp,
                            color = DarkBackground
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Settings, Presets, and Phone Control buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Settings button
                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground),
                            modifier = Modifier
                                .height(28.dp)
                                .width(50.dp)
                        ) {
                            Text(
                                text = "SET",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanPrimary
                            )
                        }
                        
                        // Presets button
                        Button(
                            onClick = onNavigateToPresets,
                            colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground),
                            modifier = Modifier
                                .height(28.dp)
                                .width(50.dp)
                        ) {
                            Text(
                                text = "PRE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeRest
                            )
                        }
                        
                        // Phone Control button
                        Button(
                            onClick = onNavigateToPhoneControl,
                            colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground),
                            modifier = Modifier
                                .height(28.dp)
                                .width(50.dp)
                        ) {
                            Text(
                                text = "📱",
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    // Active workout controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pause/Resume is centered on the left.
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { togglePause() },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (timerState == TimerState.PAUSED) GreenSuccess else YellowPause
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Text(
                                    text = if (timerState == TimerState.PAUSED) "▶" else "⏸",
                                    fontSize = 16.sp,
                                    color = DarkBackground
                                )
                            }
                        }

                        // Stop is centered between Pause and Skip.
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { stopWorkout() },
                                colors = ButtonDefaults.buttonColors(backgroundColor = RedStop),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text(
                                    text = "⏹",
                                    fontSize = 11.sp,
                                    color = TextWhite
                                )
                            }
                        }

                        // Skip/Next is centered on the right.
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (timerState != TimerState.PAUSED) {
                                Button(
                                    onClick = { skipPhase() },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = GreenSuccess),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(54.dp)
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
}
