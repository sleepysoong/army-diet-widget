package com.sleepysoong.armydiet.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.sleepysoong.armydiet.R

// Pretendard Font Family
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)

// Typography with Pretendard
val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

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
    
    // Tag/Chip colors
    val TagBg = Color(0xFFE8E8E8)
    val TagBgDark = Color(0xFF3D3D3D)
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
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF424242),
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFEEEEEE)
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
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF424242),
    outlineVariant = Color(0xFF363636)
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
        typography = AppTypography,
        content = content
    )
}
