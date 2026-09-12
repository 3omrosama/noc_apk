package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NocDarkColorScheme = darkColorScheme(
    primary = NocCyan,
    onPrimary = NocBackground,
    primaryContainer = NocSurfaceVariant,
    onPrimaryContainer = NocCyan,
    secondary = NocBlue,
    onSecondary = NocBackground,
    secondaryContainer = NocSurfaceVariant,
    onSecondaryContainer = NocBlue,
    tertiary = NocIndigo,
    onTertiary = NocBackground,
    background = NocBackground,
    onBackground = NocTextPrimary,
    surface = NocSurface,
    onSurface = NocTextPrimary,
    surfaceVariant = NocSurfaceVariant,
    onSurfaceVariant = NocTextSecondary,
    outline = NocSurfaceHighlight,
    outlineVariant = NocSurfaceHover,
    error = NocRed,
    onError = NocTextPrimary,
    errorContainer = NocRedBg,
    onErrorContainer = NocRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // NOC NOCs always default to dark theme
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NocDarkColorScheme,
        typography = Typography,
        content = content
    )
}

