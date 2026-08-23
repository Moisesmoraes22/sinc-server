package com.apksinc.monitor.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Tokens de design que o ColorScheme do Material3 nao modela nativamente
 * (escala de elevacao por luminosidade, tom "muted", variantes "soft" das
 * cores funcionais). Telas devem ler cores daqui (`ApkSincColors.colors.*`),
 * nao criar `Color(0x...)` soltos.
 */
data class AppColors(
    val background: Color,
    val elevated: Color,
    val card: Color,
    val interactive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val hairline: Color,
    val accent: Color,
    val accentSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
)

private val DarkExtendedColors = AppColors(
    background = Background, elevated = Elevated, card = Card, interactive = Interactive,
    textPrimary = TextPrimary, textSecondary = TextSecondary, textMuted = TextMuted, hairline = Hairline,
    accent = Accent, accentSoft = AccentSoft,
    success = Success, successSoft = SuccessSoft,
    warning = Warning, warningSoft = WarningSoft,
    danger = Danger, dangerSoft = DangerSoft,
)

private val LightExtendedColors = AppColors(
    background = BackgroundLight, elevated = ElevatedLight, card = CardLight, interactive = ElevatedLight,
    textPrimary = TextPrimaryLight, textSecondary = TextSecondaryLight, textMuted = TextMutedLight, hairline = HairlineLight,
    accent = Accent, accentSoft = AccentSoft,
    success = Success, successSoft = SuccessSoft,
    warning = Warning, warningSoft = WarningSoft,
    danger = Danger, dangerSoft = DangerSoft,
)

private val LocalAppColors = staticCompositionLocalOf { DarkExtendedColors }

private val DarkColors = darkColorScheme(
    primary = Accent, background = Background, surface = Elevated, surfaceVariant = Card,
    onBackground = TextPrimary, onSurface = TextPrimary, onSurfaceVariant = TextSecondary, error = Danger,
)

private val LightColors = lightColorScheme(
    primary = Accent, background = BackgroundLight, surface = ElevatedLight, surfaceVariant = CardLight,
    onBackground = TextPrimaryLight, onSurface = TextPrimaryLight, onSurfaceVariant = TextSecondaryLight, error = Danger,
)

object ApkSincColors {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current
}

@Composable
fun ApkSincTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SincTypography,
            content = content,
        )
    }
}
