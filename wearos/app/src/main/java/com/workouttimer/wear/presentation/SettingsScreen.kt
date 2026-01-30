package com.workouttimer.wear.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.workouttimer.wear.data.PreferencesManager
import com.workouttimer.wear.presentation.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    
    // Current settings
    val currentWorkTime by preferencesManager.workTime.collectAsState(initial = 40)
    val currentRestTime by preferencesManager.restTime.collectAsState(initial = 60)
    val currentRounds by preferencesManager.rounds.collectAsState(initial = 5)
    
    // Editable values
    var workTime by remember { mutableIntStateOf(currentWorkTime) }
    var restTime by remember { mutableIntStateOf(currentRestTime) }
    var rounds by remember { mutableIntStateOf(currentRounds) }
    
    // Update when loaded
    LaunchedEffect(currentWorkTime, currentRestTime, currentRounds) {
        workTime = currentWorkTime
        restTime = currentRestTime
        rounds = currentRounds
    }
    
    Scaffold(
        timeText = { TimeText() },
        positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            item {
                Text(
                    text = "Settings",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Work Time
            item {
                SettingItem(
                    label = "Work Time",
                    value = workTime,
                    unit = "sec",
                    onDecrease = { if (workTime > 5) workTime -= 5 },
                    onIncrease = { if (workTime < 300) workTime += 5 }
                )
            }
            
            // Rest Time
            item {
                SettingItem(
                    label = "Rest Time",
                    value = restTime,
                    unit = "sec",
                    onDecrease = { if (restTime > 5) restTime -= 5 },
                    onIncrease = { if (restTime < 300) restTime += 5 }
                )
            }
            
            // Rounds
            item {
                SettingItem(
                    label = "Rounds",
                    value = rounds,
                    unit = "",
                    onDecrease = { if (rounds > 1) rounds-- },
                    onIncrease = { if (rounds < 30) rounds++ }
                )
            }
            
            // Save button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            preferencesManager.saveSettings(workTime, restTime, rounds)
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = CyanPrimary),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(40.dp)
                ) {
                    Text(
                        text = "Save",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Back button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                CompactButton(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground)
                ) {
                    Text("← Back", color = TextGray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SettingItem(
    label: String,
    value: Int,
    unit: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = TextGray,
            fontSize = 12.sp
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decrease button
            CompactButton(
                onClick = onDecrease,
                colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground)
            ) {
                Text("-", color = TextWhite, fontSize = 18.sp)
            }
            
            // Value
            Text(
                text = if (unit.isNotEmpty()) "$value $unit" else value.toString(),
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(70.dp)
            )
            
            // Increase button
            CompactButton(
                onClick = onIncrease,
                colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground)
            ) {
                Text("+", color = TextWhite, fontSize = 18.sp)
            }
        }
    }
}
