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
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.workouttimer.wear.data.PreferencesManager
import com.workouttimer.wear.data.WorkoutPreset
import com.workouttimer.wear.data.defaultPresets
import com.workouttimer.wear.presentation.theme.*
import kotlinx.coroutines.launch

@Composable
fun PresetsScreen(
    preferencesManager: PreferencesManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    
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
                    text = "Presets",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Preset items
            items(defaultPresets) { preset ->
                PresetCard(
                    preset = preset,
                    onSelect = {
                        scope.launch {
                            preferencesManager.saveSettings(
                                preset.workTime,
                                preset.restTime,
                                preset.rounds
                            )
                            onBack()
                        }
                    }
                )
            }
            
            // Back button
            item {
                Spacer(modifier = Modifier.height(8.dp))
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
fun PresetCard(
    preset: WorkoutPreset,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = CardBackground,
            endBackgroundColor = CardBackground
        ),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preset name
            Text(
                text = preset.name,
                color = CyanPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Details
            Text(
                text = "${preset.workTime}s / ${preset.restTime}s × ${preset.rounds}",
                color = TextGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
