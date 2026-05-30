package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AlarmDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entities.AlarmEntity
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.TaskEntity

@Database(
    entities = [TaskEntity::class, AlarmEntity::class, MemoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class DailyFlowDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun alarmDao(): AlarmDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: DailyFlowDatabase? = null

        fun getDatabase(context: Context): DailyFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DailyFlowDatabase::class.java,
                    "dailyflow_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
