package com.example.link.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = GuardianAction,
    onPrimary = GuardianSurface,
    primaryContainer = GuardianPrimarySoft,
    onPrimaryContainer = GuardianTextPrimary,
    secondary = GuardianPrimary,
    onSecondary = GuardianSurface,
    secondaryContainer = GuardianCream,
    onSecondaryContainer = GuardianTextPrimary,
    error = GuardianDanger,
    onError = GuardianSurface,
    background = GuardianBackground,
    onBackground = GuardianTextPrimary,
    surface = GuardianSurface,
    onSurface = GuardianTextPrimary,
    surfaceVariant = GuardianCream,
    onSurfaceVariant = GuardianTextSecondary,
    outline = GuardianBorder
)

@Composable
fun LinkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
