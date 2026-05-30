package com.example.ai_engine.parser

import android.util.Log
import com.example.ai_engine.intent.AssistantIntent
import java.util.Calendar
import java.util.regex.Pattern

object LocalPersianParser {
    private const val TAG = "LocalPersianParser"

    // Persian numbers normalization helpers
    private val faNumbers = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val arNumbers = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    private val enNumbers = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    fun normalizeText(input: String?): String {
        if (input == null) return ""
        var text = input.trim()
        // Replace Persian/Arabic numbers with standard western digits for parsing ease
        for (i in 0..9) {
            text = text.replace(faNumbers[i], enNumbers[i])
            text = text.replace(arNumbers[i], enNumbers[i])
        }
        return text.lowercase()
    }

    /**
     * Attempts to parse standard relative Persian time phrases and returns time in Epoch MS.
     */
    fun parseRelativeTime(text: String): Long? {
        val normalized = normalizeText(text)
        val now = Calendar.getInstance()

        return when {
            normalized.contains("فردا صبح") -> {
                now.add(Calendar.DAY_OF_YEAR, 1)
                now.set(Calendar.HOUR_OF_DAY, 8)
                now.set(Calendar.MINUTE, 0)
                now.set(Calendar.SECOND, 0)
                now.set(Calendar.MILLISECOND, 0)
                now.timeInMillis
            }
            normalized.contains("یک دقیقه دیگه") || normalized.contains("1 دقیقه دیگه") -> {
                now.add(Calendar.MINUTE, 1)
                now.timeInMillis
            }
            normalized.contains("یک ربع دیگه") || normalized.contains("15 دقیقه دیگه") -> {
                now.add(Calendar.MINUTE, 15)
                now.timeInMillis
            }
            normalized.contains("نیم ساعت دیگه") || normalized.contains("30 دقیقه دیگه") -> {
                now.add(Calendar.MINUTE, 30)
                now.timeInMillis
            }
            normalized.contains("یک ساعت دیگه") -> {
                now.add(Calendar.HOUR_OF_DAY, 1)
                now.timeInMillis
            }
            normalized.contains("فردا شب") -> {
                now.add(Calendar.DAY_OF_YEAR, 1)
                now.set(Calendar.HOUR_OF_DAY, 21)
                now.set(Calendar.MINUTE, 0)
                now.set(Calendar.SECOND, 0)
                now.set(Calendar.MILLISECOND, 0)
                now.timeInMillis
            }
            // Parse custom hours: "3 ساعت دیگه" using regex
            else -> {
                val hourPattern = Pattern.compile("(\\d+)\\s*ساعت\\s*دیگه")
                val hourMatcher = hourPattern.matcher(normalized)
                if (hourMatcher.find()) {
                    val hours = hourMatcher.group(1)?.toIntOrNull() ?: 0
                    now.add(Calendar.HOUR_OF_DAY, hours)
                    return now.timeInMillis
                }

                val minPattern = Pattern.compile("(\\d+)\\s*دقیقه\\s*دیگه")
                val minMatcher = minPattern.matcher(normalized)
                if (minMatcher.find()) {
                    val minutes = minMatcher.group(1)?.toIntOrNull() ?: 0
                    now.add(Calendar.MINUTE, minutes)
                    return now.timeInMillis
                }

                null
            }
        }
    }

    /**
     * Triage user query. Returns AssistantIntent if local parsing holds high confidence, or null to trigger Gemini model fallback.
     */
    fun tryParseLocalIntent(query: String): AssistantIntent? {
        val normalized = normalizeText(query)
        Log.d(TAG, "Parsing text: '$normalized'")

        // 1. ALARM INTENT: Matches "زنگ هشدار", "زنگ بزن برای", "ساعت ... بیدارم کن", "ساعت ... هشدار بگذار"
        // Regex watch matching "زنگ هشدار برای ساعت [Hour]:[Minute]" or "ساعت [Hour]:[Minute] هشدار"
        val alarmTimePattern = Pattern.compile("ساعت\\s*(\\d+)(?:\\s*و\\s*|:)?(\\d+)?")
        if (normalized.contains("زنگ") || normalized.contains("هشدار") || normalized.contains("بیدار")) {
            val isRecurring = normalized.contains("هر روز") || normalized.contains("همه روزها") 
            
            // Handle relative alarm like "یک دقیقه دیگه هشدار بزار"
            val relativeTimeMs = parseRelativeTime(normalized)
            if (relativeTimeMs != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = relativeTimeMs }
                val labelPattern = Pattern.compile("به\\s*نام\\s*([^\\d\\n]+)")
                val labelMatcher = labelPattern.matcher(normalized)
                val label = labelMatcher.group(1)?.trim() ?: "هشدار نسبی"
                return AssistantIntent.SetAlarm(label, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), isRecurring, relativeTimeMs)
            }
            
