package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFD0BCFF), // Cyber Purple
    primaryContainer = Color(0xFF381E72),
    secondary = Color(0xFF2B2930), // Slate Core Carbon
    tertiary = Color(0xFFE8DEF8), // Gentle Lavender Accent
    background = Color(0xFF1C1B1F), // Elegant Slate Black
    surface = Color(0xFF2B2930), // Smooth Core Slate
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFCAC4D0),
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFFFFFFFF)
  )

private val LightColorScheme = DarkColorScheme // Forced Dark theme for developers feel


@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+ (force false here to make elegant dark standard)
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
