package com.apksinc.monitor.data.remote

import com.google.gson.annotations.SerializedName

data class ProfileDto(
    val id: String?,
    @SerializedName("display_name") val displayName: String,
)

data class ProfileUpdateRequestDto(
    @SerializedName("display_name") val displayName: String,
)

data class HabitDto(
    val id: String?,
    val title: String,
    val category: String,
    @SerializedName("icon_key") val iconKey: String,
    @SerializedName("target_value") val targetValue: Double?,
    @SerializedName("target_unit") val targetUnit: String?,
    @SerializedName("color_tag") val colorTag: String,
    @SerializedName("today_value") val todayValue: Double?,
    @SerializedName("today_completed") val todayCompleted: Boolean,
)

data class HabitListResponseDto(
    val status: String,
    val habits: List<HabitDto>,
)

data class HabitCreateRequestDto(
    val title: String,
    val category: String,
    @SerializedName("icon_key") val iconKey: String = "circle",
    @SerializedName("target_value") val targetValue: Double?,
    @SerializedName("target_unit") val targetUnit: String?,
    @SerializedName("color_tag") val colorTag: String = "ACCENT",
)

data class HabitLogRequestDto(
    val value: Double?,
    val completed: Boolean,
)

data class MetricDto(
    val id: Int?,
    @SerializedName("metric_type") val metricType: String,
    val value: Double,
    @SerializedName("recorded_at") val recordedAt: String?,
)

data class MetricListResponseDto(
    val status: String,
    val metrics: List<MetricDto>,
)

data class MetricCreateRequestDto(
    @SerializedName("metric_type") val metricType: String,
    val value: Double,
)

data class HomeMetricSnapshotDto(
    @SerializedName("metric_type") val metricType: String,
    val value: Double?,
    @SerializedName("recorded_at") val recordedAt: String?,
)

data class HomeSummaryResponseDto(
    val status: String,
    @SerializedName("greeting_name") val greetingName: String,
    @SerializedName("weekly_progress_pct") val weeklyProgressPct: Double,
    @SerializedName("weekly_progress_delta_pct") val weeklyProgressDeltaPct: Double,
    @SerializedName("habits_done_today") val habitsDoneToday: Int,
    @SerializedName("habits_total_today") val habitsTotalToday: Int,
    @SerializedName("next_action_habit_id") val nextActionHabitId: String?,
    @SerializedName("next_action_message") val nextActionMessage: String,
    val metrics: List<HomeMetricSnapshotDto>,
    val habits: List<HabitDto>,
)

data class DeviceRegisterRequestDto(
    val token: String,
    val platform: String = "android",
)
