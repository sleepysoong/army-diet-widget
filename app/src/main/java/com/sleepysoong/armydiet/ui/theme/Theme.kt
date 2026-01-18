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

// Pretendard Font Family (SF Pro 대체)
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)

// Apple-style Typography - 더 가볍고 깔끔하게
val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = 0.25.sp),
    displayMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 18.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 15.sp, letterSpacing = 0.sp),
    titleSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal, fontSize = 17.sp, letterSpacing = 0.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal, fontSize = 15.sp, letterSpacing = 0.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = 0.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 15.sp, letterSpacing = 0.sp),
    labelMedium = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.sp),
    labelSmall = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.sp)
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
    surfaceVariant = ArmyColors.SurfaceElevated,
    onSurfaceVariant = ArmyColors.OnSurfaceVariant,
    outline = ArmyColors.Divider,
    outlineVariant = ArmyColors.Divider.copy(alpha = 0.5f)
)

private val DarkColorScheme = darkColorScheme(
    primary = ArmyColors.HighlightDark,
    onPrimary = Color.Black,
    primaryContainer = ArmyColors.PrimaryContainerDark,
    onPrimaryContainer = ArmyColors.HighlightDark,
    secondary = ArmyColors.PrimaryLight,
    onSecondary = Color.Black,
    background = ArmyColors.BackgroundDark,
    onBackground = ArmyColors.OnBackgroundDark,
    surface = ArmyColors.SurfaceDark,
    onSurface = ArmyColors.OnSurfaceDark,
    surfaceVariant = ArmyColors.SurfaceElevatedDark,
    onSurfaceVariant = ArmyColors.OnSurfaceVariantDark,
    outline = ArmyColors.DividerDark,
    outlineVariant = ArmyColors.DividerDark.copy(alpha = 0.5f)
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
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
