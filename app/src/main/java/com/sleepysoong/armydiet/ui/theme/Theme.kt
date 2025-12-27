package com.sleepysoong.armydiet.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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

private val LightColorScheme = lightColorScheme(
    primary = ArmyColors.Primary,
    onPrimary = ArmyColors.OnPrimary,
    primaryContainer = ArmyColors.PrimaryContainer,
    onPrimaryContainer = ArmyColors.PrimaryDark,
    secondary = ArmyColors.PrimaryLight,
    onSecondary = ArmyColors.OnPrimary,
    background = ArmyColors.Background,
    onBackground = ArmyColors.OnBackground,
    surface = ArmyColors.Surface,
    onSurface = ArmyColors.OnSurface,
    surfaceVariant = ArmyColors.Background, // Use background color for variant in light mode
    onSurfaceVariant = ArmyColors.OnSurfaceVariant,
    outline = ArmyColors.OnSurfaceVariant.copy(alpha = 0.2f)
)

private val DarkColorScheme = darkColorScheme(
    primary = ArmyColors.PrimaryLight, // Lighter green for dark mode accessibility
    onPrimary = ArmyColors.PrimaryDark,
    primaryContainer = ArmyColors.PrimaryContainerDark,
    onPrimaryContainer = ArmyColors.OnPrimary,
    secondary = ArmyColors.PrimaryLight,
    onSecondary = ArmyColors.PrimaryDark,
    background = ArmyColors.BackgroundDark,
    onBackground = ArmyColors.OnBackgroundDark,
    surface = ArmyColors.SurfaceDark,
    onSurface = ArmyColors.OnSurfaceDark,
    surfaceVariant = ArmyColors.SurfaceDark,
    onSurfaceVariant = ArmyColors.OnSurfaceVariantDark,
    outline = ArmyColors.OnSurfaceVariantDark.copy(alpha = 0.2f)
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
            window.statusBarColor = colorScheme.background.toArgb() // Match background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
