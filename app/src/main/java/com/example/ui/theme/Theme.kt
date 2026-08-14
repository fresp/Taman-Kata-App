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

private val DarkColorScheme =
  darkColorScheme(primary = PrimaryGreen, secondary = WarmOrange, tertiary = SoftYellow)

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryGreen,
    secondary = WarmOrange,
    tertiary = SoftYellow,
    background = LightBackground,
    surface = LightBackground,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = TextDark,
    onSurface = TextDark
  )

@Composable
fun TamanKataTheme(
  darkTheme: Boolean = false, // Force light theme for this vibrant design
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
