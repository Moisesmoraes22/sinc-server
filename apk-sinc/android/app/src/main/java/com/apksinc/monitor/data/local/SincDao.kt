package com.apksinc.monitor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SincDao {

    @Query("SELECT * FROM servers ORDER BY name")
    fun observeServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getServer(id: String): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertServers(servers: List<ServerEntity>)

    @Query("DELETE FROM servers WHERE id NOT IN (:keepIds)")
    suspend fun deleteServersNotIn(keepIds: List<String>)

    @Query("DELETE FROM events WHERE serverId NOT IN (SELECT id FROM servers)")
    suspend fun deleteOrphanEvents()

    /**
     * Espelha a lista vinda do backend: alem de inserir/atualizar, remove do
     * cache local as unidades que o servidor nao retorna mais (e os eventos
     * orfaos que sobrariam). Sem isso, unidades desativadas - ou dados de
     * teste antigos - ficariam visiveis no app para sempre.
     *
     * Uma lista vazia e tratada como resposta suspeita (backend fora do ar,
     * fonte de monitoramento falhando) e NAO limpa o cache: e preferivel
     * mostrar dados possivelmente desatualizados a esvaziar a tela do usuario
     * por causa de uma falha transitoria.
     */
    @Transaction
    suspend fun replaceServers(servers: List<ServerEntity>) {
        if (servers.isEmpty()) return
        upsertServers(servers)
        deleteServersNotIn(servers.map { it.id })
        deleteOrphanEvents()
    }

    @Query("SELECT * FROM events ORDER BY occurredAt DESC LIMIT :limit")
    fun observeEvents(limit: Int = 200): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE serverId = :serverId ORDER BY occurredAt DESC")
    fun observeEventsForServer(serverId: String): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<EventEntity>)
}
