package com.example.data.repository

import com.example.data.local.dao.AlarmDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entities.AlarmEntity
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.TaskEntity
import com.example.domain.model.AlarmItem
import com.example.domain.model.Task
import com.example.domain.repository.AlarmRepository
import com.example.domain.repository.MemoryRepository
import com.example.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun TaskEntity.toDomain() = Task(id, title, dueDate, isCompleted, createdAt)
fun Task.toEntity() = TaskEntity(id, title, dueDate, isCompleted, createdAt)

fun AlarmEntity.toDomain() = AlarmItem(id, label, hour, minute, isEnabled, isRecurring, triggerTimeMs, createdAt)
fun AlarmItem.toEntity() = AlarmEntity(id, label, hour, minute, isEnabled, isRecurring, triggerTimeMs, createdAt)

class TaskRepositoryImpl(private val taskDao: TaskDao) : TaskRepository {
    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(id: Long) {
        taskDao.deleteTaskById(id)
    }

    override suspend fun clearCompletedTasks() {
        taskDao.clearCompletedTasks()
    }
}

class AlarmRepositoryImpl(private val alarmDao: AlarmDao) : AlarmRepository {
    override fun getAllAlarms(): Flow<List<AlarmItem>> {
        return alarmDao.getAllAlarms().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertAlarm(alarm: AlarmItem): Long {
        return alarmDao.insertAlarm(alarm.toEntity())
    }

    override suspend fun updateAlarm(alarm: AlarmItem) {
        alarmDao.updateAlarm(alarm.toEntity())
    }

    override suspend fun deleteAlarm(id: Int) {
        alarmDao.deleteAlarmById(id)
    }
}

class MemoryRepositoryImpl(private val memoryDao: MemoryDao) : MemoryRepository {
    override suspend fun getMemory(key: String): String? {
        return memoryDao.getMemory(key)?.value
    }

    override suspend fun saveMemory(key: String, value: String) {
        memoryDao.saveMemory(MemoryEntity(key, value))
    }

    override suspend fun deleteMemory(key: String) {
        memoryDao.deleteMemory(key)
    }
}
