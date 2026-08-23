package com.apksinc.monitor.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apksinc.monitor.ui.theme.ApkSincColors

/** Rotulo discreto de secao (ex.: "UNIDADES"). Mesmo estilo em todo o app. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.08.sp),
        color = ApkSincColors.colors.textMuted,
        modifier = modifier.padding(bottom = 4.dp),
    )
}
