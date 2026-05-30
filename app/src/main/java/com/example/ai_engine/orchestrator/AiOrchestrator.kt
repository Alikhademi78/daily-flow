package com.example.ai_engine.orchestrator

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import com.example.ai_engine.intent.AssistantAction
import com.example.ai_engine.intent.AssistantIntent
import com.example.ai_engine.parser.LocalPersianParser
import com.example.core.utils.ContactUtils
import com.example.core.voice.TextToSpeechManager
import com.example.data.remote.gemini.GeminiClient
import com.example.data.remote.websearch.DuckDuckGoWebSearch
import com.example.domain.model.AlarmItem
import com.example.domain.model.ConversationMessage
import com.example.domain.model.Task
import com.example.domain.repository.AlarmRepository
import com.example.domain.repository.MemoryRepository
import com.example.domain.repository.TaskRepository
import com.example.feature.alarm.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlinx.coroutines.flow.first
import java.util.Calendar

sealed class AssistantState {
    object Idle : AssistantState()
    object Listening : AssistantState()
    object Processing : AssistantState()
    data class AwaitingConfirmation(val action: AssistantAction, val textPrompt: String) : AssistantState()
    data class Error(val message: String) : AssistantState()
}

class AiOrchestrator(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val alarmRepository: AlarmRepository,
    private val memoryRepository: MemoryRepository,
    private val ttsManager: TextToSpeechManager,
    private val externalScope: CoroutineScope
) {
    private val TAG = "AiOrchestrator"

    private val _messages = MutableStateFlow<List<ConversationMessage>>(
        listOf(
            ConversationMessage(
                text = "سلام! من دستیار دیلی‌فلو هستم. کارهای امروزتون، پیامک‌ها، آلارم‌ها یا سوالاتتون رو از من بپرسید. برای صحبت با من دکمه میکروفون رو نگهدارید.",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ConversationMessage>> = _messages.asStateFlow()

    private val _state = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    var currentLanguage: String = "fa"

    private fun getSystemInstructions(): String {
        val isEng = currentLanguage == "en"
        val languageName = if (isEng) "English" else "Persian (Farsi)"
        val toneInstruction = if (isEng) {
            "Respond entirely in clean, polite, helpful English. All speech speakText must be read in US English."
        } else {
            "Respond entirely in clean, polite, poetically charming Persian (Farsi). All speech speakText must be read in Persian (Farsi)."
        }

        return """
            You are the NLU/NLP parser engine for "DailyFlow" - a voice-first executive Android task assistant.
            The user's currently selected speech language is: $languageName.
            $toneInstruction
            
            Read the user voice transcript or text query, and return ONLY a single structured JSON object representing the matched action inside a markdown-free payload.
            Do NOT write details or normal responses unless the action is "speak_response". Keep responses polite, professional and in $languageName.
    
            The JSON schema MUST match one of these formats:
    
            1. Create Task (e.g. "ثبت قرار ملاقات برای فردا"):
            {"action": "create_task", "title": "TEXT_OF_THE_TASK", "dueDateMs": null_or_timestamp, "isRecurring": true_or_false}
    
            2. Send SMS (requires user voice approval downstream):
            {"action": "send_sms", "recipient": "RECIPIENT_NAME", "message": "MESSAGE_BODY"}
    
            3. Make Call (requires approval):
            {"action": "make_call", "recipient": "RECIPIENT_NAME"}
    
            4. Create Alarm (e.g. "آلارم بذار برای ساعت ۸"):
            {"action": "create_alarm", "label": "ALARM_LABEL", "hour": HOUR_24, "minute": MINUTE, "isRecurring": true_or_false, "triggerTimeMs": EXACT_UNIX_TIMESTAMP_IF_RELATIVE_OR_NULL}
            
            IMPORTANT:
             - If relative or specific future time is used (e.g "فردا ساعت 4 عصر", "فردا صبح ساعت 3", "پس‌فردا ساعت 9", "یک دقیقه دیگه", "5 minutes from now"), you MUST calculate and include the "triggerTimeMs" as the exact UNIX epoch timestamp of that specific scheduled moment (e.g. for tomorrow 4 PM, calculate the timestamp corresponding to 16:00 tomorrow in target local time, which is relative to the current local time supplied below). Otherwise leave it null.
             - If the user asks you to set an alarm/task, but does NOT specify a specific time (e.g. missing hour, missing AM/PM except for 24-hour style), return "action": "speak_response" and ask the user "عصر یا صبح؟" or "چه ساعتی؟". DO NOT guess times.
             - EXTREMELY IMPORTANT ABOUT isRecurring: Only set isRecurring to true if the user explicitly specifies words like 'هر روز' (every day), 'همه روزها' or 'همیشه' (always). If the user just says an hour, a day (like "آلارم بذار برای 8") or a relative time ("فردا صبح", "پس‌فردا", "فردا ساعت ۴ عصر"), you MUST set isRecurring to false. So it rings ONLY ONCE.
             - If the user asks to schedule something, DO NOT try to use external phone calendars. Always return create_task or create_alarm. Our system has its own independent internal offline memory database and schedule UI.
    
            5. Web Search (general queries about weather, web facts, generic prices):
            {"action": "web_search", "query": "SEARCH_QUERY"}
    
            6. Chit-chat / Conversation response:
            {"action": "speak_response", "speakText": "CONVERSATION_VOICE_SPEECH", "displayMessage": "OPTIONAL_LONGER_DISPLAY_TEXT"}

            7. Query/Read Tasks & Appointments (e.g. "چه قرار ملاقات هایی دارم", "تسک‌های من چیه", "ساعت آلارم من فردا چیه", "چه کارگاهی دارم"):
            {"action": "query_tasks", "query": "USER_FILTER_OR_QUESTION"}

            8. Query/Read Alarms (e.g. "چه آلارم هایی دارم", "آلارم هام چیه"):
            {"action": "query_alarms", "query": "USER_FILTER_OR_QUESTION"}
    
            IMPORTANT:
            - Current local time is ${Calendar.getInstance().time}
            - Current UNIX timestamp is ${System.currentTimeMillis()} (Use this for precise milliseconds additions like adding 60000 for 1 minute elapsed)
            - Match "query_tasks" ONLY when user asks about their tasks, appointments, schedules, calendars, or items they've scheduled previously.
            - Match "query_alarms" ONLY when user asks about what alarms or wake-up signals they've set up on the phone.
            - Return strictly JSON. NO formatting tags like ```json or trailing letters. Use ONLY one of the 8 action types above.
        """.trimIndent()
    }

    /**
     * Integrates Voice/Text Input and routes to local execution or Gemini fallback, managing states reactively.
     */
    fun processUserInput(query: String) {
        if (query.trim().isEmpty()) return

        externalScope.launch(Dispatchers.Main) {
            // Add user's query to message streams UI immediately
            val userMsg = ConversationMessage(text = query, isUser = true)
            _messages.value = _messages.value + userMsg

            val currentState = _state.value
            if (currentState is AssistantState.AwaitingConfirmation) {
                handleConfirmationResponse(query, currentState.action)
                return@launch
            }

            _state.value = AssistantState.Processing

            // 1. LOCAL PERSian RULES PARSING (First-priority offline support)
            val localIntent = LocalPersianParser.tryParseLocalIntent(query)
            if (localIntent != null) {
                Log.d(TAG, "Local parser succeeded: $localIntent")
                executeLocalIntent(localIntent)
            } else {
                // 2. GEMINI PARSING FALLBACK (Online Deep NLU)
                Log.d(TAG, "Local parsing ambiguous. Invoking Gemini API fallback.")
                executeGeminiFallback(query)
            }
        }
    }

    private suspend fun executeLocalIntent(intent: AssistantIntent) {
        when (intent) {
            is AssistantIntent.CreateTask -> {
                val action = AssistantAction.CreateTask(intent.title, intent.dueDateMs)
                executeActionDirectly(action)
            }
            is AssistantIntent.SetAlarm -> {
                val action = AssistantAction.CreateAlarm(intent.label, intent.hour, intent.minute, intent.isRecurring, intent.triggerTimeMs)
                executeActionDirectly(action)
            }
            is AssistantIntent.SendSms -> {
                val action = AssistantAction.SendSms(intent.recipient, intent.message)
                requestActionConfirmation(action, "آیا مایلید پیامک به '${intent.recipient}' ارسال شود؟")
            }
            is AssistantIntent.MakeCall -> {
                val number = ContactUtils.resolvePhoneNumber(context, intent.recipient)
                val action = AssistantAction.MakeCall(number, intent.recipient)
                requestActionConfirmation(action, "آیا مایلید با '${intent.recipient}' تماس گرفته شود؟")
            }
            is AssistantIntent.WebSearch -> {
                val action = AssistantAction.WebSearch(intent.query)
                executeActionDirectly(action)
            }
            is AssistantIntent.Conversation -> {
                val action = AssistantAction.SpeakResponse(intent.text)
                executeActionDirectly(action)
            }
        }
    }

    private fun executeGeminiFallback(query: String) {
        externalScope.launch(Dispatchers.IO) {
            val rawResponse = GeminiClient.queryGemini(
                prompt = query,
                systemInstruction = getSystemInstructions(),
                returnJson = true
            )

            launch(Dispatchers.Main) {
                if (rawResponse == "API_KEY_MISSING") {
                    val isEng = currentLanguage == "en"
                    val localResponseMsg = if (isEng) {
                        "Sorry, deep AI features require a Gemini API key. However, local task and alarm managers are still fully operational."
                    } else {
                        "متاسفم، برای پردازش عمیق سوال شما نیاز به کلید API جمینی است. با این حال، کارهایی مثل اضافه کردن تسک و آلارم به صورت محلی در دسترس هستند."
                    }
                    ttsManager.speak(localResponseMsg)
                    addAssistantMessage(localResponseMsg)
                    _state.value = AssistantState.Idle
                    return@launch
                }

                try {
                    val cleanedJson = rawResponse.trim()
                        .removePrefix("```json")
                        .removeSuffix("```")
                        .trim()

                    Log.d(TAG, "Gemini JSON reply: '$cleanedJson'")
                    val json = JSONObject(cleanedJson)
                    val actionType = json.optString("action", "")

                    when (actionType) {
                        "create_task" -> {
                            val title = json.optString("title", if (currentLanguage == "en") "Voice Task" else "کار صوتی")
                            val due = if (json.isNull("dueDateMs")) null else json.optLong("dueDateMs")
                            executeActionDirectly(AssistantAction.CreateTask(title, due))
                        }
                        "send_sms" -> {
                            val recipient = json.optString("recipient", "")
                            val msg = json.optString("message", "")
                            val action = AssistantAction.SendSms(recipient, msg)
                            requestActionConfirmation(
                                action,
                                "آیا پیامک به '$recipient' ارسال شود؟",
                                "Should I send the SMS to '$recipient'?"
                            )
                        }
                        "make_call" -> {
                            val recipient = json.optString("recipient", "")
                            val number = ContactUtils.resolvePhoneNumber(context, recipient)
                            val action = AssistantAction.MakeCall(number, recipient)
                            requestActionConfirmation(
                                action,
                                "آیا تماس با '$recipient' برقرار کنم؟",
                                "Should I place a call to '$recipient'?"
                            )
                        }
                        "create_alarm" -> {
                            val label = json.optString("label", if (currentLanguage == "en") "Wake up" else "بیداری")
                            val hour = json.optInt("hour", 8)
                            val minute = json.optInt("minute", 0)
                            val isRec = json.optBoolean("isRecurring", false)
                            val triggerMs = if (json.isNull("triggerTimeMs")) null else json.optLong("triggerTimeMs")
                            executeActionDirectly(AssistantAction.CreateAlarm(label, hour, minute, isRec, triggerMs))
                        }
                        "web_search" -> {
                            val webQuery = json.optString("query")
                            executeActionDirectly(AssistantAction.WebSearch(webQuery))
                        }
                        "speak_response" -> {
                            val speech = json.optString("speakText", "")
                            val dMsg = json.optString("displayMessage", speech)
                            executeActionDirectly(AssistantAction.SpeakResponse(speech, dMsg))
                        }
                        "query_tasks" -> {
                            val q = json.optString("query", query)
                            executeActionDirectly(AssistantAction.QueryTasks(q))
                        }
                        "query_alarms" -> {
                            val q = json.optString("query", query)
                            executeActionDirectly(AssistantAction.QueryAlarms(q))
                        }
                        else -> {
                            val promptSuffix = if (currentLanguage == "en") "Write a brief helpful response to: $query" else "پاسخ رسا و کوتاه به زبان فارسی"
                            val textReply = GeminiClient.queryGemini(query, promptSuffix)
                            executeActionDirectly(AssistantAction.SpeakResponse(textReply))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed parsing Gemini JSON. Fallback to conversation.", e)
                    val promptSuffix = if (currentLanguage == "en") "You are simple friendly AI talking to assistant user in US English." else "شما با کاربر صحبت ساده فارسی می‌کنید."
                    val textReply = GeminiClient.queryGemini(query, promptSuffix)
                    executeActionDirectly(AssistantAction.SpeakResponse(textReply))
                }
            }
        }
    }

    private fun requestActionConfirmation(action: AssistantAction, promptFa: String, promptEn: String = "") {
        val isEng = currentLanguage == "en"
        val finalPrompt = if (isEng && promptEn.isNotEmpty()) promptEn else promptFa
        _state.value = AssistantState.AwaitingConfirmation(action, finalPrompt)
        ttsManager.speak(finalPrompt)
        addAssistantMessage(finalPrompt)
    }

    private fun handleConfirmationResponse(userInput: String, pendingAction: AssistantAction) {
        val normalized = LocalPersianParser.normalizeText(userInput)
        val isEng = currentLanguage == "en"

        val isAffirmative = normalized.contains("بله") ||
                normalized.contains("ارسال") ||
                normalized.contains("تایید") ||
                normalized.contains("آره") ||
                normalized.contains("تماس بگیر") ||
                normalized.contains("بفرست") ||
                normalized.contains("yes") ||
                normalized.contains("send") ||
                normalized.contains("confirm") ||
                normalized.contains("okay") ||
                normalized.contains("ok")

        val isNegative = normalized.contains("خیر") ||
                normalized.contains("لغو") ||
                normalized.contains("نفرست") ||
                normalized.contains("به هیچ وجه") ||
                normalized.contains("نه") ||
                normalized.contains("no") ||
                normalized.contains("cancel")

        if (isAffirmative) {
            _state.value = AssistantState.Processing
            executeActionDirectly(pendingAction)
        } else if (isNegative) {
            _state.value = AssistantState.Idle
            val cancelMsg = if (isEng) "Operation cancelled." else "عملیات لغو شد."
            ttsManager.speak(cancelMsg)
            addAssistantMessage(cancelMsg)
        } else {
            val rePrompt = if (isEng) "I didn't catch that. Should I perform this action? (Yes / No)" else "ببخشید، متوجه نشدم. آیا این کار انجام بشه؟ (بله / خیر)"
            _state.value = AssistantState.AwaitingConfirmation(pendingAction, rePrompt)
            ttsManager.speak(rePrompt)
            addAssistantMessage(rePrompt)
        }
    }

    private fun executeActionDirectly(action: AssistantAction) {
        externalScope.launch(Dispatchers.Main) {
            val isEng = currentLanguage == "en"
            when (action) {
                is AssistantAction.CreateTask -> {
                    taskRepository.insertTask(Task(title = action.title, dueDate = action.dueDateMs))
                    val voiceR = if (isEng) "Task '${action.title}' created successfully." else "وظیفه جدید با موضوع '${action.title}' ایجاد شد."
                    ttsManager.speak(voiceR)
                    addAssistantMessage(voiceR)
                    _state.value = AssistantState.Idle
                }
                is AssistantAction.CreateAlarm -> {
                    val alarmId = (1000..9999).random()
                    val item = AlarmItem(id = alarmId, label = action.label, hour = action.hour, minute = action.minute, isRecurring = action.isRecurring, triggerTimeMs = action.triggerTimeMs)
                    alarmRepository.insertAlarm(item)
                    AlarmScheduler.scheduleAlarm(context, item)

                    val verbal = if (isEng) "Alarm set for ${action.hour} ${String.format("%02d", action.minute)}." else "زنگ هشدار برای ساعت ${action.hour} و ${action.minute} تنظیم شد."
                    val display = if (isEng) "Alarm '${action.label}' set for ${String.format("%02d:%02d", action.hour, action.minute)}." else "زنگ هشدار '${action.label}' برای ساعت ${String.format("%02d:%02d", action.hour, action.minute)} ثبت شد."
                    ttsManager.speak(verbal)
                    addAssistantMessage(display)
                    _state.value = AssistantState.Idle
                }
                is AssistantAction.SendSms -> {
                    val number = ContactUtils.resolvePhoneNumber(context, action.recipient)
                    try {
                        val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            context.getSystemService(SmsManager::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            SmsManager.getDefault()
                        }
                        smsManager.sendTextMessage(number, null, action.message, null, null)

                        val verbal = if (isEng) "SMS sent successfully." else "پیامک با موفقیت ارسال شد."
                        val display = if (isEng) "SMS sent to '${action.recipient}' ($number): '${action.message}'" else "پیامک با موفقیت به '${action.recipient}' ($number) ارسال شد. متن پیام: '${action.message}'"
                        ttsManager.speak(verbal)
                        addAssistantMessage(display)
                    } catch (e: Exception) {
                        Log.e(TAG, "SmsManager send error", e)
                        val errorV = if (isEng) "Routing failed. Saved draft for '${action.recipient}'." else "خطا در بستر پیام رسانی. پیش‌نویس پیامک برای '${action.recipient}' ذخیره شد."
                        val display = if (isEng) "SMS failed to send. Draft content: '${action.message}'" else "پیامک ارسال نشد (نیاز به مجوز سیستم یا سیم‌کارت). متن پیش‌فرض: '${action.message}'"
                        ttsManager.speak(errorV)
                        addAssistantMessage(display)
                    }
                    _state.value = AssistantState.Idle
                }
                is AssistantAction.MakeCall -> {
                    try {
                        val callIntent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:${action.phoneNumber}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(callIntent)
                        val verbal = if (isEng) "Calling ${action.recipientName}" else "در حال برقراری تماس صوتی با ${action.recipientName}"
                        val display = if (isEng) "Calling ${action.recipientName} (${action.phoneNumber})." else "در حال برقراری تماس مستقیم با ${action.recipientName} (${action.phoneNumber})."
                        ttsManager.speak(verbal)
                        addAssistantMessage(display)
                    } catch (e: SecurityException) {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${action.phoneNumber}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(dialIntent)
                        val verbal = if (isEng) "Dial pad open for ${action.recipientName}." else "صفحه تماس برای تماس با ${action.recipientName} باز شد."
                        val display = if (isEng) "Dial screen launched for ${action.recipientName}." else "صفحه شماره‌گیری با شماره ${action.recipientName} باز شد (جهت تایید نهایی تماس دکمه سبز را بفشارید)."
                        ttsManager.speak(verbal)
                        addAssistantMessage(display)
                    } catch (e: Exception) {
                        Log.e(TAG, "Call initiation error", e)
                        val err = if (isEng) "Calling is currently unavailable." else "تماس صوتی امکان‌پذیر نبود."
                        addAssistantMessage(err)
                    }
                    _state.value = AssistantState.Idle
                }
                is AssistantAction.WebSearch -> {
                    _state.value = AssistantState.Processing
                    val label = if (isEng) "Searching the web for '${action.query}'..." else "در حال جستجوی وب برای: '${action.query}'..."
                    addAssistantMessage(label)
                    
                    externalScope.launch(Dispatchers.IO) {
                        val ddgResult = DuckDuckGoWebSearch.performSearch(action.query)
                        
                        launch(Dispatchers.Main) {
                            if (GeminiClient.isApiKeyAvailable && ddgResult.length > 50) {
                                val queryPrompt = if (isEng) {
                                    "Synthesize draft web results to answer query '${action.query}': \n$ddgResult\nKeep reply short and professional in English."
                                } else {
                                    "خلاصه‌سازی نهایی اطلاعات وب برای پاسخ به سوال '${action.query}': \n$ddgResult\nپاسخ کوتاه و به فارسی باشد."
                                }
                                val synthesMsg = GeminiClient.queryGemini(queryPrompt, if (isEng) "Web Synthesizer" else "خلاصه‌گر وب فشرده و فصیح")
                                ttsManager.speak(synthesMsg)
                                addAssistantMessage(synthesMsg)
                            } else {
                                val localizedMsg = if (isEng) "Web search results: \n$ddgResult" else "نتیجه سرچ وب: \n$ddgResult"
                                ttsManager.speak(action.query)
                                addAssistantMessage(localizedMsg)
                            }
                            _state.value = AssistantState.Idle
                        }
                    }
                }
                is AssistantAction.SpeakResponse -> {
                    ttsManager.speak(action.speakText)
                    addAssistantMessage(action.displayMessage ?: action.speakText)
                    _state.value = AssistantState.Idle
                }
                is AssistantAction.QueryTasks -> {
                    _state.value = AssistantState.Processing
                    val searchingMsg = if (isEng) "Analyzing your registered tasks..." else "در حال بررسی قرارهای ثبت شده شما..."
                    addAssistantMessage(searchingMsg)
                    
                    externalScope.launch(Dispatchers.IO) {
                        try {
                            val allTasks = taskRepository.getAllTasks().first()
                            val tasksStr = if (allTasks.isEmpty()) {
                                if (isEng) "No tasks are registered in the local database." else "هیچ وظیفه‌ای در دیتابیس محلی ثبت نشده است."
                            } else {
                                allTasks.joinToString("\n") { task ->
                                    val dueString = if (task.dueDate != null) {
                                        com.example.core.utils.JalaliCalendar.formatIranDateTime(task.dueDate)
                                    } else {
                                        if (isEng) "No due date" else "بدون تاریخ"
                                    }
                                    "- ${task.title} ($dueString) - ${if (task.isCompleted) (if (isEng) "Completed" else "انجام شده") else (if (isEng) "Pending" else "انجام نشده")}"
                                }
                            }
                            
                            val synthesizePrompt = """
                                User asked: '${action.filterQuery}'
                                Below is the list of tasks inside the local database:
                                $tasksStr
                                
                                Provide a helpful, clear, highly accurate, and direct response to the user's question, strictly based on the real task items in the database.
                                Do not make up any other info or search online. Keep speech text short and readable in ${if (isEng) "English" else "Persian"}.
                            """.trimIndent()
                            
                            val synthesMsg = GeminiClient.queryGemini(synthesizePrompt, if (isEng) "Helper" else "راهنمای دیتابیس")
                            launch(Dispatchers.Main) {
                                ttsManager.speak(synthesMsg)
                                addAssistantMessage(synthesMsg)
                                _state.value = AssistantState.Idle
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "QueryTasks error", e)
                            launch(Dispatchers.Main) {
                                val err = if (isEng) "Failed to access local database." else "خطا در دسترسی به پایگاه‌داده محلی."
                                ttsManager.speak(err)
                                addAssistantMessage(err)
                                _state.value = AssistantState.Idle
                            }
                        }
                    }
                }
                is AssistantAction.QueryAlarms -> {
                    _state.value = AssistantState.Processing
                    val searchingMsg = if (isEng) "Checking your active alarms..." else "در حال بررسی هشدارهای فعال شما..."
                    addAssistantMessage(searchingMsg)
                    
                    externalScope.launch(Dispatchers.IO) {
                        try {
                            val allAlarms = alarmRepository.getAllAlarms().first()
                            val alarmsStr = if (allAlarms.isEmpty()) {
                                if (isEng) "No alarms are registered." else "هیچ هشدار بیداری ثبت نشده است."
                            } else {
                                allAlarms.joinToString("\n") { alarm ->
                                    "- ${alarm.label}: ${String.format("%02d:%02d", alarm.hour, alarm.minute)} (${if (alarm.isEnabled) (if (isEng) "Enabled" else "فعال") else (if (isEng) "Disabled" else "غیرفعال")})"
                                }
                            }
                            
                            val synthesizePrompt = """
                                User asked: '${action.filterQuery}'
                                Below is the list of alarms inside the phone's database:
                                $alarmsStr
                                
                                Provide a helpful, clear, highly accurate, and direct response to the user's question, strictly based on the real alarms in the database.
                                Keep speech text short and readable in ${if (isEng) "English" else "Persian"}.
                            """.trimIndent()
                            
                            val synthesMsg = GeminiClient.queryGemini(synthesizePrompt, if (isEng) "Helper" else "راهنمای دیتابیس")
                            launch(Dispatchers.Main) {
                                ttsManager.speak(synthesMsg)
                                addAssistantMessage(synthesMsg)
                                _state.value = AssistantState.Idle
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "QueryAlarms error", e)
                            launch(Dispatchers.Main) {
                                val err = if (isEng) "Failed to access alarm database." else "خطا در دسترسی به پایگاه‌داده هشدارها."
                                ttsManager.speak(err)
                                addAssistantMessage(err)
                                _state.value = AssistantState.Idle
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addAssistantMessage(text: String) {
        val assistMsg = ConversationMessage(text = text, isUser = false)
        _messages.value = _messages.value + assistMsg
    }
}
