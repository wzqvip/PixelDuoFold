package com.pixeldual.fold.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FoldColorScheme = darkColorScheme(
    primary = Color(0xFF8FD8FF),
    onPrimary = Color(0xFF003548),
    primaryContainer = Color(0xFF07506A),
    onPrimaryContainer = Color(0xFFBDEBFF),
    secondary = Color(0xFFFFC777),
    onSecondary = Color(0xFF452A00),
    secondaryContainer = Color(0xFF654000),
    onSecondaryContainer = Color(0xFFFFDDB2),
    tertiary = Color(0xFFD4B9FF),
    background = Color(0xFF0B1014),
    onBackground = Color(0xFFE1E8EC),
    surface = Color(0xFF111A20),
    onSurface = Color(0xFFE1E8EC),
    surfaceVariant = Color(0xFF26343D),
    onSurfaceVariant = Color(0xFFB7C7D0),
)

@Composable
fun PixelDualFoldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FoldColorScheme,
        typography = Typography(),
        content = content,
    )
}