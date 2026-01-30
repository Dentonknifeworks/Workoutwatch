package com.workouttimer.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "workout_settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        val WORK_TIME = intPreferencesKey("work_time")
        val REST_TIME = intPreferencesKey("rest_time")
        val ROUNDS = intPreferencesKey("rounds")
        val TOTAL_WORKOUTS = intPreferencesKey("total_workouts")
        val TOTAL_ROUNDS = intPreferencesKey("total_rounds")
        val TOTAL_MINUTES = intPreferencesKey("total_minutes")
    }
    
    val workTime: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[WORK_TIME] ?: 40
    }
    
    val restTime: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[REST_TIME] ?: 60
    }
    
    val rounds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[ROUNDS] ?: 5
    }
    
    val totalWorkouts: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TOTAL_WORKOUTS] ?: 0
    }
    
    val totalRounds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TOTAL_ROUNDS] ?: 0
    }
    
    val totalMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TOTAL_MINUTES] ?: 0
    }
    
    suspend fun saveSettings(workTime: Int, restTime: Int, rounds: Int) {
        context.dataStore.edit { prefs ->
            prefs[WORK_TIME] = workTime
            prefs[REST_TIME] = restTime
            prefs[ROUNDS] = rounds
        }
    }
    
    suspend fun recordWorkout(roundsCompleted: Int, workTime: Int, restTime: Int) {
        context.dataStore.edit { prefs ->
            val currentWorkouts = prefs[TOTAL_WORKOUTS] ?: 0
            val currentRounds = prefs[TOTAL_ROUNDS] ?: 0
            val currentMinutes = prefs[TOTAL_MINUTES] ?: 0
            
            prefs[TOTAL_WORKOUTS] = currentWorkouts + 1
            prefs[TOTAL_ROUNDS] = currentRounds + roundsCompleted
            prefs[TOTAL_MINUTES] = currentMinutes + (roundsCompleted * (workTime + restTime) / 60)
        }
    }
    
    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs[TOTAL_WORKOUTS] = 0
            prefs[TOTAL_ROUNDS] = 0
            prefs[TOTAL_MINUTES] = 0
        }
    }
}
