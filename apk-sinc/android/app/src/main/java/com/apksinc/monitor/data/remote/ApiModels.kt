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
)

data class EventListResponseDto(
    val status: String,
    val events: List<EventDto>,
)

data class DeviceRegisterRequestDto(
    val token: String,
    val platform: String = "android",
)
