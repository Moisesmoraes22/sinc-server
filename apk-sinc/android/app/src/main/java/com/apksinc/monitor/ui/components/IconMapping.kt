package com.apksinc.monitor.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.apksinc.monitor.domain.HabitCategory
import com.apksinc.monitor.domain.MetricType

fun HabitCategory.icon(): ImageVector = when (this) {
    HabitCategory.AGUA -> Icons.Filled.WaterDrop
    HabitCategory.SONO -> Icons.Filled.Bedtime
    HabitCategory.EXERCICIO -> Icons.Filled.FitnessCenter
    HabitCategory.SKINCARE -> Icons.Filled.Spa
    HabitCategory.HUMOR -> Icons.Filled.Mood
    HabitCategory.OUTRO -> Icons.Filled.CheckCircle
}

fun MetricType.icon(): ImageVector = when (this) {
    MetricType.SLEEP_HOURS -> Icons.Filled.Bedtime
    MetricType.WATER_ML -> Icons.Filled.WaterDrop
    MetricType.MOOD_SCORE -> Icons.Filled.Mood
    MetricType.STEPS -> Icons.Filled.FitnessCenter
    MetricType.WEIGHT_KG -> Icons.Filled.CheckCircle
}

fun MetricType.label(): String = when (this) {
    MetricType.SLEEP_HOURS -> "Sono"
    MetricType.WATER_ML -> "Agua"
    MetricType.MOOD_SCORE -> "Humor"
    MetricType.STEPS -> "Passos"
    MetricType.WEIGHT_KG -> "Peso"
}

fun MetricType.unit(): String = when (this) {
    MetricType.SLEEP_HOURS -> "h"
    MetricType.WATER_ML -> "ml"
    MetricType.MOOD_SCORE -> "/10"
    MetricType.STEPS -> "passos"
    MetricType.WEIGHT_KG -> "kg"
}
