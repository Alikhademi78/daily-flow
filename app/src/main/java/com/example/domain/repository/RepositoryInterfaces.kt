package com.example.domain.repository

import com.example.domain.model.AlarmItem
import com.example.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    suspend fun insertTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: Long)
    suspend fun clearCompletedTasks()
}

interface AlarmRepository {
    fun getAllAlarms(): Flow<List<AlarmItem>>
    suspend fun insertAlarm(alarm: AlarmItem): Long
    suspend fun updateAlarm(alarm: AlarmItem)
    suspend fun deleteAlarm(id: Int)
}

interface MemoryRepository {
    suspend fun getMemory(key: String): String?
    suspend fun saveMemory(key: String, value: String)
    suspend fun deleteMemory(key: String)
}
