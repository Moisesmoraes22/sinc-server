package com.apksinc.monitor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cache local dos habitos, para a tela abrir instantaneamente enquanto a
 * rede atualiza em segundo plano (mesmo padrao offline-first do app antigo). */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val iconKey: String,
    val targetValue: Double?,
    val targetUnit: String?,
    val colorTag: String,
    val todayValue: Double?,
    val todayCompleted: Boolean,
)
