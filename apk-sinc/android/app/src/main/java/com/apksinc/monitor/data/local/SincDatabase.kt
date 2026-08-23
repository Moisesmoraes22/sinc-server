package com.apksinc.monitor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HabitEntity::class], version = 2, exportSchema = false)
abstract class SincDatabase : RoomDatabase() {
    abstract fun sincDao(): SincDao

    companion object {
        @Volatile private var instance: SincDatabase? = null

        fun getInstance(context: Context): SincDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SincDatabase::class.java,
                    "sinc.db",
                )
                    // Cache local descartavel (fonte da verdade e sempre a API) -
                    // um reset limpo no pivot de dominio e preferivel a manter
                    // uma migracao complexa para tabelas que nao existem mais.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
