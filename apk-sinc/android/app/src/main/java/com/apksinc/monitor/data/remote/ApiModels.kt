package com.apksinc.monitor.data.remote

import com.google.gson.annotations.SerializedName

data class ServerDto(
    val id: String,
    val name: String,
    val status: String,
    @SerializedName("response_time_ms") val responseTimeMs: Int?,
    @SerializedName("last_check") val lastCheck: String?,
    @SerializedName("last_down_at") val lastDownAt: String?,
    @SerializedName("last_up_at") val lastUpAt: String?,
    @SerializedName("down_count") val downCount: Int,
    @SerializedName("send_status") val sendStatus: String?,
    @SerializedName("receive_status") val receiveStatus: String?,
    @SerializedName("send_elapsed_minutes") val sendElapsedMinutes: Double?,
    @SerializedName("receive_elapsed_minutes") val receiveElapsedMinutes: Double?,
    @SerializedName("last_send_at") val lastSendAt: String?,
    @SerializedName("last_receive_at") val lastReceiveAt: String?,
    @SerializedName("warning_threshold_minutes") val warningThresholdMinutes: Int?,
    @SerializedName("critical_threshold_minutes") val criticalThresholdMinutes: Int?,
)

data class ServerListResponseDto(
    val status: String,
    val servers: List<ServerDto>,
)

data class EventDto(
    val id: Int,
    @SerializedName("server_id") val serverId: String,
    @SerializedName("server_name") val serverName: String,
    @SerializedName("from_status") val fromStatus: String,
    @SerializedName("to_status") val toStatus: String,
    @SerializedName("occurred_at") val occurredAt: String,
    val reason: String?,
    @SerializedName("response_time_ms") val responseTimeMs: Int?,
    @SerializedName("duration_seconds") val durationSeconds: Int?,
)

data class EventListResponseDto(
    val status: String,
    val events: List<EventDto>,
)

data class HealthDto(
    val status: String,
    val database: String,
    val monitor: String,
    val firebase: String,
    @SerializedName("source_mode") val sourceMode: String,
    @SerializedName("db_backend") val dbBackend: String,
    @SerializedName("last_poll_at") val lastPollAt: String?,
    @SerializedName("units_count") val unitsCount: Int,
)

data class DeviceRegisterRequestDto(
    val token: String,
    val platform: String = "android",
)

data class TestNotificationResponseDto(
    val status: String,
    @SerializedName("devices_notified") val devicesNotified: Int,
)

data class UptimeDto(
    @SerializedName("period_days") val periodDays: Int,
    @SerializedName("uptime_percent") val uptimePercent: Double,
    @SerializedName("downtime_seconds") val downtimeSeconds: Int,
)
