package com.workouttimer.wear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class PhoneCommunicator(private val context: Context) {
    
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val TAG = "PhoneCommunicator"
    
    companion object {
        const val SPEAK_PATH = "/speak"
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
    
    suspend fun isPhoneConnected(): Boolean {
        return try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
