package com.example.feature.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.core.notifications.NotificationHelper
import com.example.domain.model.AlarmItem
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val label = intent.getStringExtra("alarm_label") ?: ""
        val id = intent.getIntExtra("alarm_id", 0)
        Log.d("AlarmReceiver", "Alarm triggered: id=$id, label=$label")
        
        // Ensure channel exists and show heads-up alert with fullscreen trigger
        NotificationHelper.createNotificationChannel(context)
        NotificationHelper.showAlarmNotification(context, id, label)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val db = com.example.data.local.database.DailyFlowDatabase.getDatabase(context)
            val alarmEntity = db.alarmDao().getAlarmById(id)
            if (alarmEntity != null) {
                if (alarmEntity.isRecurring) {
                    val item = com.example.domain.model.AlarmItem(
                        id = alarmEntity.id,
                        label = alarmEntity.label,
                        hour = alarmEntity.hour,
                        minute = alarmEntity.minute,
                        isEnabled = alarmEntity.isEnabled,
                        isRecurring = alarmEntity.isRecurring,
                        triggerTimeMs = alarmEntity.triggerTimeMs,
                        createdAt = alarmEntity.createdAt
                    )
                    AlarmScheduler.scheduleAlarm(context, item)
                } else {
                    db.alarmDao().updateAlarm(alarmEntity.copy(isEnabled = false))
                }
            }
        }

        // Additionally launch activity directly as a dual backup approach
        try {
            val activityIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtra("alarm_id", id)
                putExtra("alarm_label", label)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(activityIntent)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Direct activity launching deferred to FullScreenIntent system", e)
        }
    }
}

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarm(context: Context, alarm: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_id", alarm.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        
        if (alarm.triggerTimeMs != null && !alarm.isRecurring) {
            // One-time precise alarm based on direct timestamp (so seconds match exactly)
            calendar.timeInMillis = alarm.triggerTimeMs
        } else {
            // Hour/Minute based logic
            calendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
            calendar.set(Calendar.MINUTE, alarm.minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
    
            // If time has elapsed today, delay scheduling to tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Successfully scheduled alarm ${alarm.id} at ${calendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Alarm scheduling error for id ${alarm.id}", e)
        }
    }

    fun cancelAlarm(context: Context, alarm: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Successfully cancelled alarm ${alarm.id}")
        }
    }
}
