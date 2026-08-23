package com.apksinc.monitor.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/profile")
    suspend fun getProfile(): ProfileDto

    @PUT("api/profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequestDto): ProfileDto

    @GET("api/habits")
    suspend fun getHabits(): HabitListResponseDto

    @POST("api/habits")
    suspend fun createHabit(@Body request: HabitCreateRequestDto): HabitDto

    @POST("api/habits/{id}/log")
    suspend fun logHabit(@Path("id") habitId: String, @Body request: HabitLogRequestDto): HabitDto

    @GET("api/metrics")
    suspend fun getMetrics(
        @Query("metric_type") metricType: String? = null,
        @Query("days") days: Int = 30,
    ): MetricListResponseDto

    @POST("api/metrics")
    suspend fun createMetric(@Body request: MetricCreateRequestDto): MetricDto

    @GET("api/home/summary")
    suspend fun getHomeSummary(): HomeSummaryResponseDto

    @POST("api/devices")
    suspend fun registerDevice(@Body request: DeviceRegisterRequestDto)
}
