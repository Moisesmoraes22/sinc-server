package com.apksinc.monitor.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.domain.Habit
import com.apksinc.monitor.ui.theme.OmgSincTheme

@Composable
fun HabitRow(
    habit: Habit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tagColor = habit.colorTag.toColor()
    val tagSoft = habit.colorTag.toSoftColor()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OmgSincTheme.colors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tagSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = habit.category.icon(),
                    contentDescription = null,
                    tint = tagColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = OmgSincTheme.colors.textPrimary,
                )
                val subtitle = habitSubtitle(habit)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = OmgSincTheme.colors.textMuted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    LinearProgress(
                        progress = habitProgress(habit),
                        color = tagColor,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            CheckToggle(checked = habit.todayCompleted, tint = tagColor, onClick = onToggle)
        }
    }
}

@Composable
private fun CheckToggle(checked: Boolean, tint: Color, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check-scale",
    )
    Box(
        modifier = Modifier
            .size(30.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (checked) tint else OmgSincTheme.colors.interactive)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Concluido",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun habitSubtitle(habit: Habit): String? {
    val target = habit.targetValue ?: return null
    val unit = habit.targetUnit.orEmpty()
    val current = habit.todayValue ?: 0.0
    return "${formatNumber(current)}/${formatNumber(target)} $unit".trim()
}

private fun habitProgress(habit: Habit): Float {
    val target = habit.targetValue ?: return if (habit.todayCompleted) 1f else 0f
    if (target <= 0) return 0f
    val current = habit.todayValue ?: 0.0
    return (current / target).toFloat()
}

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
