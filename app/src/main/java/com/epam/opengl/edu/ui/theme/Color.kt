package com.epam.opengl.edu.ui.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

val DarkThemeColors = darkColors(
    primary = Color.White,
    secondary = Color(0xFFFFFFFF),
    background = Color.Black,
    surface = Color(4, 4, 6),
    error = Color(0xFFF70040),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

val LightThemeColors = lightColors(
    primary = Color(0xFF283593),
    primaryVariant = Color(0xFF5f5fc4),
    secondary = Color(0xFF1565c0),
    secondaryVariant = Color(0xFF5e92f3),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    error = Color(0xFFD00036),
)