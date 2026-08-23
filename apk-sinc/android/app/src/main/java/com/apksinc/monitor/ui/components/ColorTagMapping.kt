package com.apksinc.monitor.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.apksinc.monitor.domain.ColorTag
import com.apksinc.monitor.ui.theme.OmgSincTheme

/** Unico lugar que traduz [ColorTag] (dado do dominio) em cor de UI -
 * telas nunca decidem cor "na mao", sempre passam por aqui. */
@Composable
fun ColorTag.toColor(): Color = when (this) {
    ColorTag.ACCENT -> OmgSincTheme.colors.accent
    ColorTag.SUCCESS -> OmgSincTheme.colors.success
    ColorTag.WARNING -> OmgSincTheme.colors.warning
}

@Composable
fun ColorTag.toSoftColor(): Color = when (this) {
    ColorTag.ACCENT -> OmgSincTheme.colors.accentSoft
    ColorTag.SUCCESS -> OmgSincTheme.colors.successSoft
    ColorTag.WARNING -> OmgSincTheme.colors.warningSoft
}
