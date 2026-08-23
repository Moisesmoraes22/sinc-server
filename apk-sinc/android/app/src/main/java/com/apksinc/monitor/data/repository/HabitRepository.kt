package com.apksinc.monitor.data.repository

import com.apksinc.monitor.data.local.HabitEntity
import com.apksinc.monitor.data.local.SincDao
import com.apksinc.monitor.data.remote.ApiService
import com.apksinc.monitor.data.remote.DeviceRegisterRequestDto
import com.apksinc.monitor.data.remote.HabitCreateRequestDto
import com.apksinc.monitor.data.remote.HabitDto
import com.apksinc.monitor.data.remote.HabitLogRequestDto
import com.apksinc.monitor.data.remote.MetricCreateRequestDto
import com.apksinc.monitor.data.remote.ProfileUpdateRequestDto
import com.apksinc.monitor.domain.ColorTag
import com.apksinc.monitor.domain.Habit
import com.apksinc.monitor.domain.HabitCategory
import com.apksinc.monitor.domain.HomeSummary
import com.apksinc.monitor.domain.MetricReading
import com.apksinc.monitor.domain.MetricSnapshot
import com.apksinc.monitor.domain.MetricType
import com.apksinc.monitor.domain.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Fonte unica de verdade para a UI. Habitos tem cache local (Room) para a
 * tela abrir instantaneamente; perfil, resumo da Home e metricas sao lidos
 * direto da rede (dados que mudam pouco ou sao consultados sob demanda -
 * cache aqui adicionaria complexidade sem beneficio real de UX).
 */
class HabitRepository(
    private val api: ApiService,
    private val dao: SincDao,
) {
    fun observeHabits(): Flow<List<Habit>> =
        dao.observeHabits().map { list -> list.map { it.toDomain() } }

    suspend fun refreshHabits() {
        val response = api.getHabits()
        dao.replaceHabits(response.habits.map { it.toEntity() })
    }

    suspend fun logHabit(habitId: String, value: Double?, completed: Boolean) {
        api.logHabit(habitId, HabitLogRequestDto(value = value, completed = completed))
        refreshHabits()
    }

    suspend fun createHabit(
        title: String,
        category: HabitCategory,
        targetValue: Double?,
        targetUnit: String?,
        colorTag: ColorTag,
    ) {
        api.createHabit(
            HabitCreateRequestDto(
                title = title,
                category = category.name,
                targetValue = targetValue,
                targetUnit = targetUnit,
                colorTag = colorTag.name,
            )
        )
        refreshHabits()
    }

    suspend fun getProfile(): Profile =
        api.getProfile().let { Profile(id = it.id, displayName = it.displayName) }

    suspend fun updateProfile(displayName: String): Profile =
        api.updateProfile(ProfileUpdateRequestDto(displayName)).let { Profile(id = it.id, displayName = it.displayName) }

    suspend fun getHomeSummary(): HomeSummary {
        val dto = api.getHomeSummary()
        return HomeSummary(
            greetingName = dto.greetingName,
            weeklyProgressPct = dto.weeklyProgressPct,
            weeklyProgressDeltaPct = dto.weeklyProgressDeltaPct,
            habitsDoneToday = dto.habitsDoneToday,
            habitsTotalToday = dto.habitsTotalToday,
            nextActionHabitId = dto.nextActionHabitId,
            nextActionMessage = dto.nextActionMessage,
            metrics = dto.metrics.mapNotNull { m ->
                MetricType.fromRaw(m.metricType)?.let { type ->
                    MetricSnapshot(type = type, value = m.value, recordedAt = m.recordedAt)
                }
            },
            habits = dto.habits.map { it.toDomain() },
        )
    }

    suspend fun getMetricHistory(type: MetricType, days: Int = 30): List<MetricReading> =
        api.getMetrics(metricType = type.name, days = days).metrics.map {
            MetricReading(id = it.id ?: 0, type = type, value = it.value, recordedAt = it.recordedAt)
        }

    suspend fun addMetric(type: MetricType, value: Double) {
        api.createMetric(MetricCreateRequestDto(metricType = type.name, value = value))
    }

    suspend fun registerDeviceToken(token: String) {
        api.registerDevice(DeviceRegisterRequestDto(token = token))
    }
}

private fun HabitDto.toEntity() = HabitEntity(
    id = id.orEmpty(),
    title = title,
    category = category,
    iconKey = iconKey,
    targetValue = targetValue,
    targetUnit = targetUnit,
    colorTag = colorTag,
    todayValue = todayValue,
    todayCompleted = todayCompleted,
)

private fun HabitEntity.toDomain() = Habit(
    id = id,
    title = title,
    category = HabitCategory.fromRaw(category),
    iconKey = iconKey,
    targetValue = targetValue,
    targetUnit = targetUnit,
    colorTag = ColorTag.fromRaw(colorTag),
    todayValue = todayValue,
    todayCompleted = todayCompleted,
)

private fun HabitDto.toDomain() = Habit(
    id = id.orEmpty(),
    title = title,
    category = HabitCategory.fromRaw(category),
    iconKey = iconKey,
    targetValue = targetValue,
    targetUnit = targetUnit,
    colorTag = ColorTag.fromRaw(colorTag),
    todayValue = todayValue,
    todayCompleted = todayCompleted,
)
