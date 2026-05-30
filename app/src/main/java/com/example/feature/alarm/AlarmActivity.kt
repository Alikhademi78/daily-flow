package com.example.feature.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.domain.model.AlarmItem
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AlarmActivity", "AlarmActivity onCreate launched!")

        // 1. Force screen on & show over lockscreen
        turnScreenOnAndShowOnLockScreen()

        // 2. Extrapolate intent alarm details
        val alarmId = intent.getIntExtra("alarm_id", (1000..9999).random())
        val alarmLabel = intent.getStringExtra("alarm_label") ?: "روال بیداری صوتی"
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        notificationManager?.cancel(alarmId)

        // 3. Start high-priority sound and vibration
        startAlarmFeedback()

        // 4. Render beautiful glassmorphic dark interface
        setContent {
            MyApplicationTheme(darkTheme = true) {
                AlarmScreen(
                    label = alarmLabel,
                    onStop = {
                        stopAlarmFeedback()
                        finish()
                    },
                    onSnooze = {
                        snoozeAlarm(alarmId, alarmLabel)
                        stopAlarmFeedback()
                        finish()
                    }
                )
            }
        }
    }

    private fun turnScreenOnAndShowOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startAlarmFeedback() {
        try {
            // A. Play Looping Alarm Sound
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmActivity, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Failed playing custom alarm ringtone", e)
        }

        try {
            // B. Play Continuous Vibration
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 800, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Failed starting vibration feedback", e)
        }
    }

    private fun snoozeAlarm(id: Int, label: String) {
        val prefs = getSharedPreferences("dailyflow_prefs", Context.MODE_PRIVATE)
        val snoozeAmt = prefs.getInt("snooze_duration", 5)
        
        // Schedule alarm in snoozeAmt minutes
        val snoozeCal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, snoozeAmt)
        }
        val newId = id + (10000..99999).random() // Ensure unique ID so FullScreenIntent is guaranteed to trigger again
        val item = com.example.domain.model.AlarmItem(
            id = newId,
            label = "$label (اسنوز شده)",
            hour = snoozeCal.get(Calendar.HOUR_OF_DAY),
            minute = snoozeCal.get(Calendar.MINUTE),
            isEnabled = true,
            isRecurring = false,
            triggerTimeMs = snoozeCal.timeInMillis
        )
        
        // Save to Database so user can see it in UI and it's robust
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val db = com.example.data.local.database.DailyFlowDatabase.getDatabase(this@AlarmActivity)
            db.alarmDao().insertAlarm(com.example.data.local.entities.AlarmEntity(
                id = item.id,
                label = item.label,
                hour = item.hour,
                minute = item.minute,
                isEnabled = item.isEnabled,
                isRecurring = item.isRecurring,
                triggerTimeMs = item.triggerTimeMs,
                createdAt = System.currentTimeMillis()
            ))
            com.example.feature.alarm.AlarmScheduler.scheduleAlarm(this@AlarmActivity, item)
            Log.d("AlarmActivity", "Snoozed alarm $id -> new id $newId in $snoozeAmt minutes")
        }
    }

    private fun stopAlarmFeedback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Error stopping player", e)
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Error stopping vibrator", e)
        }
    }

    override fun onDestroy() {
        stopAlarmFeedback()
        super.onDestroy()
    }
}

@Composable
fun AlarmScreen(
    label: String,
    onStop: () -> Unit,
    onSnooze: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AlarmTransitions")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlarmPulse"
    )

    val currentFormattedTime = remember {
        val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tehran"))
        val timeEn = String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        val faNumbers = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        timeEn.map { char ->
            if (char in '0'..'9') faNumbers[char - '0'] else char
        }.joinToString("")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712)) // Dark sleek background
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Atmospheric gradient blobs behind the clock
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(pulseScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x22EF4444), // glowing soft red/pink warning shade
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
        ) {
            // 1. Sleek Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = "هشدار یادآور هوشمند",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF43F5E), // Rose-500
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "DailyFlow Assistant",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9CA3AF)
                )
            }

            // 2. Giant Glowing Time and Alarm Icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFF43F5E), Color(0xFFE11D48))
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Text(
                    text = currentFormattedTime,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                // Glassmorphic Alarm Box displaying details
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "رویداد: $label",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Command Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // STOP BUTTON: High contrast modern button
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444) // Solid eye-catching Red
                    ),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(58.dp)
                ) {
                    Text(
                        text = "توقف هشدار / STOP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                val ctx = androidx.compose.ui.platform.LocalContext.current
                val prefs = ctx.getSharedPreferences("dailyflow_prefs", Context.MODE_PRIVATE)
                val snoozeAmt = prefs.getInt("snooze_duration", 5)

                // SNOOZE BUTTON: Slate glow outline button
                OutlinedButton(
                    onClick = onSnooze,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(54.dp)
                ) {
                    Text(
                        text = "به تعویق انداختن ($snoozeAmt دقیقه) / SNOOZE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD1D5DB)
                    )
                }
            }
        }
    }
}
