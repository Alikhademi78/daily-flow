package com.example.ai_engine.intent

sealed class AssistantIntent {
    data class CreateTask(val title: String, val dueDateMs: Long? = null) : AssistantIntent()
    data class SendSms(val recipient: String, val message: String) : AssistantIntent()
    data class MakeCall(val recipient: String) : AssistantIntent()
    data class WebSearch(val query: String) : AssistantIntent()
    data class SetAlarm(val label: String, val hour: Int, val minute: Int, val isRecurring: Boolean = false, val triggerTimeMs: Long? = null) : AssistantIntent()
    data class Conversation(val text: String) : AssistantIntent()
}

sealed class AssistantAction {
    data class CreateTask(val title: String, val dueDateMs: Long? = null) : AssistantAction()
    data class SendSms(val recipient: String, val message: String) : AssistantAction()
    data class MakeCall(val phoneNumber: String, val recipientName: String) : AssistantAction()
    data class WebSearch(val query: String) : AssistantAction()
    data class CreateAlarm(val label: String, val hour: Int, val minute: Int, val isRecurring: Boolean = false, val triggerTimeMs: Long? = null) : AssistantAction()
    data class SpeakResponse(val speakText: String, val displayMessage: String? = null) : AssistantAction()
    data class QueryTasks(val filterQuery: String) : AssistantAction()
    data class QueryAlarms(val filterQuery: String) : AssistantAction()
}
