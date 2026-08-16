package com.apksinc.monitor.domain

enum class ServerStatus {
    ONLINE,
    ATENCAO,
    OFFLINE;

    companion object {
        fun fromRaw(raw: String): ServerStatus = when (raw.uppercase()) {
            "ONLINE" -> ONLINE
            "ATENCAO", "ATENÇÃO" -> ATENCAO
            "OFFLINE" -> OFFLINE
            else -> ATENCAO
        }
    }
}

data class ServerInfo(
    val id: String,
    val name: String,
    val status: ServerStatus,
    val responseTimeMs: Int?,
    val lastCheck: String?,
    val lastDownAt: String?,
    val lastUpAt: String?,
    val downCount: Int,
)

data class ServerEvent(
    val id: Int,
    val serverId: String,
    val serverName: String,
    val fromStatus: String,
    val toStatus: String,
    val occurredAt: String,
    val reason: String?,
    val responseTimeMs: Int?,
)
