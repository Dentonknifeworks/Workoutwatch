package com.workouttimer.wear.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HeartRateManager(private val context: Context) : SensorEventListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    
    private val _currentHeartRate = MutableStateFlow(0)
    val currentHeartRate: StateFlow<Int> = _currentHeartRate
    
    private val _averageHeartRate = MutableStateFlow(0)
    val averageHeartRate: StateFlow<Int> = _averageHeartRate
    
    private val heartRateReadings = mutableListOf<Int>()
    private var isMonitoring = false
    
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasSensor(): Boolean {
        return heartRateSensor != null
    }
    
    fun startMonitoring() {
        if (!isMonitoring && hasPermission() && hasSensor()) {
            heartRateSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                isMonitoring = true
            }
        }
    }
    
    fun stopMonitoring() {
        if (isMonitoring) {
            sensorManager.unregisterListener(this)
            isMonitoring = false
        }
    }
    
    fun resetAverage() {
        heartRateReadings.clear()
        _averageHeartRate.value = 0
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_HEART_RATE) {
                val heartRate = it.values[0].toInt()
                if (heartRate > 0) {
                    _currentHeartRate.value = heartRate
                    heartRateReadings.add(heartRate)
                    _averageHeartRate.value = heartRateReadings.average().toInt()
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}
