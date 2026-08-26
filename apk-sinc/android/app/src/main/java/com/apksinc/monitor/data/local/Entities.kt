package com.apksinc.monitor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val status: String,
    val responseTimeMs: Int?,
    val lastCheck: String?,
    val lastDownAt: String?,
    val lastUpAt: String?,
    val downCount: Int,
    val sendStatus: String?,
    val receiveStatus: String?,
    val sendElapsedMinutes: Double?,
    val receiveElapsedMinutes: Double?,
    val lastSendAt: String?,
    val lastReceiveAt: String?,
    val warningThresholdMinutes: Int?,
    val criticalThresholdMinutes: Int?,
    val revisionsToSend: Int?,
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: Int,
    val serverId: String,
    val serverName: String,
    val fromStatus: String,
    val toStatus: String,
    val occurredAt: String,
    val reason: String?,
    val responseTimeMs: Int?,
    val durationSeconds: Int?,
)
