package com.lenzbeyer.hathor.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Ink: Color = Color(0xFF000000)
val Paper: Color = Color(0xFFFFFFFF)

val LightColors = lightColorScheme(
    primary             = Ink,
    onPrimary           = Paper,
    primaryContainer    = Ink,
    onPrimaryContainer  = Paper,
    secondary           = Ink,
    onSecondary         = Paper,
    secondaryContainer  = Paper,
    onSecondaryContainer= Ink,
    tertiary            = Ink,
    onTertiary          = Paper,
    background          = Paper,
    onBackground        = Ink,
    surface             = Paper,
    onSurface           = Ink,
    surfaceVariant      = Paper,
    onSurfaceVariant    = Ink.copy(alpha = 0.6f),
    surfaceTint         = Paper,        // disable tonal elevation tinting
    inverseSurface      = Ink,
    inverseOnSurface    = Paper,
    inversePrimary      = Paper,
    outline             = Ink,
    outlineVariant      = Ink.copy(alpha = 0.24f),
    error               = Ink,
    onError             = Paper,
    errorContainer      = Paper,
    onErrorContainer    = Ink,
    scrim               = Ink.copy(alpha = 0.32f),
)

val DarkColors = darkColorScheme(
    primary             = Paper,
    onPrimary           = Ink,
    primaryContainer    = Paper,
    onPrimaryContainer  = Ink,
    secondary           = Paper,
    onSecondary         = Ink,
    secondaryContainer  = Ink,
    onSecondaryContainer= Paper,
    tertiary            = Paper,
    onTertiary          = Ink,
    background          = Ink,
    onBackground        = Paper,
    surface             = Ink,
    onSurface           = Paper,
    surfaceVariant      = Ink,
    onSurfaceVariant    = Paper.copy(alpha = 0.6f),
    surfaceTint         = Ink,
    inverseSurface      = Paper,
    inverseOnSurface    = Ink,
    inversePrimary      = Ink,
    outline             = Paper,
    outlineVariant      = Paper.copy(alpha = 0.24f),
    error               = Paper,
    onError             = Ink,
    errorContainer      = Ink,
    onErrorContainer    = Paper,
    scrim               = Paper.copy(alpha = 0.32f),
)
