package com.example.domain.model

data class Task(
    val id: Long = 0,
    val title: String,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class AlarmItem(
    val id: Int = 0,
    val label: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val isRecurring: Boolean = false,
    val triggerTimeMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedTime: CharSequence
        get() = String.format("%02d:%02d", hour, minute)
}

data class ConversationMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
