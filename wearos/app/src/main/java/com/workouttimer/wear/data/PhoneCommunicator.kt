package com.workouttimer.wear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class PhoneCommunicator(private val context: Context) {
    
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val TAG = "PhoneCommunicator"
    
    companion object {
        const val SPEAK_PATH = "/speak"
        const val TIMER_CONTROL_PATH = "/timer_control"
        
        // Timer commands
        const val CMD_START = "start"
        const val CMD_PAUSE = "pause"
        const val CMD_STOP = "stop"
        const val CMD_SKIP = "skip"
    }
    
    suspend fun sendSpeakMessage(text: String): Boolean {
        return try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.d(TAG, "No connected phone found")
                false
            } else {
                var success = false
                for (node in nodes) {
                    try {
                        messageClient.sendMessage(
                            node.id,
                            SPEAK_PATH,
                            text.toByteArray()
                        ).await()
                        Log.d(TAG, "Message sent to ${node.displayName}: $text")
                        success = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send to ${node.displayName}: ${e.message}")
                    }
                }
                success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}")
            false
        }
    }
    
    /**
     * Send a timer control command to the phone app
     * @param command One of: start, pause, stop, skip
     * @param payload Optional JSON payload with additional data
     */
    suspend fun sendTimerCommand(command: String, payload: JSONObject? = null): Boolean {
        return try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.d(TAG, "No connected phone found for timer command")
                false
            } else {
                val message = JSONObject().apply {
                    put("command", command)
                    put("timestamp", System.currentTimeMillis())
                    if (payload != null) {
                        put("payload", payload)
                    }
                }
                
                var success = false
                for (node in nodes) {
                    try {
                        messageClient.sendMessage(
                            node.id,
                            TIMER_CONTROL_PATH,
                            message.toString().toByteArray(Charsets.UTF_8)
                        ).await()
                        Log.d(TAG, "Timer command '$command' sent to ${node.displayName}")
                        success = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send timer command to ${node.displayName}: ${e.message}")
                    }
                }
                success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending timer command: ${e.message}")
            false
        }
    }
    
    // Convenience methods for common timer commands
    suspend fun sendStart(): Boolean = sendTimerCommand(CMD_START)
    suspend fun sendPause(): Boolean = sendTimerCommand(CMD_PAUSE)
    suspend fun sendStop(): Boolean = sendTimerCommand(CMD_STOP)
    suspend fun sendSkip(): Boolean = sendTimerCommand(CMD_SKIP)
    
    suspend fun isPhoneConnected(): Boolean {
        return try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
