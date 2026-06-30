package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Chapter
import com.example.data.Flashcard
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.Message
import com.example.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.*

// --- Cohesive Gamified Color Scheme ---
val CozySlateDark = Color(0xFF121420)
val CosmicIndigo = Color(0xFF2C3056)
val ScholarPurple = Color(0xFF8A6DFD)
val AccentAmber = Color(0xFFFFB627)
val SuccessEmerald = Color(0xFF10B981)
val CardBackground = Color(0xFF1E2235)
val DividerColor = Color(0xFF2D324F)

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val progress by viewModel.userProgress.collectAsState(initial = null)
    val userEmail by viewModel.firebaseUserEmail.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CozySlateDark,
                contentColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen is Screen.Dashboard,
                    onClick = { viewModel.currentScreen.value = Screen.Dashboard },
                    label = { Text("Dashboard", color = Color.White) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard", tint = if (currentScreen is Screen.Dashboard) ScholarPurple else Color.Gray) },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.ProfessorChat,
                    onClick = { viewModel.currentScreen.value = Screen.ProfessorChat },
                    label = { Text("Professor", color = Color.White) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "AI Professor", tint = if (currentScreen is Screen.ProfessorChat) ScholarPurple else Color.Gray) },
                    modifier = Modifier.testTag("nav_professor")
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Flashcards,
                    onClick = { viewModel.currentScreen.value = Screen.Flashcards },
                    label = { Text("Cards", color = Color.White) },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Flashcards", tint = if (currentScreen is Screen.Flashcards) ScholarPurple else Color.Gray) },
                    modifier = Modifier.testTag("nav_cards")
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Scheduler,
                    onClick = { viewModel.currentScreen.value = Screen.Scheduler },
                    label = { Text("Schedule", color = Color.White) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Planner", tint = if (currentScreen is Screen.Scheduler) ScholarPurple else Color.Gray) },
                    modifier = Modifier.testTag("nav_scheduler")
                )
            }
        },
        containerColor = CozySlateDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                is Screen.Dashboard -> DashboardScreen(viewModel)
                is Screen.ProfessorChat -> ProfessorChatScreen(viewModel)
                is Screen.Flashcards -> FlashcardReviewScreen(viewModel)
                is Screen.ExamSimulator -> ExamSimulatorScreen(viewModel)
                is Screen.Scheduler -> SchedulerScreen(viewModel)
                is Screen.StudySpaceFinder -> StudySpaceFinderScreen(viewModel)
                is Screen.VideoExplainer -> VideoExplainerScreen(viewModel)
                is Screen.Visualizer -> VisualizerScreen(viewModel)
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD VIEW
// ==========================================
@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    val progress by viewModel.userProgress.collectAsState(initial = null)
    val chaptersList by viewModel.chapters.collectAsState()
    val userEmail by viewModel.firebaseUserEmail.collectAsState()
    val scrollState = rememberScrollState()

    var showAddChapterDialog by remember { mutableStateOf(false) }
    var context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(CozySlateDark)
            .padding(16.dp)
    ) {
        // --- Custom App Banner Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Brilliant Study",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (userEmail != null) "Logged in: $userEmail" else "Local Offline Mode",
                    fontSize = 13.sp,
                    color = Color.LightGray
                )
            }

            // Sync/User Profile Badge
            Button(
                onClick = {
                    if (userEmail == null) {
                        viewModel.handleFirebaseSignIn()
                    } else {
                        viewModel.handleFirebaseSignOut()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (userEmail != null) ScholarPurple else Color(0xFF2C3056)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (userEmail != null) Icons.Default.Check else Icons.Default.Lock,
                    contentDescription = "Sync Badge",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (userEmail != null) "Synced" else "Sign In",
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }

        // --- Gamified Milestones (Streak & XP) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Streak Widget
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔥",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${progress?.streakCount ?: 0} Day Streak",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Study daily to grow",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // XP Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentAmber)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${progress?.totalXp ?: 0} XP",
                            color = CozySlateDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Reward Claim Button
                Button(
                    onClick = {
                        viewModel.collectDailyReward(
                            onSuccess = { msg ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { err ->
                                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_reward_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ScholarPurple),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("🎁 Claim Daily Reward (+150 XP)", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }

        // --- Weekly Performance Analytics Chart ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Weekly Study Analytics",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Daily focus milestone breakdown (Mon - Sun)",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Custom Canvas drawing for the analytics chart
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    // Custom height values for days
                    val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.3f, 0.8f, 0.6f)
                    val width = size.width
                    val height = size.height
                    val barWidth = 24.dp.toPx()
                    val spacing = (width - (barWidth * days.size)) / (days.size + 1)

                    // Draw baseline
                    drawLine(
                        color = DividerColor,
                        start = Offset(0f, height),
                        end = Offset(width, height),
                        strokeWidth = 2.dp.toPx()
                    )

                    for (i in days.indices) {
                        val x = spacing + i * (barWidth + spacing)
                        val barHeight = heights[i] * height
                        val y = height - barHeight

                        // Draw background bar track
                        drawRect(
                            color = DividerColor,
                            topLeft = Offset(x, 0f),
                            size = androidx.compose.ui.geometry.Size(barWidth, height)
                        )

                        // Draw filled progress bar
                        drawRect(
                            color = if (i == 3) AccentAmber else ScholarPurple,
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    days.forEach {
                        Text(it, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // --- Fast Launcher Navigation Widgets (Top tier design) ---
        Text(
            text = "AI Study Hub Tools",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .clickable { viewModel.currentScreen.value = Screen.StudySpaceFinder }
                    .padding(14.dp)
            ) {
                Column {
                    Text("📍", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Study Spaces", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("Find public libraries", color = Color.Gray, fontSize = 11.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .clickable { viewModel.currentScreen.value = Screen.VideoExplainer }
                    .padding(14.dp)
            ) {
                Column {
                    Text("📺", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Video Explainer", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("Summarize video lectures", color = Color.Gray, fontSize = 11.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .clickable { viewModel.currentScreen.value = Screen.Visualizer }
                    .padding(14.dp)
            ) {
                Column {
                    Text("🎨", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("AI Illustrator", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("Draw study diagrams", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }

        // --- My Course Schedule / Chapter Progress ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Syllabus & Chapters",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(
                onClick = { showAddChapterDialog = true },
                modifier = Modifier.testTag("add_chapter_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Chapter", tint = ScholarPurple)
            }
        }

        if (chaptersList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📚", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No chapters scheduled yet.", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Click '+' to schedule Enzymes or any test syllabus!",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            chaptersList.forEach { chapter ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapter.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Pages ${chapter.rangeFrom} - ${chapter.rangeTo} | Duration: ${chapter.daysToStudy} days",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                            }

                            // Finished badge or Complete Chapter finish button
                            if (chapter.isFinished) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SuccessEmerald.copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Finished", color = SuccessEmerald, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.finishAndTestChapter(chapter) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("Finish", color = CozySlateDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(onClick = { viewModel.deleteChapter(chapter) }) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- Continuous Chat Bar (ubiquitous chat feature requested!) ---
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "AI Fast Study Command",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Type any exam or study need (e.g. NEET pakistan past paper list, Board levels...)",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        var chatCommandText by remember { mutableStateOf("") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = chatCommandText,
                onValueChange = { chatCommandText = it },
                placeholder = { Text("Ask for MDCAT, NEET, IELTS, CSS, page exams...", color = Color.Gray, fontSize = 13.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("fast_chat_command_input")
            )

            IconButton(
                onClick = {
                    if (chatCommandText.isNotBlank()) {
                        val prompt = chatCommandText
                        chatCommandText = ""
                        viewModel.sendProfessorMessage(prompt)
                        viewModel.currentScreen.value = Screen.ProfessorChat
                    }
                },
                modifier = Modifier.testTag("fast_chat_command_send")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Send Command", tint = ScholarPurple)
            }
        }
    }

    // --- Custom Chapter Add Dialog ---
    if (showAddChapterDialog) {
        var chapName by remember { mutableStateOf("") }
        var fromPage by remember { mutableStateOf("1") }
        var toPage by remember { mutableStateOf("10") }
        var durationDays by remember { mutableStateOf("5") }

        Dialog(onDismissRequest = { showAddChapterDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Schedule New Chapter", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = chapName,
                        onValueChange = { chapName = it },
                        label = { Text("Chapter Title (e.g. Enzymes)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScholarPurple,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fromPage,
                            onValueChange = { fromPage = it },
                            label = { Text("From Page") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = toPage,
                            onValueChange = { toPage = it },
                            label = { Text("To Page") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = durationDays,
                        onValueChange = { durationDays = it },
                        label = { Text("Study Duration (Days)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showAddChapterDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }

                        Button(
                            onClick = {
                                if (chapName.isNotBlank()) {
                                    val f = fromPage.toIntOrNull() ?: 1
                                    val t = toPage.toIntOrNull() ?: 10
                                    val d = durationDays.toIntOrNull() ?: 5
                                    viewModel.addChapter(chapName, f, t, d)
                                }
                                showAddChapterDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScholarPurple)
                        ) {
                            Text("Create", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. PROFESSOR CHAT & CONVERSATION VIEW (TTS)
// ==========================================
@Composable
fun ProfessorChatScreen(viewModel: AppViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isProfessorLoading.collectAsState()
    var inputQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when message size increases
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CozySlateDark)
    ) {
        // Professor Title Header Block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(ScholarPurple),
                contentAlignment = Alignment.Center
            ) {
                Text("🎓", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Professor AI", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Text("Answers any level (MDCAT, NEET, Board, IELTS, CSS)", fontSize = 11.sp, color = Color.LightGray)
            }
        }

        // Message Thread list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                val alignEnd = msg.isUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 290.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (alignEnd) ScholarPurple else CardBackground
                        ),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (alignEnd) 16.dp else 2.dp,
                            bottomEnd = if (alignEnd) 2.dp else 16.dp
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = msg.text,
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )

                            // Play TTS option for AI answers
                            if (!alignEnd) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (msg.isPlayingTts) {
                                                viewModel.stopPlayingTts()
                                            } else {
                                                viewModel.playTtsForMessage(msg)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (msg.isPlayingTts) Icons.Default.Close else Icons.Default.PlayArrow,
                                            contentDescription = "TTS Voice",
                                            tint = AccentAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (msg.isPlayingTts) {
                                        Text("Reading...", color = AccentAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Professor is thinking...", modifier = Modifier.padding(12.dp), color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Input bottom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask details on Enzyme pathways or board MCQs...", fontSize = 13.sp, color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ScholarPurple,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("professor_chat_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputQuery.isNotBlank()) {
                        viewModel.sendProfessorMessage(inputQuery)
                        inputQuery = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ScholarPurple)
                    .testTag("professor_chat_send")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

// ==========================================
// 3. SPACED REPETITION FLASHCARDS REVIEW
// ==========================================
@Composable
fun FlashcardReviewScreen(viewModel: AppViewModel) {
    val dueCards by viewModel.dueFlashcards.collectAsState()
    val cardIndex by viewModel.currentCardIndex.collectAsState()
    val isRevealed by viewModel.isAnswerRevealed.collectAsState()
    val isOcrLoading by viewModel.isOcrLoading.collectAsState()

    var showCreateCardDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CozySlateDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Spaced Repetition Deck", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)

            IconButton(onClick = { showCreateCardDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Card", tint = ScholarPurple)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // OCR Upload Simulator box (Fully interactive visual mock for mock/real analysis)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📷 Study OCR - Extract Flashcards from Images", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Select/capture an image of your book (Max 8 pages) to generate QA pairs", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(10.dp))

                if (isOcrLoading) {
                    CircularProgressIndicator(color = ScholarPurple, modifier = Modifier.size(24.dp))
                } else {
                    Button(
                        onClick = {
                            // Since we run in virtual Android build, we simulate taking an image from camera or gallery
                            // and generating highly accurate spaced flashcards using Pro model.
                            val simulatedBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                            viewModel.analyzeStudyImage(simulatedBitmap)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ScholarPurple.copy(alpha = 0.2f)),
                        modifier = Modifier.testTag("ocr_analysis_trigger")
                    ) {
                        Text("Simulate Image Capture & Analyze", color = ScholarPurple, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (dueCards.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("All caught up!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Your memory retention interval is strong. Add more cards to expand.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            val card = dueCards[cardIndex]

            Text(
                text = "Reviewing Card ${cardIndex + 1} of ${dueCards.size}",
                color = Color.LightGray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Flip card container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { viewModel.isAnswerRevealed.value = !isRevealed },
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, if (isRevealed) ScholarPurple else DividerColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isRevealed) "ANSWER" else "QUESTION",
                            color = if (isRevealed) SuccessEmerald else AccentAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isRevealed) card.answer else card.question,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SM-2 Spaced repetition response rating buttons
            if (!isRevealed) {
                Button(
                    onClick = { viewModel.isAnswerRevealed.value = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ScholarPurple),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reveal Answer", color = Color.White)
                }
            } else {
                Text("Rate your memory quality feedback:", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Rating 1: Hard, Rating 3: Normal, Rating 5: Easy
                    Button(
                        onClick = { viewModel.answerFlashcard(1) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Blackout (1)", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.answerFlashcard(3) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Hard (3)", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.answerFlashcard(5) },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Perfect (5)", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showCreateCardDialog) {
        var cardQ by remember { mutableStateOf("") }
        var cardA by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCreateCardDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Create Custom Flashcard", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = cardQ,
                        onValueChange = { cardQ = it },
                        label = { Text("Question or term") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = cardA,
                        onValueChange = { cardA = it },
                        label = { Text("Answer or definition") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { showCreateCardDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                if (cardQ.isNotBlank() && cardA.isNotBlank()) {
                                    viewModel.createFlashcard(cardQ, cardA)
                                }
                                showCreateCardDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScholarPurple)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. HIGH THINKING EXAM SIMULATOR VIEW
// ==========================================
@Composable
fun ExamSimulatorScreen(viewModel: AppViewModel) {
    val isQuizLoading by viewModel.isQuizLoading.collectAsState()
    val questions by viewModel.activeQuizQuestions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val selectedAns by viewModel.selectedAnswerIndex.collectAsState()
    val score by viewModel.quizScore.collectAsState()
    val isActive by viewModel.isQuizActive.collectAsState()
    val isCompleted by viewModel.isQuizCompleted.collectAsState()
    val lastAward by viewModel.lastAwardEarned.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CozySlateDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isQuizLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ScholarPurple, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("High Thinking model generating customized exam questions...", color = Color.White, textAlign = TextAlign.Center)
                    Text("Setting thinkingLevel = ThinkingLevel.HIGH for precision...", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        } else if (isCompleted) {
            // Gamified Awards & Milestones Ceremony View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🎉 Exam Completed! 🎉", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(20.dp))

                Text("Your Score:", color = Color.LightGray, fontSize = 14.sp)
                Text("$score / ${questions.size}", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = AccentAmber)

                Spacer(modifier = Modifier.height(16.dp))

                // Reward Display Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎓 Milestone Reward Earned 🎓", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = lastAward ?: "Chapter Participant Ribbon",
                            color = AccentAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "XP rewards and badges are calculated according to the levels of difficulty and percentage marks.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.exitQuiz() },
                    colors = ButtonDefaults.buttonColors(containerColor = ScholarPurple),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text("Return to Dashboard", color = Color.White)
                }
            }
        } else if (isActive && questions.isNotEmpty()) {
            val q = questions[currentIndex]

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${currentIndex + 1} of ${questions.size}",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )

                IconButton(onClick = { viewModel.exitQuiz() }) {
                    Icon(Icons.Default.Close, contentDescription = "Exit Quiz", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / questions.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = ScholarPurple,
                trackColor = DividerColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Question Container Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = q.question,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multiple choice options list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                q.options.forEachIndexed { idx, option ->
                    val isSelected = selectedAns == idx
                    val isCorrect = q.answerIndex == idx
                    val buttonColor = when {
                        selectedAns != null && isCorrect -> SuccessEmerald
                        isSelected && !isCorrect -> Color(0xFFEF4444)
                        isSelected -> ScholarPurple
                        else -> CardBackground
                    }

                    Button(
                        onClick = { viewModel.selectQuizAnswer(idx) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = option,
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Reveal explanation after answer selected
            if (selectedAns != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = DividerColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            text = if (selectedAns == q.answerIndex) "✨ Brilliant! Correct Answer. ✨" else "❌ Incorrect. Correct Option was Index: ${q.answerIndex}",
                            color = if (selectedAns == q.answerIndex) SuccessEmerald else Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(q.explanation, color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.nextQuizQuestion() },
                    colors = ButtonDefaults.buttonColors(containerColor = ScholarPurple),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Next Question", color = Color.White)
                }
            }
        } else {
            // Setup quiz selector view if quiz is not active
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🎯 High-Thinking Exam Simulator", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Precision tests based on high accuracy reasoning", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(24.dp))

                var examTopic by remember { mutableStateOf("Enzymes") }
                var fromPage by remember { mutableStateOf("24") }
                var toPage by remember { mutableStateOf("52") }
                var examType by remember { mutableStateOf("MDCAT Pakistan") }
                var examLevel by remember { mutableStateOf("Normal") }

                OutlinedTextField(
                    value = examTopic,
                    onValueChange = { examTopic = it },
                    label = { Text("Chapter Title") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fromPage,
                        onValueChange = { fromPage = it },
                        label = { Text("From Page") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = toPage,
                        onValueChange = { toPage = it },
                        label = { Text("To Page") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Exam type selection
                Text("Select Target Exam Body:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("MDCAT Pakistan", "NEET India", "Board level MCQs", "IELTS talking", "CSS Pakistan").forEach {
                        val isSel = examType == it
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) ScholarPurple else CardBackground)
                                .clickable { examType = it }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(it, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Level Selection
                Text("Select Difficulty Level:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Easy", "Normal", "Hard").forEach {
                        val isSel = examLevel == it
                        Button(
                            onClick = { examLevel = it },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSel) ScholarPurple else CardBackground),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(it, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val from = fromPage.toIntOrNull() ?: 24
                        val to = toPage.toIntOrNull() ?: 52
                        viewModel.startExam(examTopic, from, to, examType, examLevel)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScholarPurple),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Launch customized exam simulator", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 5. PLANNER & STUDY SCHEDULER VIEW
// ==========================================
@Composable
fun SchedulerScreen(viewModel: AppViewModel) {
    val chaptersList by viewModel.chapters.collectAsState()
    val scrollState = rememberScrollState()

    var customTaskTitle by remember { mutableStateOf("") }
    var customTaskRangeFrom by remember { mutableStateOf("") }
    var customTaskRangeTo by remember { mutableStateOf("") }
    var customTaskDays by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(CozySlateDark)
            .padding(16.dp)
    ) {
        Text("Personalized Syllabus Planner", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Create target schedules and study paths tailored with countdown milestone tasks.", fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Schedule Study Range Goal", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customTaskTitle,
                    onValueChange = { customTaskTitle = it },
                    label = { Text("Chapter Topic Name (e.g. Bioenergetics)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customTaskRangeFrom,
                        onValueChange = { customTaskRangeFrom = it },
                        label = { Text("From Page") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customTaskRangeTo,
                        onValueChange = { customTaskRangeTo = it },
                        label = { Text("To Page") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customTaskDays,
                    onValueChange = { customTaskDays = it },
                    label = { Text("Duration (Days allocated)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (customTaskTitle.isNotBlank()) {
                            val f = customTaskRangeFrom.toIntOrNull() ?: 1
                            val t = customTaskRangeTo.toIntOrNull() ?: 10
                            val d = customTaskDays.toIntOrNull() ?: 5
                            viewModel.addChapter(customTaskTitle, f, t, d)
                            customTaskTitle = ""
                            customTaskRangeFrom = ""
                            customTaskRangeTo = ""
                            customTaskDays = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScholarPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add scheduled topic to planner", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Planner Milestones", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

        if (chaptersList.isEmpty()) {
            Text("No goals added yet.", color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
        } else {
            chaptersList.forEach { chapter ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(chapter.name, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Pages ${chapter.rangeFrom} - ${chapter.rangeTo} | Duration: ${chapter.daysToStudy} Days", color = Color.Gray, fontSize = 12.sp)
                            }

                            if (chapter.isFinished) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SuccessEmerald.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Done", color = SuccessEmerald, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.finishAndTestChapter(chapter) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Chapter Finished", color = CozySlateDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Countdown Day Milestones
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = DividerColor)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (day in 1..chapter.daysToStudy) {
                                val isDoneDay = day < chapter.currentDayOfStudy || chapter.isFinished
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isDoneDay) SuccessEmerald else DividerColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$day", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. MAPS GROUNDING: STUDY SPACE FINDER VIEW
// ==========================================
@Composable
fun StudySpaceFinderScreen(viewModel: AppViewModel) {
    val isLocationLoading by viewModel.isLocationLoading.collectAsState()
    val spacesResult by viewModel.studySpacesResult.collectAsState()
    var searchLocation by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CozySlateDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.currentScreen.value = Screen.Dashboard }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Study Spot Maps Grounding", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Text("Find public libraries, academic coaching spots or quiet group study halls near you.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchLocation,
            onValueChange = { searchLocation = it },
            placeholder = { Text("Enter city or neighborhood (e.g. Lahore, Delhi, Boston...)") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { viewModel.findLocations(searchLocation) }) {
                    Icon(Icons.Default.Search, contentDescription = "Search locations")
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isLocationLoading) {
            CircularProgressIndicator(color = ScholarPurple, modifier = Modifier.size(36.dp))
        } else if (spacesResult != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("📍 Maps Verified Study Spaces Nearby:", fontWeight = FontWeight.Bold, color = AccentAmber, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(spacesResult!!, color = Color.White, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗺️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No search history. Look up quiet places to study!", color = Color.Gray)
                }
            }
        }
    }
}

// ==========================================
// 7. VIDEO STUDY COURSE EXPLAINER VIEW
// ==========================================
@Composable
fun VideoExplainerScreen(viewModel: AppViewModel) {
    val isVideoLoading by viewModel.isVideoLoading.collectAsState()
    val videoExplResult by viewModel.videoExplanationResult.collectAsState()

    var videoTitle by remember { mutableStateOf("Enzymes & Coenzymes Lecture") }
    var isBriefExplan by remember { mutableStateOf(true) }
    var customQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CozySlateDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.currentScreen.value = Screen.Dashboard }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI Video Lecture Explainer", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Text("Enter video title/link (supports files up to 2-3 hrs) to extract key syllabus notes & test questions.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = videoTitle,
                    onValueChange = { videoTitle = it },
                    label = { Text("Video Title or Lecture Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isBriefExplan, onClick = { isBriefExplan = true })
                    Text("Brief Explanation", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !isBriefExplan, onClick = { isBriefExplan = false })
                    Text("Long/Detailed Note", color = Color.White, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customQuery,
                    onValueChange = { customQuery = it },
                    placeholder = { Text("What topics do you want explained? (e.g. show questions)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.analyzeStudyVideo(videoTitle, isBriefExplan, customQuery) },
                    colors = ButtonDefaults.buttonColors(containerColor = ScholarPurple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Extract Full Lecture Explanation & Questions", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isVideoLoading) {
            CircularProgressIndicator(color = ScholarPurple)
        } else if (videoExplResult != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("📊 Generated Lecture Syllabus Summary:", fontWeight = FontWeight.Bold, color = AccentAmber, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(videoExplResult!!, color = Color.White, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Your educational video breakdown will show here.", color = Color.Gray)
            }
        }
    }
}

// ==========================================
// 8. AI STUDY ILLUSTRATOR & CONCEPT VISUALIZER
// ==========================================
@Composable
fun VisualizerScreen(viewModel: AppViewModel) {
    val isImageGenerating by viewModel.isImageGenerating.collectAsState()
    val bitmapResult by viewModel.generatedImageBitmap.collectAsState()

    var imagePrompt by remember { mutableStateOf("Structure of enzyme showing active site bound with substrate") }
    val aspectRatios = listOf("1:1", "16:9", "9:16", "3:4", "4:3", "21:9")
    val selectedRatio by viewModel.imageAspectRatioSelected.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CozySlateDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.currentScreen.value = Screen.Dashboard }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI Conceptual Visualizer", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Text("Generate high fidelity customized drawings/illustrations of complex study concepts.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = imagePrompt,
                    onValueChange = { imagePrompt = it },
                    label = { Text("What diagram/concept artwork to draw?") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScholarPurple, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Select Aspect Ratio:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    aspectRatios.forEach { ratio ->
                        val isSel = selectedRatio == ratio
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) ScholarPurple else DividerColor)
                                .clickable { viewModel.imageAspectRatioSelected.value = ratio }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(ratio, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.generateConceptArtwork(imagePrompt, selectedRatio) },
                    colors = ButtonDefaults.buttonColors(containerColor = ScholarPurple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate Concept Study Artwork", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isImageGenerating) {
            CircularProgressIndicator(color = ScholarPurple)
        } else if (bitmapResult != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = bitmapResult!!.asImageBitmap(),
                        contentDescription = "AI Generated Concept",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Your generated conceptual illustration will display here.", color = Color.Gray)
            }
        }
    }
}
