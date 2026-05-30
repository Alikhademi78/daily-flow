package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val isRecurring: Boolean = false,
    val triggerTimeMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory")
data class MemoryEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedTime: Long = System.currentTimeMillis()
)
