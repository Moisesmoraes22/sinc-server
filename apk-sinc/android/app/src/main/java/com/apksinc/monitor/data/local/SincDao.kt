package com.apksinc.monitor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SincDao {

    @Query("SELECT * FROM servers ORDER BY name")
    fun observeServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getServer(id: String): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertServers(servers: List<ServerEntity>)

    @Query("SELECT * FROM events ORDER BY occurredAt DESC LIMIT :limit")
    fun observeEvents(limit: Int = 200): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE serverId = :serverId ORDER BY occurredAt DESC")
    fun observeEventsForServer(serverId: String): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<EventEntity>)
}
