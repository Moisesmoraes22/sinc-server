package com.apksinc.monitor.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/health")
    suspend fun getHealth(): HealthDto

    @GET("api/servers")
    suspend fun getServers(): ServerListResponseDto

    @GET("api/servers/{id}")
    suspend fun getServer(@Path("id") id: String): ServerDto

    @GET("api/events")
    suspend fun getEvents(
        @Query("server_id") serverId: String? = null,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 200,
    ): EventListResponseDto

    @POST("api/devices")
    suspend fun registerDevice(@Body request: DeviceRegisterRequestDto)

    @POST("api/devices/test-notification")
    suspend fun sendTestNotification(): TestNotificationResponseDto
}
