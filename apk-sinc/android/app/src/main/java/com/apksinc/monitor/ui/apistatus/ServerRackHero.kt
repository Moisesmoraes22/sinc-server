package com.apksinc.monitor.ui.apistatus

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.ui.theme.ApkSincColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Rack de servidor desenhado em projecao obliqua (frente + topo + lateral),
 * o que da profundidade real sem custo de um motor 3D: e so geometria em
 * Canvas, zero dependencia extra e zero asset no APK.
 *
 * As luzes piscam em fases diferentes (como equipamento real) e assumem a
 * cor do estado da API - verde quando conectada, vermelho quando nao.
 */
@Composable
fun ServerRackHero(isConnected: Boolean, modifier: Modifier = Modifier) {
    val colors = ApkSincColors.colors
    val transition = rememberInfiniteTransition(label = "rack")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2600, easing = LinearEasing)),
        label = "ledPhase",
    )

    val statusColor = if (isConnected) colors.success else colors.danger
    val frontFace = colors.elevated
    val topFace = lerp(frontFace, Color.White, 0.07f)
    val sideFace = lerp(frontFace, Color.Black, 0.42f)
    val slotColor = lerp(colors.card, Color.Black, 0.18f)
    val ventColor = lerp(slotColor, Color.White, 0.06f)

    Canvas(modifier = modifier.fillMaxWidth().height(184.dp)) {
        val rackW = 96.dp.toPx()
        val rackH = 132.dp.toPx()
        val depthX = 26.dp.toPx()
        val depthY = 15.dp.toPx()

        val ox = (size.width - (rackW + depthX)) / 2f
        val oy = (size.height - (rackH + depthY)) / 2f + depthY

        // Sombra no chao - ancora o objeto, evita aparencia de adesivo flutuante.
        drawOval(
            color = Color.Black.copy(alpha = 0.30f),
            topLeft = Offset(ox - 6.dp.toPx(), oy + rackH - 5.dp.toPx()),
            size = Size(rackW + depthX + 12.dp.toPx(), 16.dp.toPx()),
        )

        // Faces de profundidade primeiro (ficam atras da frente).
        drawQuad(
            topFace,
            Offset(ox, oy),
            Offset(ox + rackW, oy),
            Offset(ox + rackW + depthX, oy - depthY),
            Offset(ox + depthX, oy - depthY),
        )
        drawQuad(
            sideFace,
            Offset(ox + rackW, oy),
            Offset(ox + rackW + depthX, oy - depthY),
            Offset(ox + rackW + depthX, oy + rackH - depthY),
            Offset(ox + rackW, oy + rackH),
        )

        // Ranhuras de ventilacao na lateral, seguindo a inclinacao da face.
        repeat(3) { i ->
            val t = 0.30f + i * 0.16f
            val yTop = oy + rackH * t
            drawLine(
                color = Color.Black.copy(alpha = 0.22f),
                start = Offset(ox + rackW + 4.dp.toPx(), yTop - depthY * 0.16f),
                end = Offset(ox + rackW + depthX - 4.dp.toPx(), yTop - depthY * 0.84f),
                strokeWidth = 1.5.dp.toPx(),
            )
        }

        // Chassi (face frontal).
        drawRoundRect(
            color = frontFace,
            topLeft = Offset(ox, oy),
            size = Size(rackW, rackH),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )

        // Modulos empilhados.
        val slotCount = 6
        val padH = 7.dp.toPx()
        val padV = 8.dp.toPx()
        val gap = 3.dp.toPx()
        val slotH = (rackH - 2 * padV - (slotCount - 1) * gap) / slotCount
        val slotW = rackW - 2 * padH

        repeat(slotCount) { index ->
            val sx = ox + padH
            val sy = oy + padV + index * (slotH + gap)

            drawRoundRect(
                color = slotColor,
                topLeft = Offset(sx, sy),
                size = Size(slotW, slotH),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )

            // Brilho superior do modulo - sugere superficie metalica.
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(sx + 2.dp.toPx(), sy + 0.75.dp.toPx()),
                end = Offset(sx + slotW - 2.dp.toPx(), sy + 0.75.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )

            val midY = sy + slotH / 2f

            // LED de atividade: cada modulo pisca fora de fase com os outros.
            val ledPhase = (phase + index * 0.17f) % 1f
            val ledAlpha = 0.34f + 0.66f * (sin(ledPhase * 2f * PI.toFloat()) * 0.5f + 0.5f)
            drawCircle(
                color = statusColor.copy(alpha = ledAlpha),
                radius = 2.3.dp.toPx(),
                center = Offset(sx + 7.dp.toPx(), midY),
            )
            // Halo suave em volta do LED aceso.
            drawCircle(
                color = statusColor.copy(alpha = ledAlpha * 0.22f),
                radius = 4.6.dp.toPx(),
                center = Offset(sx + 7.dp.toPx(), midY),
            )
            // LED de energia, sempre fixo e discreto.
            drawCircle(
                color = colors.textMuted.copy(alpha = 0.55f),
                radius = 1.6.dp.toPx(),
                center = Offset(sx + 14.dp.toPx(), midY),
            )

            // Grelha de ventilacao a direita do modulo.
            val ventLeft = sx + 21.dp.toPx()
            val ventRight = sx + slotW - 5.dp.toPx()
            if (ventRight > ventLeft) {
                repeat(3) { line ->
                    val vy = midY + (line - 1) * 3.dp.toPx()
                    drawLine(
                        color = ventColor,
                        start = Offset(ventLeft, vy),
                        end = Offset(ventRight, vy),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
        }

        // Reflexo vertical sutil na frente do chassi.
        drawRoundRect(
            color = Color.White.copy(alpha = 0.03f),
            topLeft = Offset(ox, oy),
            size = Size(rackW * 0.22f, rackH),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )

        // Halo de status na base, refletindo no "chao".
        drawOval(
            color = statusColor.copy(alpha = 0.13f),
            topLeft = Offset(ox + rackW * 0.12f, oy + rackH - 9.dp.toPx()),
            size = Size(rackW * 0.76f, 14.dp.toPx()),
        )
    }
}

private fun DrawScope.drawQuad(color: Color, a: Offset, b: Offset, c: Offset, d: Offset) {
    val path = Path().apply {
        moveTo(a.x, a.y)
        lineTo(b.x, b.y)
        lineTo(c.x, c.y)
        lineTo(d.x, d.y)
        close()
    }
    drawPath(path, color)
}
