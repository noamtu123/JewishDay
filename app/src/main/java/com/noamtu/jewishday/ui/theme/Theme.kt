// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noamtu.jewishday.data.AppThemeOption

private val LightColors = lightColorScheme(
    primary = Color(0xFF315B46),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDECE3),
    onPrimaryContainer = Color(0xFF0A2117),
    secondary = Color(0xFF635E4B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECE5D2),
    onSecondaryContainer = Color(0xFF201B0D),
    tertiary = Color(0xFF6B4E7A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0DBF7),
    onTertiaryContainer = Color(0xFF25112F),
    background = Color(0xFFFCFAF4),
    onBackground = Color(0xFF1B1D19),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF1B1D19),
    surfaceVariant = Color(0xFFE5E7DE),
    onSurfaceVariant = Color(0xFF5B6158),
    outline = Color(0xFF767D72),
    outlineVariant = Color(0xFFD7DAD0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAFCDBD),
    onPrimary = Color(0xFF183426),
    primaryContainer = Color(0xFF244B38),
    onPrimaryContainer = Color(0xFFDDECE3),
    secondary = Color(0xFFD0C7AF),
    onSecondary = Color(0xFF35301F),
    secondaryContainer = Color(0xFF4B4633),
    onSecondaryContainer = Color(0xFFECE5D2),
    tertiary = Color(0xFFDAB9E8),
    onTertiary = Color(0xFF3A2545),
    tertiaryContainer = Color(0xFF523B61),
    onTertiaryContainer = Color(0xFFF0DBF7),
    background = Color(0xFF111410),
    onBackground = Color(0xFFE6E4DC),
    surface = Color(0xFF171A16),
    onSurface = Color(0xFFE6E4DC),
    surfaceVariant = Color(0xFF42483F),
    onSurfaceVariant = Color(0xFFC5CBC0),
    outline = Color(0xFF8F978B),
    outlineVariant = Color(0xFF3E453C),
)

private val AmoledColors = darkColorScheme(
    primary = Color(0xFFAFCDBD),
    onPrimary = Color(0xFF103023),
    primaryContainer = Color(0xFF183D2C),
    onPrimaryContainer = Color(0xFFDDECE3),
    secondary = Color(0xFFD0C7AF),
    onSecondary = Color(0xFF2D2818),
    secondaryContainer = Color(0xFF393422),
    onSecondaryContainer = Color(0xFFECE5D2),
    tertiary = Color(0xFFDAB9E8),
    onTertiary = Color(0xFF32203C),
    tertiaryContainer = Color(0xFF432D50),
    onTertiaryContainer = Color(0xFFF0DBF7),
    background = Color.Black,
    onBackground = Color(0xFFEAEAE6),
    surface = Color.Black,
    onSurface = Color(0xFFEAEAE6),
    surfaceVariant = Color(0xFF181A18),
    onSurfaceVariant = Color(0xFFC7CBC4),
    outline = Color(0xFF8F978B),
    outlineVariant = Color(0xFF242A23),
)

private val BlueWhiteColors = lightColorScheme(
    primary = Color(0xFF2196F3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8ECFF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF1976D2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E3FF),
    onSecondaryContainer = Color(0xFF001B3E),
    tertiary = Color(0xFF0061A8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD3E4FF),
    onTertiaryContainer = Color(0xFF001C38),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF151C24),
    // Faint blue tint so InfoCards (surface) lift off the pure-white background.
    surface = Color(0xFFF1F6FE),
    onSurface = Color(0xFF151C24),
    surfaceVariant = Color(0xFFE2EDFB),
    onSurfaceVariant = Color(0xFF4F5F70),
    outline = Color(0xFF6F7F90),
    outlineVariant = Color(0xFFC9D8E8),
)

private val MidnightColors = darkColorScheme(
    primary = Color(0xFF9EC1FF),
    onPrimary = Color(0xFF052046),
    primaryContainer = Color(0xFF1B3A6B),
    onPrimaryContainer = Color(0xFFD7E3FF),
    secondary = Color(0xFFB6C5E8),
    onSecondary = Color(0xFF202F4B),
    secondaryContainer = Color(0xFF354563),
    onSecondaryContainer = Color(0xFFD9E3FF),
    tertiary = Color(0xFFCBBFE8),
    onTertiary = Color(0xFF322A4C),
    tertiaryContainer = Color(0xFF494063),
    onTertiaryContainer = Color(0xFFE8DDFF),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE2E6EE),
    surface = Color(0xFF141B26),
    onSurface = Color(0xFFE2E6EE),
    surfaceVariant = Color(0xFF3A4253),
    onSurfaceVariant = Color(0xFFC2C9D9),
    outline = Color(0xFF8B93A6),
    outlineVariant = Color(0xFF2A313E),
)

