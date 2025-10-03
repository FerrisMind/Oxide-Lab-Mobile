package com.oxidelabmobile.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Legacy colors for fallback
@Suppress("unused")
val Purple80 = Color(0xFFD0BCFF)
@Suppress("unused")
val PurpleGrey80 = Color(0xFFCCC2DC)
@Suppress("unused")
val Pink80 = Color(0xFFEFB8C8)

@Suppress("unused")
val Purple40 = Color(0xFF6650a4)
@Suppress("unused")
val PurpleGrey40 = Color(0xFF625b71)
@Suppress("unused")
val Pink40 = Color(0xFF7D5260)

// Oxide Lab Brand Colors
val OxidePrimary = Color(0xFF1B1C3A)
val OxideSecondary = Color(0xFF252643)
val OxideAccent = Color(0xFFFF6D34)
val OxideAccentEnd = Color(0xFFF7911F)

// Status Colors
@Suppress("unused")
val StatusSuccess = Color(0xFF4CAF50)
@Suppress("unused")
val StatusWarning = Color(0xFFFF9800)
val StatusError = Color(0xFFF44336)
@Suppress("unused")
val StatusInfo = Color(0xFF2196F3)

// Text Colors
val TextOnDark = Color(0xFFCDCDCE)
val TextOnLight = Color(0xFF2C2C2C)
@Suppress("unused")
val ButtonTextDark = Color(0xFF1B1C3A)

// Background Colors
val BackgroundDark = Color(0xFF1B1C3A)
val BackgroundDarkCenter = Color(0xFF252643)
val BackgroundDarkEnd = Color(0xFF252643)

// Material 3 Color Schemes
val DarkColorScheme = darkColorScheme(
    primary = OxideAccent,
    onPrimary = Color.White,
    primaryContainer = OxideAccent.copy(alpha = 0.12f),
    onPrimaryContainer = OxideAccent,
    secondary = OxideSecondary,
    onSecondary = Color.White,
    tertiary = OxideAccentEnd,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = TextOnDark,
    surface = BackgroundDarkCenter,
    onSurface = TextOnDark,
    surfaceVariant = BackgroundDarkEnd,
    onSurfaceVariant = TextOnDark.copy(alpha = 0.7f),
    outline = TextOnDark.copy(alpha = 0.3f),
    error = StatusError,
    onError = Color.White
)

val LightColorScheme = lightColorScheme(
    primary = OxidePrimary,
    onPrimary = Color.White,
    primaryContainer = OxidePrimary.copy(alpha = 0.12f),
    onPrimaryContainer = OxidePrimary,
    secondary = OxideSecondary,
    onSecondary = Color.White,
    tertiary = OxideAccent,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = TextOnLight,
    surface = Color(0xFFFAFAFA),
    onSurface = TextOnLight,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = TextOnLight.copy(alpha = 0.7f),
    outline = TextOnLight.copy(alpha = 0.3f),
    error = StatusError,
    onError = Color.White
)
