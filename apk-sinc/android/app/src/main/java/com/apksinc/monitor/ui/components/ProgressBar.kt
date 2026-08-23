package com.apksinc.monitor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.ui.theme.OmgSincTheme

/** Barra de progresso linear com preenchimento animado - usada em cards de
 * habito e na Home. Motion sutil: so anima a largura, sem rebote/pulo. */
@Composable
fun LinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = OmgSincTheme.colors.accent,
    trackColor: Color = OmgSincTheme.colors.interactive,
    height: androidx.compose.ui.unit.Dp = 6.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 420),
        label = "progress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}
