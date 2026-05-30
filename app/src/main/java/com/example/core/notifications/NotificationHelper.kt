package com.example.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.feature.alarm.AlarmActivity

object NotificationHelper {
    private const val CHANNEL_ID = "dailyflow_alarms"
    private const val CHANNEL_NAME = "هشدارهای صوتی DailyFlow"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = " هشدارهای یادآور صوتی و اجرایی دستیار صوتی"
                enableVibration(true)
                setBypassDnd(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showAlarmNotification(context: Context, alarmId: Int, label: String) {
        // High-priority full screen intent pointing directly to AlarmActivity
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("alarm_label", label)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // General click fallback to main screen
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            alarmId + 100000,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("هشدار بیداری هوشمند")
            .setContentText(if (label.isEmpty()) "زمان زنگ هشدار فرارسیده است!" else label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true) // Launches AlarmActivity even when locked
            .setContentIntent(mainPendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(alarmId, builder.build())
    }
}
