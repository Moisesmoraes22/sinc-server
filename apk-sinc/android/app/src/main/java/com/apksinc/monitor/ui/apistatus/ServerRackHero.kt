package com.apksinc.monitor.ui.apistatus

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.ui.theme.ApkSincColors

/**
 * Representacao 3D leve de um rack de servidor, feita 100% com Compose
 * nativo (perspectiva real via rotationY + cameraDistance, geometria
 * desenhada em Canvas) - sem modelo .glb, sem biblioteca de renderizacao
 * extra. Decorativa, usada so na tela de Status da API.
 */
@Composable
fun ServerRackHero(isConnected: Boolean, modifier: Modifier = Modifier) {
    val colors = ApkSincColors.colors
    val infiniteTransition = rememberInfiniteTransition(label = "server_rack")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rotationY",
    )
    val ledColor = if (isConnected) colors.success else colors.danger

    Box(modifier = modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .height(120.dp)
                .padding(horizontal = 56.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 24f * density
                }
                .clip(RoundedCornerShape(14.dp))
                .background(colors.elevated),
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).padding(10.dp)) {
                val slotCount = 4
                val slotHeight = size.height / slotCount
                repeat(slotCount) { index ->
                    val top = index * slotHeight
                    drawRoundRect(
                        color = colors.card,
                        topLeft = Offset(0f, top + 3f),
                        size = Size(size.width, slotHeight - 6f),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                    drawCircle(
                        color = if (index == 0) ledColor else colors.textMuted,
                        radius = 4f,
                        center = Offset(14f, top + slotHeight / 2f),
                    )
                    drawRoundRect(
                        color = colors.hairline,
                        topLeft = Offset(28f, top + slotHeight / 2f - 2f),
                        size = Size(size.width - 44f, 4f),
                        cornerRadius = CornerRadius(2f, 2f),
                    )
                }
            }
        }
    }
}
