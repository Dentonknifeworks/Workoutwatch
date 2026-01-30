package com.workouttimer.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Colors

private val WearColorPalette = Colors(
    primary = CyanPrimary,
    primaryVariant = CyanPrimary,
    secondary = OrangeRest,
    secondaryVariant = OrangeRest,
    error = RedStop,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onError = TextWhite,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = CardBackground,
    onSurface = TextWhite
)

@Composable
fun WorkoutTimerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WearColorPalette,
        content = content
    )
}
