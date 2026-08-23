package com.apksinc.monitor.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apksinc.monitor.ui.theme.OmgSincTheme

/** Rotulo discreto de secao (ex.: "HOJE", "HABITOS"). Sempre o mesmo estilo
 * em todo o app - consistencia de hierarquia tipografica. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.08.sp),
        fontWeight = FontWeight.Bold,
        color = OmgSincTheme.colors.textMuted,
        modifier = modifier.padding(bottom = 8.dp),
    )
}
