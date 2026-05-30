package com.example.feature.home

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai_engine.orchestrator.AssistantState
import com.example.core.ui.VoiceOrb
import com.example.domain.model.AlarmItem
import com.example.domain.model.ConversationMessage
import com.example.domain.model.Task
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTasks: () -> Unit = {}
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val lastSpeechError by viewModel.lastSpeechError.collectAsStateWithLifecycle()

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val voiceLanguage by viewModel.voiceLanguage.collectAsStateWithLifecycle()

    val isDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            hour < 6 || hour >= 18
        }
    }

    val DeepSpaceBackground = if (isDark) Color(0xFF05070A) else Color(0xFFF1F5F9)
    val SecondarySpaceBackground = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val SoftWhite = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val MutedTextSpace = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val BorderSpaceGray = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    var textInputState by remember { mutableStateOf("") }
    var showSettingsState by remember { mutableStateOf(false) }
    var quickTaskTitle by remember { mutableStateOf("") }

    val formattedDate = remember {
        val now = System.currentTimeMillis()
        val jDate = com.example.core.utils.JalaliCalendar.getJalaliDate(now)
        val dayName = com.example.core.utils.JalaliCalendar.getPersianDayName(now)
        val monthName = com.example.core.utils.JalaliCalendar.getPersianMonthName(jDate.month)
        "$dayName، ${jDate.day} $monthName ${jDate.year}"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    val indicatorColor = if (isRecording || assistantState is AssistantState.Listening) {
                        CosmicNeonPink
                    } else if (assistantState is AssistantState.Processing) {
                        CosmicNeonCyan
                    } else if (assistantState is AssistantState.AwaitingConfirmation) {
                        CosmicNeonPurple
                    } else {
                        MutedTextSpace.copy(alpha = 0.5f)
                    }

                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isRecording || assistantState !is AssistantState.Idle) 1.5f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp * scale)
                                .clip(CircleShape)
                                .background(indicatorColor.copy(alpha = 0.3f))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(indicatorColor)
                        )
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DailyFlow",
                            fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = SoftWhite,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = formattedDate,
                            fontSize = 12.sp,
                            color = MutedTextSpace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ImmersiveViolet, ImmersiveFuchsia)
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            .clickable { showSettingsState = true }
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "تنظیمات صوتی",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepSpaceBackground
                )
            )
        },
        containerColor = DeepSpaceBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp), // Auto-align responsive desktop tablet sizes
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // 1. WEEKLY COMPACT INTERACTIVE CALENDAR
            item {
                SlidingCompactCalendar(
                    tasks = tasks,
                    alarms = alarms,
                    isDark = isDark
                )
            }

            // 2. MAIN CONVERSATION SCREEN (Glassmorphic Persian logs)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondarySpaceBackground),
                    border = CardStrokeSpace(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .testTag("chat_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Scrollable chat transcript
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            LazyColumn(
                                reverseLayout = true, // keeps the latest message visible
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(messages.reversed()) { message ->
                                    ConversationRow(message)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Text typing / Manual instruction row as backup entry
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepSpaceBackground, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderSpaceGray, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (textInputState.isNotBlank()) {
                                        viewModel.submitSimulatedVoiceText(textInputState)
                                        textInputState = ""
                                    }
                                },
                                modifier = Modifier.testTag("send_text_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "فرستادن متن",
                                    tint = CosmicNeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            TextField(
                                value = textInputState,
                                onValueChange = { textInputState = it },
                                placeholder = {
                                    Text(
                                        "دستور متنی یا پاسخ بنویسید...",
                                        fontSize = 12.sp,
                                        color = MutedTextSpace,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Right
                                ),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("text_input_field")
                            )
                        }
                    }
                }
            }

            // Interactive Confirmation Sheet (Action System strictly matching design HTML)
            if (assistantState is AssistantState.AwaitingConfirmation) {
                item {
                    ConfirmationSheet(
                        state = assistantState as AssistantState.AwaitingConfirmation,
                        onConfirm = { viewModel.submitSimulatedVoiceText("بله") },
                        onDecline = { viewModel.submitSimulatedVoiceText("خیر") },
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }
            }

            // 3. KEY INTERACTIVE WIDGET: FLOATING VOICE ORB
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    VoiceOrb(
                        state = assistantState,
                        onClick = { viewModel.toggleVoiceListening() }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onNavigateToTasks,
                        modifier = Modifier.padding(16.dp).fillMaxWidth()
                    ) {
                        Text("مشاهده تسک‌ها و آلارم‌ها")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Context status text reflecting Voice State in Persian RTL
                    val speakStatus = when (assistantState) {
                        is AssistantState.Listening -> "گوش دادن فعال صوتی... صحبت کنید"
                        is AssistantState.Processing -> "در حال پردازش هوشمند..."
                        is AssistantState.AwaitingConfirmation -> "منتظر تایید صوتی شما... (بله / خیر)"
                        else -> "دکمه صوتی را برای گفتار لمس کنید"
                    }

                    Text(
                        text = speakStatus,
                        fontSize = 13.sp,
                        color = if (assistantState is AssistantState.Listening) CosmicNeonCyan else if (assistantState is AssistantState.AwaitingConfirmation) CosmicNeonPink else SoftWhite,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    lastSpeechError?.let { err ->
                        Text(
                            text = "سیستم: $err (لطفا از لیست شبیه‌ساز صوتی پایین استفاده کنید)",
                            fontSize = 11.sp,
                            color = CosmicNeonPink.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 4. LOCAL TASKS LIST BOARD
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondarySpaceBackground),
                    border = CardStrokeSpace(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "کارهای در دست انجام (${tasks.count { !it.isCompleted }})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicNeonPurple
                            )

                            if (tasks.any { it.isCompleted }) {
                                TextButton(
                                    onClick = { viewModel.clearCompletedTasks() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("پاکسازی انجام شده‌ها", fontSize = 13.sp, color = CosmicNeonPink)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick manual list task addition
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(DeepSpaceBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderSpaceGray, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (quickTaskTitle.isNotBlank()) {
                                        viewModel.insertTaskDirectly(quickTaskTitle)
                                        quickTaskTitle = ""
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "تعریف کار",
                                    tint = CosmicNeonPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            TextField(
                                value = quickTaskTitle,
                                onValueChange = { quickTaskTitle = it },
                                placeholder = {
                                    Text(
                                        "افزودن کار جدید...",
                                        fontSize = 13.sp,
                                        color = MutedTextSpace,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Right
                                ),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (tasks.isEmpty()) {
                            Text(
                                text = "لیست کارهای امروز خالی است. برنامه‌هایتان را ثبت کنید.",
                                fontSize = 13.sp,
                                color = MutedTextSpace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                tasks.forEach { task ->
                                }
                            }
                        }
                    }
                }
            }

            // 5. ALARM AND REMINDER SYSTEM BOARD
            item {
                Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SecondarySpaceBackground),
                        border = CardStrokeSpace(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "هشدارهای بیداری صوتی",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicNeonCyan,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            if (alarms.isEmpty()) {
                                Text(
                                    text = "آلارمی ثبت نشده است (روال را با تلفظ صوتی ثبت کنید)",
                                    fontSize = 13.sp,
                                    color = MutedTextSpace,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    alarms.forEach { alarm ->
                                        AlarmItemRow(
                                            alarm = alarm,
                                            onToggled = { viewModel.toggleAlarmState(alarm) },
                                            onDelete = { viewModel.removeAlarm(alarm) }
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }

    // Settings details popup bottom card
    if (showSettingsState) {
        AlertDialog(
            onDismissRequest = { showSettingsState = false },
            confirmButton = {
                TextButton(onClick = { showSettingsState = false }) {
                    Text("بستن صفحه", color = CosmicNeonPurple, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = "اطلاعات سیستم و حریم خصوصی",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "دستیار DailyFlow با ۷۰ درصد منطق محلی مستقل بر روی گوشی شما اجرا می‌شود.",
                        color = SoftWhite,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(color = BorderSpaceGray)

                    // Language Selection Section (Interactive)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (voiceLanguage == "en") CosmicNeonPurple else BorderSpaceGray)
                                    .clickable { viewModel.setVoiceLanguage("en") }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("English", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (voiceLanguage == "fa") CosmicNeonPurple else BorderSpaceGray)
                                    .clickable { viewModel.setVoiceLanguage("fa") }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("فارسی", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(text = "زبان سیستم صوتی:", color = MutedTextSpace, fontSize = 12.sp)
                    }
                    
                    Divider(color = BorderSpaceGray)
                    
                    val prefs = LocalContext.current.getSharedPreferences("dailyflow_prefs", android.content.Context.MODE_PRIVATE)
                    var snoozeDur by remember { mutableStateOf(prefs.getInt("snooze_duration", 5)) }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 5, 10, 15).forEach { min ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (snoozeDur == min) CosmicNeonCyan else BorderSpaceGray)
                                        .clickable { 
                                            snoozeDur = min
                                            prefs.edit().putInt("snooze_duration", min).apply()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("$min دقیقه", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(text = "زمان تعویق (Snooze):", color = MutedTextSpace, fontSize = 12.sp)
                    }

                    // Theme Selector Section (Interactive)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (themeMode == "auto") CosmicNeonCyan else BorderSpaceGray)
                                    .clickable { viewModel.setThemeMode("auto") }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("ساعتی", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (themeMode == "dark") CosmicNeonCyan else BorderSpaceGray)
                                    .clickable { viewModel.setThemeMode("dark") }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("تاریک", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (themeMode == "light") CosmicNeonCyan else BorderSpaceGray)
                                    .clickable { viewModel.setThemeMode("light") }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("روشن", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(text = "قالب هوشمند:", color = MutedTextSpace, fontSize = 12.sp)
                    }

                    Divider(color = BorderSpaceGray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (com.example.data.remote.gemini.GeminiClient.isApiKeyAvailable) "متصل (فعال)" else "محلی (بدون کلید)",
                            color = if (com.example.data.remote.gemini.GeminiClient.isApiKeyAvailable) CosmicNeonCyan else CosmicNeonPink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "پروکسی مدل جمینی:", color = MutedTextSpace, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "نسخه ۱.۰.۰", color = SoftWhite, fontSize = 12.sp)
                        Text(text = "نگارش برنامه:", color = MutedTextSpace, fontSize = 12.sp)
                    }

                    Text(
                        text = "امنیت: پرداخت‌های بانکی، پیامک‌ها و هشدارهای بیداری صوتی فقط محلی پردازش می‌شوند.",
                        color = MutedTextSpace,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            },
            containerColor = SecondarySpaceBackground,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("settings_dialog")
        )
    }
}

@Composable
fun ConversationRow(message: ConversationMessage) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val DeepSpaceBackground = if (isDark) Color(0xFF05070A) else Color(0xFFF1F5F9)
    val BorderSpaceGray = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bg = if (message.isUser) {
        Brush.linearGradient(colors = listOf(CosmicNeonPurple, CosmicNeonPurple.copy(alpha = 0.85f)))
    } else {
        Brush.linearGradient(colors = listOf(BorderSpaceGray, DeepSpaceBackground))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (message.isUser) 12.dp else 0.dp,
                            bottomEnd = if (message.isUser) 0.dp else 12.dp
                        )
                    )
                    .background(bg)
                    .border(
                        1.dp,
                        if (message.isUser) Color.Transparent else BorderSpaceGray,
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (message.isUser) 12.dp else 0.dp,
                            bottomEnd = if (message.isUser) 0.dp else 12.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 12.sp,
                    color = if (message.isUser) Color.White else (if (isDark) Color.White else Color(0xFF0F172A)),
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

data class ActionInfo(val actionTitle: String, val detailTitle: String, val contentText: String, val icon: ImageVector)

@Composable
fun TaskItemRow(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val DeepSpaceBackground = if (isDark) Color(0xFF05070A) else Color(0xFFF1F5F9)
    val BorderSpaceGray = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val MutedTextSpace = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DeepSpaceBackground)
            .border(1.dp, BorderSpaceGray, RoundedCornerShape(10.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف تسک",
                    tint = CosmicNeonPink.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                ) {
                    Text(
                        text = task.title,
                        fontSize = 13.sp, // slightly bigger
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isCompleted) MutedTextSpace else (if (isDark) Color.White else Color(0xFF0F172A)),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (task.dueDate != null) {
                        Text(
                            text = "هشدار تنظیم شده",
                            fontSize = 11.sp, // slightly bigger
                            color = CosmicNeonPurple,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = CosmicNeonPurple,
                        uncheckedColor = MutedTextSpace,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Expanded Details
        if (isExpanded) {
            HorizontalDivider(color = BorderSpaceGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "جزئیات ثبت:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MutedTextSpace,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "زمان ایجاد: ${com.example.core.utils.JalaliCalendar.formatIranDateTime(task.createdAt)}",
                fontSize = 11.sp,
                color = if (isDark) SoftWhite else Color.DarkGray,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AlarmItemRow(
    alarm: AlarmItem,
    onToggled: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val DeepSpaceBackground = if (isDark) Color(0xFF05070A) else Color(0xFFF1F5F9)
    val BorderSpaceGray = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val MutedTextSpace = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DeepSpaceBackground)
            .border(1.dp, BorderSpaceGray, RoundedCornerShape(10.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف آلارم",
                        tint = CosmicNeonPink.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CosmicNeonCyan,
                        uncheckedThumbColor = MutedTextSpace,
                        uncheckedTrackColor = BorderSpaceGray
                    ),
                    modifier = Modifier.scale(0.82f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        text = alarm.label.ifEmpty { "روال بیداری" },
                        fontSize = 13.sp, // bigger
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = if (alarm.isRecurring) "هر روز" else "فقط یک بار",
                        fontSize = 10.sp, // bigger
                        color = if (alarm.isEnabled) CosmicNeonCyan else MutedTextSpace
                    )
                }

                Text(
                    text = alarm.formattedTime.toString(),
                    fontSize = 20.sp, // bigger
                    fontWeight = FontWeight.Black,
                    color = if (alarm.isEnabled) CosmicNeonCyan else MutedTextSpace,
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily
                )
            }
        }
        
        if (isExpanded) {
            HorizontalDivider(color = BorderSpaceGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "جزئیات هشدار:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MutedTextSpace,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (alarm.triggerTimeMs != null) "زمان دقیق اجرا: ${com.example.core.utils.JalaliCalendar.formatIranDateTime(alarm.triggerTimeMs)}" else "زمان تنظیم: ${alarm.formattedTime}",
                fontSize = 11.sp,
                color = if (isDark) SoftWhite else Color.DarkGray,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CardStrokeSpace() = androidx.compose.foundation.BorderStroke(1.dp, BorderSpaceGray)

@Composable
fun ConfirmationSheet(
    state: AssistantState.AwaitingConfirmation,
    onConfirm: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val ImmersiveSlate900 = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val SoftWhite = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val ImmersiveTextSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val ImmersiveTextCardSec = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)

    val action = state.action
    
    // Extrapolate Iranian title & styling based on the assistant action
    val actionInfo = when (action) {
        is com.example.ai_engine.intent.AssistantAction.SendSms -> {
            ActionInfo(
                "تأیید پیامک",
                "گیرنده: ${action.recipient}",
                "«${action.message}»",
                Icons.AutoMirrored.Filled.Send
            )
        }
        is com.example.ai_engine.intent.AssistantAction.CreateTask -> {
            ActionInfo(
                "تأیید تعریف کار",
                "کار جدید در دست انجام",
                "«${action.title}»" + (if (action.dueDateMs != null) " - با فرجه زمان مشخص شده" else ""),
                Icons.Default.CheckCircle
            )
        }
        is com.example.ai_engine.intent.AssistantAction.CreateAlarm -> {
            ActionInfo(
                "تأیید تنظیم زنگ هشدار",
                "عنوان روال بیداری: ${action.label.ifEmpty { "بیدارباش صبحگاهی" }}",
                "ساعت آلارم صوتی: ${action.hour}:${String.format("%02d", action.minute)}",
                Icons.Default.Notifications
            )
        }
        is com.example.ai_engine.intent.AssistantAction.MakeCall -> {
            ActionInfo(
                "تأیید تماس صوتی",
                "مخاطب: ${action.recipientName}",
                "برقراری تماس با شماره: ${action.phoneNumber}",
                Icons.Default.Call
            )
        }
        is com.example.ai_engine.intent.AssistantAction.WebSearch -> {
            ActionInfo(
                "تأیید جستجو در وب",
                "موتور جستجوی امن DuckDuckGo",
                "عبارت جستجو شده: «${action.query}»",
                Icons.Default.Search
            )
        }
        is com.example.ai_engine.intent.AssistantAction.SpeakResponse -> {
            ActionInfo(
                "تأیید پیام دستیار",
                "پاسخ صوتی گوینده",
                "«${action.speakText}»",
                Icons.Default.PlayArrow
            )
        }
        else -> {
            ActionInfo(
                "بررسی پایگاه‌داده",
                "دریافت اطلاعات محلی",
                "در حال بازیابی وظایف و آلارم‌ها...",
                Icons.Default.Search
            )
        }
    }

    val (actionTitle, detailTitle, contentText, icon) = actionInfo

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ImmersiveSlate900.copy(alpha = 0.82f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("action_confirmation_sheet")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row: Icon + Type metadata
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Texts in Persian (RTL layout)
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = actionTitle.uppercase(),
                        fontSize = 10.sp,
                        color = ImmersiveTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = detailTitle,
                        fontSize = 14.sp,
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Rounded Icon Container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ImmersiveViolet.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ImmersiveViolet,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main bubble-content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = contentText,
                    fontSize = 13.sp,
                    color = ImmersiveTextCardSec,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm & Cancel Buttons Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Cancel Button
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ImmersiveSlate800)
                        .testTag("decline_action_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "انصراف",
                        tint = ImmersiveTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Confirm / Positive Button
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveViolet),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("confirm_action_button")
                ) {
                    Text(
                        text = when (action) {
                            is com.example.ai_engine.intent.AssistantAction.SendSms -> "ارسال پیام"
                            is com.example.ai_engine.intent.AssistantAction.CreateTask -> "ثبت کار"
                            is com.example.ai_engine.intent.AssistantAction.CreateAlarm -> "تنظیم روال"
                            else -> "تأیید و انجام"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Simple quad helper class
data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun SlidingCompactCalendar(
    tasks: List<Task>,
    alarms: List<AlarmItem>,
    isDark: Boolean,
    onDaySelected: (Date) -> Unit = {}
) {
    var selectedDay by remember { mutableStateOf(Date()) }
    
    // Generate precisely today and 4 days after (total of 5 days) using Asia/Tehran timezone
    val days = remember {
        (0..4).map { offset ->
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
            cal.add(Calendar.DAY_OF_YEAR, offset)
            cal.time
        }
    }

    val SoftWhite = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val MutedTextSpace = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val BorderSpaceGray = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تقویم هوشمند خلاصه",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SoftWhite
            )
            Text(
                text = "وضعیت زمان‌بندی امروز",
                fontSize = 12.sp,
                color = CosmicNeonPurple,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            days.forEach { date ->
                val isToday = remember(date) {
                    val tDate = com.example.core.utils.JalaliCalendar.getJalaliDate(System.currentTimeMillis())
                    val dDate = com.example.core.utils.JalaliCalendar.getJalaliDate(date.time)
                    tDate.year == dDate.year && tDate.month == dDate.month && tDate.day == dDate.day
                }

                val isSelected = remember(selectedDay, date) {
                    val sDate = com.example.core.utils.JalaliCalendar.getJalaliDate(selectedDay.time)
                    val dDate = com.example.core.utils.JalaliCalendar.getJalaliDate(date.time)
                    sDate.year == dDate.year && sDate.month == dDate.month && sDate.day == dDate.day
                }

                // Check for tasks on this specific day
                val hasTaskOnDay = remember(tasks, date) {
                    val dDate = com.example.core.utils.JalaliCalendar.getJalaliDate(date.time)
                    tasks.any { t ->
                        t.dueDate?.let { dueMs ->
                            val tDate = com.example.core.utils.JalaliCalendar.getJalaliDate(dueMs)
                            tDate.year == dDate.year && tDate.month == dDate.month && tDate.day == dDate.day
                        } ?: false
                    }
                }

                val dayName = remember(date) {
                    com.example.core.utils.JalaliCalendar.getPersianDayName(date.time)
                }
                val dayOfMonth = remember(date) {
                    com.example.core.utils.JalaliCalendar.getJalaliDate(date.time).day.toString()
                }

                // Modern glass blocks
                val bgBrush = when {
                    isSelected -> Brush.linearGradient(colors = listOf(CosmicNeonPurple, CosmicNeonCyan))
                    isToday -> Brush.linearGradient(colors = listOf(BorderSpaceGray.copy(alpha = 0.5f), BorderSpaceGray.copy(alpha = 0.1f)))
                    hasTaskOnDay -> Brush.linearGradient(colors = listOf(CosmicNeonPink.copy(alpha = 0.15f), Color.Transparent))
                    else -> Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                }

                val cardBorder = when {
                    isSelected -> BorderStroke(1.2.dp, Color.White.copy(alpha = 0.8f))
                    hasTaskOnDay -> BorderStroke(1.5.dp, CosmicNeonPink.copy(alpha = 0.8f)) // Highlight days with tasks!
                    isToday -> BorderStroke(1.5.dp, CosmicNeonCyan.copy(alpha = 0.7f))
                    else -> BorderStroke(1.dp, BorderSpaceGray)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgBrush)
                        .border(cardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            selectedDay = date
                            onDaySelected(date)
                        }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val hasAlarmOnDay = remember(alarms, date) {
                        val dDate = com.example.core.utils.JalaliCalendar.getJalaliDate(date.time)
                        alarms.any { alarm ->
                            if (!alarm.isEnabled) return@any false
                            if (alarm.isRecurring) {
                                true
                            } else {
                                val triggerMs = if (alarm.triggerTimeMs != null) {
                                    alarm.triggerTimeMs
                                } else {
                                    val cal = java.util.Calendar.getInstance()
                                    cal.set(java.util.Calendar.HOUR_OF_DAY, alarm.hour)
                                    cal.set(java.util.Calendar.MINUTE, alarm.minute)
                                    cal.set(java.util.Calendar.SECOND, 0)
                                    cal.set(java.util.Calendar.MILLISECOND, 0)
                                    if (cal.timeInMillis <= System.currentTimeMillis()) {
                                        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                                    }
                                    cal.timeInMillis
                                }
                                val tDate = com.example.core.utils.JalaliCalendar.getJalaliDate(triggerMs)
                                tDate.year == dDate.year && tDate.month == dDate.month && tDate.day == dDate.day
                            }
                        }
                    }

                    Text(
                        text = dayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isSelected -> Color.White
                            hasTaskOnDay -> CosmicNeonPink
                            isToday -> CosmicNeonCyan
                            else -> MutedTextSpace
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = dayOfMonth,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isSelected -> Color.White
                            isToday -> CosmicNeonCyan
                            else -> SoftWhite
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasTaskOnDay) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else CosmicNeonPink)
                            )
                        }
                        if (hasAlarmOnDay) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else CosmicNeonCyan)
                            )
                        }
                    }
                }
            }
        }
    }
}

