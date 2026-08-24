package com.apksinc.monitor.ui.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.ui.theme.ApkSincColors

/**
 * Placeholder com o formato do ServerCard, usado enquanto os dados ainda nao
 * chegaram - da a sensacao de "o conteudo ja tem forma, so esta carregando"
 * em vez de um spinner generico que nao informa nada sobre o que vem a
 * seguir.
 */
@Composable
fun SkeletonServerCard(modifier: Modifier = Modifier) {
    val colors = ApkSincColors.colors
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    val blockColor = colors.hairline.copy(alpha = alpha)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Box(width = 120.dp, height = 16.dp, color = blockColor)
                    Box(width = 80.dp, height = 12.dp, color = blockColor, top = 6.dp)
                }
                Box(width = 64.dp, height = 22.dp, color = blockColor, shape = RoundedCornerShape(50))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Box(width = 70.dp, height = 10.dp, color = blockColor)
                    Box(width = 90.dp, height = 14.dp, color = blockColor, top = 6.dp)
                }
                Column {
                    Box(width = 70.dp, height = 10.dp, color = blockColor)
                    Box(width = 60.dp, height = 14.dp, color = blockColor, top = 6.dp)
                }
            }
        }
    }
}

@Composable
private fun Box(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    color: androidx.compose.ui.graphics.Color,
    top: androidx.compose.ui.unit.Dp = 0.dp,
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .padding(top = top)
            .width(width)
            .height(height)
            .clip(shape)
            .background(color),
    )
}