            val matcher = alarmTimePattern.matcher(normalized)
            if (matcher.find()) {
                val hour = matcher.group(1)?.toIntOrNull() ?: 8
                val minute = matcher.group(2)?.toIntOrNull() ?: 0
                val labelPattern = Pattern.compile("به\\s*نام\\s*([^\\d\\n]+)")
                val labelMatcher = labelPattern.matcher(normalized)
                val label = labelMatcher.group(1)?.trim() ?: "هشدار"
                Log.d(TAG, "Parsed local Alarm: $hour:$minute label=$label")
                return AssistantIntent.SetAlarm(label, hour, minute, isRecurring, null)
            }
        }

        // 2. TASK INTENT: Matches "وظیفه", "اضافه کن کار", "یادداشت کن", "لیست انجام کار"
        // Examples: "وظیفه خرید نان فردا صبح", "کار تمیز کردن اتاق نیم ساعت دیگه"
        if (normalized.contains("وظیفه") || normalized.contains("اضافه کن کار") || normalized.contains("یادداشت کن") || (normalized.contains("کار") && normalized.contains("اضافه"))) {
            // Strip structural triggers from task text
            var title = query.replace(Regex("(?i)(وظیفه|اضافه کن کار|یادداشت کن|کار|اضافه کن|دیگه|فردا صبح|فردا شب|یک ربع دیگه|نیم ساعت دیگه|۳ ساعت دیگه|یک ساعت دیگه)"), "").trim()
            if (title.isEmpty()) {
                title = "کار جدید صوتی"
            }
            val dueDate = parseRelativeTime(normalized)
            Log.d(TAG, "Parsed local Task: title='$title', dueDate=$dueDate")
            return AssistantIntent.CreateTask(title, dueDate)
        }

        // 3. SMS INTENT: Matches "پیامک به Ali", "اس ام اس به علی بگو سلام"
        // "پیامک به [NAME] بگو [MSG]" or "اس ام اس به [NAME] [MSG]"
        if (normalized.contains("پیامک") || normalized.contains("اس ام اس") || normalized.contains("پیام به")) {
            val smsPattern = Pattern.compile("(?:پیامک|اس ام اس|پیام)?\\s*به\\s*([^\\s]+)\\s*(?:بگو|متن|بفرست)?\\s*(.+)?")
            val matcher = smsPattern.matcher(normalized)
            if (matcher.find()) {
                val recipient = matcher.group(1)?.trim() ?: ""
                val message = matcher.group(2)?.trim() ?: "من در مسیر هستم"
                if (recipient.isNotEmpty()) {
                    Log.d(TAG, "Parsed local SMS: recipient=$recipient message=$message")
                    return AssistantIntent.SendSms(recipient, message)
                }
            }
        }

        // 4. PHONE CALL INTENT: Matches "تماس با", "شماره گیری", "زنگ بزن به"
        if (normalized.contains("تماس با") || normalized.contains("زنگ بزن به")) {
            val callPattern = Pattern.compile("(?:تماس با|زنگ بزن به)\\s*([^\\s]+)")
            val matcher = callPattern.matcher(normalized)
            if (matcher.find()) {
                val recipient = matcher.group(1)?.trim() ?: ""
                if (recipient.isNotEmpty()) {
                    Log.d(TAG, "Parsed local Phone Call: recipient=$recipient")
                    return AssistantIntent.MakeCall(recipient)
                }
            }
        }

        // 5. WEB SEARCH INTENT: Matches "جستجو کن", "سرچ کن", "چیست", "کیست"
        if (normalized.contains("جستجو کن") || normalized.contains("سرچ کن") || normalized.endsWith("چیست") || normalized.endsWith("کیست")) {
            val searchQuery = query.replace(Regex("(?i)(جستجو کن|سرچ کن|چیست|کیست)"), "").trim()
            if (searchQuery.isNotEmpty()) {
                Log.d(TAG, "Parsed local WebSearch: query=$searchQuery")
                return AssistantIntent.WebSearch(searchQuery)
            }
        }

        // None of the rules met with high confidence -> Fallback return null to allow Gemini AI analysis
        Log.d(TAG, "Local NLP Parser low confidence. Delegated to Gemini fallback.")
        return null
    }
}
