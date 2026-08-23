package com.apksinc.monitor.domain

/** Categoria do habito - determina o icone/agrupamento nas telas. */
enum class HabitCategory {
    AGUA, SONO, EXERCICIO, SKINCARE, HUMOR, OUTRO;

    companion object {
        fun fromRaw(raw: String): HabitCategory = entries.find { it.name == raw } ?: OUTRO
    }
}

/** Cor de destaque do habito - sempre um dos tokens funcionais oficiais. */
enum class ColorTag {
    ACCENT, SUCCESS, WARNING;

    companion object {
        fun fromRaw(raw: String): ColorTag = entries.find { it.name == raw } ?: ACCENT
    }
}

enum class MetricType {
    SLEEP_HOURS, WATER_ML, MOOD_SCORE, STEPS, WEIGHT_KG;

    companion object {
        fun fromRaw(raw: String): MetricType? = entries.find { it.name == raw }
    }
}

data class Profile(
    val id: String?,
    val displayName: String,
)

data class Habit(
    val id: String,
    val title: String,
    val category: HabitCategory,
    val iconKey: String,
    val targetValue: Double?,
    val targetUnit: String?,
    val colorTag: ColorTag,
    val todayValue: Double?,
    val todayCompleted: Boolean,
)

data class MetricSnapshot(
    val type: MetricType,
    val value: Double?,
    val recordedAt: String?,
)

data class MetricReading(
    val id: Int,
    val type: MetricType,
    val value: Double,
    val recordedAt: String?,
)

data class HomeSummary(
    val greetingName: String,
    val weeklyProgressPct: Double,
    val weeklyProgressDeltaPct: Double,
    val habitsDoneToday: Int,
    val habitsTotalToday: Int,
    val nextActionHabitId: String?,
    val nextActionMessage: String,
    val metrics: List<MetricSnapshot>,
    val habits: List<Habit>,
)
