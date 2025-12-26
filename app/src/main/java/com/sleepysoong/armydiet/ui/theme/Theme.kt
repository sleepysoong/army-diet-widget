package com.sleepysoong.armydiet.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark Green Theme Colors
object AppColors {
    val DarkGreen = Color(0xFF1B5E20)
    val DarkGreenLight = Color(0xFF2E7D32)
    val DarkGreenDark = Color(0xFF0D3311)
    val Surface = Color(0xFFFAFAFA)
    val SurfaceDark = Color(0xFF121212)
    val OnPrimary = Color.White
    val OnSurface = Color(0xFF1C1B1F)
    val OnSurfaceDark = Color(0xFFE6E1E5)
}

private val LightColorScheme = lightColorScheme(
    primary = AppColors.DarkGreen,
    onPrimary = AppColors.OnPrimary,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = AppColors.DarkGreenDark,
    secondary = AppColors.DarkGreenLight,
    onSecondary = AppColors.OnPrimary,
    background = AppColors.Surface,
    onBackground = AppColors.OnSurface,
    surface = AppColors.Surface,
    onSurface = AppColors.OnSurface,
    surfaceVariant = Color(0xFFE8F5E9),
    onSurfaceVariant = Color(0xFF424242)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = AppColors.DarkGreenDark,
    primaryContainer = AppColors.DarkGreen,
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFF81C784),
    onSecondary = AppColors.DarkGreenDark,
    background = AppColors.SurfaceDark,
    onBackground = AppColors.OnSurfaceDark,
    surface = Color(0xFF1E1E1E),
    onSurface = AppColors.OnSurfaceDark,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFBDBDBD)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
