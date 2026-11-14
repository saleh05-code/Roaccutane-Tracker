package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = GeometricLavender,
    secondary = GeometricSkyBlue,
    tertiary = GeometricGreenSeed,
    background = GeometricBackground,
    surface = GeometricCardBackground,
    surfaceVariant = GeometricInnerBackground,
    outline = GeometricBorder,
    outlineVariant = GeometricBorder,
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFF003354),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFE1E2E6),
    onSurface = Color(0xFFE1E2E6),
    onSurfaceVariant = Color(0xFF938F99)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = Color(0xFF49454F)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
