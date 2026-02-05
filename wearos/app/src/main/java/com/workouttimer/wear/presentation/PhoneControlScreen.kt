package com.workouttimer.wear.presentation

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
import com.workouttimer.wear.data.PhoneCommunicator
import com.workouttimer.wear.presentation.theme.*
import kotlinx.coroutines.launch

@Composable
fun PhoneControlScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val phoneCommunicator = remember { PhoneCommunicator(context) }
    var phoneConnected by remember { mutableStateOf(false) }
    var lastCommandStatus by remember { mutableStateOf("Ready") }
    var isSending by remember { mutableStateOf(false) }
    
    // Check phone connection on launch
    LaunchedEffect(Unit) {
        phoneConnected = phoneCommunicator.isPhoneConnected()
    }
    
    // Send command helper
    fun sendCommand(command: suspend () -> Boolean, commandName: String) {
        if (isSending) return
        isSending = true
        lastCommandStatus = "Sending..."
        
        scope.launch {
            val success = command()
            lastCommandStatus = if (success) "$commandName ✓" else "Failed ✗"
            isSending = false
            
            // Refresh connection status
            phoneConnected = phoneCommunicator.isPhoneConnected()
        }
    }
    
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
                // Title and connection status
                Text(
                    text = "Phone Control",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Connection indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "●",
                        color = if (phoneConnected) GreenSuccess else RedStop,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (phoneConnected) "Connected" else "Disconnected",
                        color = TextGray,
                        fontSize = 10.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Status message
                Text(
                    text = lastCommandStatus,
                    color = when {
                        lastCommandStatus.contains("✓") -> GreenSuccess
                        lastCommandStatus.contains("✗") -> RedStop
                        else -> YellowPause
                    },
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Control buttons - 2x2 grid
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: Start and Pause
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start button
                        Button(
                            onClick = {
                                sendCommand(
                                    { phoneCommunicator.sendStart() },
                                    "Start"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = GreenSuccess),
                            modifier = Modifier.size(52.dp),
                            enabled = phoneConnected && !isSending
                        ) {
                            Text(
                                text = "▶",
                                fontSize = 18.sp,
                                color = DarkBackground
                            )
                        }
                        
                        // Pause button
                        Button(
                            onClick = {
                                sendCommand(
                                    { phoneCommunicator.sendPause() },
                                    "Pause"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = YellowPause),
                            modifier = Modifier.size(52.dp),
                            enabled = phoneConnected && !isSending
                        ) {
                            Text(
                                text = "⏸",
                                fontSize = 18.sp,
                                color = DarkBackground
                            )
                        }
                    }
                    
                    // Row 2: Stop and Skip
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stop button
                        Button(
                            onClick = {
                                sendCommand(
                                    { phoneCommunicator.sendStop() },
                                    "Stop"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = RedStop),
                            modifier = Modifier.size(52.dp),
                            enabled = phoneConnected && !isSending
                        ) {
                            Text(
                                text = "⏹",
                                fontSize = 18.sp,
                                color = TextWhite
                            )
                        }
                        
                        // Skip button
                        Button(
                            onClick = {
                                sendCommand(
                                    { phoneCommunicator.sendSkip() },
                                    "Skip"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = CyanPrimary),
                            modifier = Modifier.size(52.dp),
                            enabled = phoneConnected && !isSending
                        ) {
                            Text(
                                text = "⏭",
                                fontSize = 18.sp,
                                color = DarkBackground
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Back button
                CompactButton(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(backgroundColor = CardBackground)
                ) {
                    Text(
                        text = "← Back",
                        fontSize = 10.sp,
                        color = TextGray
                    )
                }
            }
        }
    }
}
