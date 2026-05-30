package com.example.feature.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_engine.orchestrator.AiOrchestrator
import com.example.ai_engine.orchestrator.AssistantState
import com.example.core.voice.SpeechToTextManager
import com.example.core.voice.TextToSpeechManager
import com.example.data.local.database.DailyFlowDatabase
import com.example.data.repository.AlarmRepositoryImpl
import com.example.data.repository.MemoryRepositoryImpl
import com.example.data.repository.TaskRepositoryImpl
import com.example.domain.model.AlarmItem
import com.example.domain.model.ConversationMessage
import com.example.domain.model.Task
import com.example.feature.alarm.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MainViewModel"

    private val database = DailyFlowDatabase.getDatabase(application)
    val taskRepository = TaskRepositoryImpl(database.taskDao())
    val alarmRepository = AlarmRepositoryImpl(database.alarmDao())
    val memoryRepository = MemoryRepositoryImpl(database.memoryDao())

    private var speechToTextManager: SpeechToTextManager? = null
    private val ttsManager = TextToSpeechManager(application)

    // AI Orchestrator Layer
    val orchestrator = AiOrchestrator(
        context = application,
        taskRepository = taskRepository,
        alarmRepository = alarmRepository,
        memoryRepository = memoryRepository,
        ttsManager = ttsManager,
        externalScope = viewModelScope
    )

    // SharedPreferences for persistent user configs
    private val sharedPrefs = application.getSharedPreferences("daily_flow_preferences", android.content.Context.MODE_PRIVATE)

    private val _voiceLanguage = MutableStateFlow(sharedPrefs.getString("speech_language", "fa") ?: "fa")
    val voiceLanguage: StateFlow<String> = _voiceLanguage.asStateFlow()

    private val _themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "auto") ?: "auto")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // UI Reactive State Flows
    val tasks: StateFlow<List<Task>> = taskRepository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alarms: StateFlow<List<AlarmItem>> = alarmRepository.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<ConversationMessage>> = orchestrator.messages
    val assistantState: StateFlow<AssistantState> = orchestrator.state

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _lastSpeechError = MutableStateFlow<String?>(null)
    val lastSpeechError: StateFlow<String?> = _lastSpeechError.asStateFlow()

    init {
        // Synchronize initial configuration parameters
        val currentLang = voiceLanguage.value
        ttsManager.setSpeechLanguage(currentLang)
        orchestrator.currentLanguage = currentLang

        // Initialize Speech-to-Text Manager
        speechToTextManager = SpeechToTextManager(
            context = application,
            onResult = { transcript ->
                Log.d(TAG, "STT Result: '$transcript'")
                orchestrator.processUserInput(transcript)
            },
            onError = { err ->
                Log.e(TAG, "STT Error: '$err'")
                _lastSpeechError.value = err
                // Reset recording states
                _isRecording.value = false
            },
            onStateChange = { active ->
                _isRecording.value = active
            }
        )
    }

    fun setVoiceLanguage(lang: String) {
        _voiceLanguage.value = lang
        sharedPrefs.edit().putString("speech_language", lang).apply()
        ttsManager.setSpeechLanguage(lang)
        orchestrator.currentLanguage = lang
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
    }

    /**
     * Toggles Voice Input listening state using dynamic voice locales.
     */
    fun toggleVoiceListening() {
        val stt = speechToTextManager ?: return
        if (isRecording.value) {
            stt.stopListening()
        } else {
            _lastSpeechError.value = null
            val code = if (voiceLanguage.value == "en") "en-US" else "fa-IR"
            stt.startListening(code)
        }
    }

    /**
     * Submits textual prompt to mock STT voice triggers (extremely useful for headless emulator tests).
     */
    fun submitSimulatedVoiceText(text: String) {
        orchestrator.processUserInput(text)
    }

    /**
     * Local direct manipulations for Task items from checkboxes/UI taps.
     */
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun insertTaskDirectly(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.insertTask(Task(title = title))
        }
    }

    fun removeTask(id: Long) {
        viewModelScope.launch {
            taskRepository.deleteTask(id)
        }
    }

    /**
     * Local direct manipulations for Alarms from the UI list.
     */
    fun toggleAlarmState(alarm: AlarmItem) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            alarmRepository.updateAlarm(updated)
            if (updated.isEnabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), updated)
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), updated)
            }
        }
    }

    fun removeAlarm(alarm: AlarmItem) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), alarm)
            alarmRepository.deleteAlarm(alarm.id)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            taskRepository.clearCompletedTasks()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechToTextManager?.destroy()
        speechToTextManager = null
        ttsManager.destroy()
    }
}
