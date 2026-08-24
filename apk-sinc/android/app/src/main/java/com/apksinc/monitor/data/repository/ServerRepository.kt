package com.apksinc.monitor.data.repository

import com.apksinc.monitor.data.local.EventEntity
import com.apksinc.monitor.data.local.ServerEntity
import com.apksinc.monitor.data.local.SincDao
import com.apksinc.monitor.data.remote.ApiService
import com.apksinc.monitor.data.remote.DeviceRegisterRequestDto
import com.apksinc.monitor.data.remote.EventDto
import com.apksinc.monitor.data.remote.HealthDto
import com.apksinc.monitor.data.remote.ServerDto
import com.apksinc.monitor.domain.ServerEvent
import com.apksinc.monitor.domain.ServerInfo
import com.apksinc.monitor.domain.ServerStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Fonte unica de verdade para a UI: busca do backend e mantem um cache local
 * (Room) para exibicao offline/rapida. O app nunca monitora nada sozinho -
 * apenas consulta o backend, que e quem detecta as mudancas de status.
 */
class ServerRepository(
    private val api: ApiService,
    private val dao: SincDao,
) {
    fun observeServers(): Flow<List<ServerInfo>> =
        dao.observeServers().map { list -> list.map { it.toDomain() } }

    fun observeEvents(limit: Int = 200): Flow<List<ServerEvent>> =
        dao.observeEvents(limit).map { list -> list.map { it.toDomain() } }

    fun observeEventsForServer(serverId: String): Flow<List<ServerEvent>> =
        dao.observeEventsForServer(serverId).map { list -> list.map { it.toDomain() } }

    suspend fun refreshServers() {
        val response = api.getServers()
        dao.replaceServers(response.servers.map { it.toEntity() })
    }

    suspend fun refreshEvents(limit: Int = 200) {
        val response = api.getEvents(limit = limit)
        dao.upsertEvents(response.events.map { it.toEntity() })
    }

    suspend fun getServer(id: String): ServerInfo? = dao.getServer(id)?.toDomain()

    suspend fun registerDeviceToken(token: String) {
        api.registerDevice(DeviceRegisterRequestDto(token = token))
    }

    // Diagnostico da infraestrutura - dado sempre ao vivo, nao faz sentido
    // cachear no Room (o ponto e saber o estado agora, nao o de ontem).
    suspend fun getHealth(): HealthDto = api.getHealth()
}

private fun ServerDto.toEntity() = ServerEntity(
    id = id,
    name = name,
    status = status,
    responseTimeMs = responseTimeMs,
    lastCheck = lastCheck,
    lastDownAt = lastDownAt,
    lastUpAt = lastUpAt,
    downCount = downCount,
    sendStatus = sendStatus,
    receiveStatus = receiveStatus,
    sendElapsedMinutes = sendElapsedMinutes,
    receiveElapsedMinutes = receiveElapsedMinutes,
    lastSendAt = lastSendAt,
    lastReceiveAt = lastReceiveAt,
    warningThresholdMinutes = warningThresholdMinutes,
    criticalThresholdMinutes = criticalThresholdMinutes,
)

private fun ServerEntity.toDomain() = ServerInfo(
    id = id,
    name = name,
    status = ServerStatus.fromRaw(status),
    responseTimeMs = responseTimeMs,
    lastCheck = lastCheck,
    lastDownAt = lastDownAt,
    lastUpAt = lastUpAt,
    downCount = downCount,
    sendStatus = sendStatus?.let { ServerStatus.fromRaw(it) },
    receiveStatus = receiveStatus?.let { ServerStatus.fromRaw(it) },
    sendElapsedMinutes = sendElapsedMinutes,
    receiveElapsedMinutes = receiveElapsedMinutes,
    lastSendAt = lastSendAt,
    lastReceiveAt = lastReceiveAt,
    warningThresholdMinutes = warningThresholdMinutes,
    criticalThresholdMinutes = criticalThresholdMinutes,
)

private fun EventDto.toEntity() = EventEntity(
    id = id,
    serverId = serverId,
    serverName = serverName,
    fromStatus = fromStatus,
    toStatus = toStatus,
    occurredAt = occurredAt,
    reason = reason,
    responseTimeMs = responseTimeMs,
    durationSeconds = durationSeconds,
)

private fun EventEntity.toDomain() = ServerEvent(
    id = id,
    serverId = serverId,
    serverName = serverName,
    fromStatus = fromStatus,
    toStatus = toStatus,
    occurredAt = occurredAt,
    reason = reason,
    responseTimeMs = responseTimeMs,
    durationSeconds = durationSeconds,
)
