package com.turel.jewishdaynext.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turel.jewishdaynext.data.AppThemeOption

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
    background = Color.White,
    onBackground = Color(0xFF151C24),
    surface = Color.White,
    onSurface = Color(0xFF151C24),
    surfaceVariant = Color(0xFFE8F2FF),
    onSurfaceVariant = Color(0xFF4F5F70),
    outline = Color(0xFF6F7F90),
    outlineVariant = Color(0xFFC9D8E8),
)

private val IsraelSkyColors = lightColorScheme(
    primary = Color(0xFF0D47A1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF4A6FA5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E8FF),
    onSecondaryContainer = Color(0xFF061B33),
    tertiary = Color(0xFF0087C7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC9EEFF),
    onTertiaryContainer = Color(0xFF001F2D),
    background = Color(0xFFF8FBFF),
    onBackground = Color(0xFF101923),
    surface = Color.White,
    onSurface = Color(0xFF101923),
    surfaceVariant = Color(0xFFE4F1FF),
    onSurfaceVariant = Color(0xFF486071),
    outline = Color(0xFF6D8092),
    outlineVariant = Color(0xFFC8D8E8),
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
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
)

@Composable
fun JewishDayNextTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeOption: AppThemeOption = AppThemeOption.Classic,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        themeOption == AppThemeOption.AmoledBlack -> AmoledColors
        themeOption == AppThemeOption.BlueWhite -> BlueWhiteColors
        themeOption == AppThemeOption.IsraelSky -> IsraelSkyColors
        themeOption == AppThemeOption.JerusalemStone -> JerusalemStoneColors

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
