package com.example.expensetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF00A3FF),
    background = Color(0xFFF5F8FF),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF101828),
    onSurface = Color(0xFF101828)
)

@Composable
fun ExpenseTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}