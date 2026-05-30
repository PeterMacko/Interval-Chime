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
    primary = GeoTealDark,
    secondary = GeoTealLightDark,
    tertiary = GeoDialBgDark,
    background = GeoBgDark,
    surface = GeoDialBgDark,
    onPrimary = GeoBgDark,
    onSecondary = GeoTextDeepDark,
    onTertiary = GeoTextGrayDark,
    onBackground = GeoTextLight,
    onSurface = GeoTextDeepDark,
    surfaceVariant = GeoButtonGrayDark,
    onSurfaceVariant = GeoTextGrayDark,
    outline = GeoSubtleGrayDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GeoTeal,
    secondary = GeoTealLight,
    tertiary = GeoDialBg,
    background = GeoBg,
    surface = GeoDialBg,
    onPrimary = Color.White,
    onSecondary = GeoTextDeep,
    onTertiary = GeoTextGray,
    onBackground = GeoTextDark,
    onSurface = GeoTextDeep,
    surfaceVariant = GeoButtonGray,
    onSurfaceVariant = GeoTextGray,
    outline = GeoSubtleGray
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color override disabled to enforce Geometric Balance theme exactly
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