private val SandColors = lightColorScheme(
    primary = Color(0xFF8A5A2B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB8),
    onPrimaryContainer = Color(0xFF2E1500),
    secondary = Color(0xFF7A5A3A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC2),
    onSecondaryContainer = Color(0xFF2B1709),
    tertiary = Color(0xFF5E6135),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE3E6AE),
    onTertiaryContainer = Color(0xFF1B1D00),
    background = Color(0xFFFFF8F1),
    onBackground = Color(0xFF221A12),
    surface = Color(0xFFFBF0E4),
    onSurface = Color(0xFF221A12),
    surfaceVariant = Color(0xFFF1E0CE),
    onSurfaceVariant = Color(0xFF524435),
    outline = Color(0xFF85735E),
    outlineVariant = Color(0xFFD7C3AE),
)

private val SlateColors = darkColorScheme(
    primary = Color(0xFFA9C7D8),
    onPrimary = Color(0xFF0E2A36),
    primaryContainer = Color(0xFF2A404C),
    onPrimaryContainer = Color(0xFFCDE6F5),
    secondary = Color(0xFFB7C4CC),
    onSecondary = Color(0xFF222E34),
    secondaryContainer = Color(0xFF38444B),
    onSecondaryContainer = Color(0xFFD3E0E8),
    tertiary = Color(0xFFC7BFD6),
    onTertiary = Color(0xFF2E283B),
    tertiaryContainer = Color(0xFF443E52),
    onTertiaryContainer = Color(0xFFE4DBF2),
    background = Color(0xFF15181B),
    onBackground = Color(0xFFE0E3E6),
    surface = Color(0xFF1C2024),
    onSurface = Color(0xFFE0E3E6),
    surfaceVariant = Color(0xFF3D434A),
    onSurfaceVariant = Color(0xFFC3C8CE),
    outline = Color(0xFF8D9298),
    outlineVariant = Color(0xFF2C3136),
)

private val JerusalemStoneColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E8FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF8A6F38),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE9B1),
    onSecondaryContainer = Color(0xFF2A1F00),
    tertiary = Color(0xFF006C8F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC5EFFF),
    onTertiaryContainer = Color(0xFF001F2D),
    background = Color(0xFFFFFBF2),
    onBackground = Color(0xFF1F1B13),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF1F1B13),
    surfaceVariant = Color(0xFFF2E7D0),
    onSurfaceVariant = Color(0xFF625B4B),
    outline = Color(0xFF817867),
    outlineVariant = Color(0xFFE0D5BE),
)

private val BaseTypography = Typography()

private val AppTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-1.6).sp,
    ),
    displayMedium = BaseTypography.displayMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-1.2).sp,
    ),
    displaySmall = BaseTypography.displaySmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.8).sp,
    ),
    headlineLarge = BaseTypography.headlineLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = BaseTypography.headlineMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.Medium),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun JewishDayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeOption: AppThemeOption = AppThemeOption.Classic,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        themeOption != AppThemeOption.Classic -> staticColorScheme(themeOption = themeOption, darkTheme = darkTheme)

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> staticColorScheme(themeOption = themeOption, darkTheme = darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

fun appThemeBackgroundColor(themeOption: AppThemeOption, darkTheme: Boolean): Int =
    staticColorScheme(themeOption = themeOption, darkTheme = darkTheme).background.toArgb()

private fun staticColorScheme(themeOption: AppThemeOption, darkTheme: Boolean): ColorScheme = when (themeOption) {
    AppThemeOption.AmoledBlack -> AmoledColors
    AppThemeOption.BlueWhite -> BlueWhiteColors
    AppThemeOption.JerusalemStone -> JerusalemStoneColors
    AppThemeOption.Sand -> SandColors
    AppThemeOption.Midnight -> MidnightColors
    AppThemeOption.Slate -> SlateColors
    AppThemeOption.Classic -> if (darkTheme) DarkColors else LightColors
}