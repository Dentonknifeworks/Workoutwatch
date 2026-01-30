package com.workouttimer.wear.data

data class WorkoutPreset(
    val id: String,
    val name: String,
    val workTime: Int,
    val restTime: Int,
    val rounds: Int
)

val defaultPresets = listOf(
    WorkoutPreset("1", "Quick HIIT", 20, 10, 8),
    WorkoutPreset("2", "Tabata", 20, 10, 8),
    WorkoutPreset("3", "Strength", 45, 15, 6),
    WorkoutPreset("4", "Cardio", 60, 20, 10)
)
