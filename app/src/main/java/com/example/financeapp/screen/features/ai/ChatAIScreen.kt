package com.example.financeapp

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.viewmodel.ai.AIViewModel
import com.example.financeapp.viewmodel.ai.ChatMessage

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAIScreen(
    navController: NavController,
    aiViewModel: AIViewModel = viewModel()
) {
    val messages = aiViewModel.messages
    val isAITyping by aiViewModel.isAITyping
    val lastError by aiViewModel.lastError

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Màu sắc
    val primaryColor = Color(0xFF0F4C75)
    val secondaryColor = Color(0xFF2E8B57)
    val backgroundColor = Color(0xFFF5F7FA)
    val surfaceColor = Color.White
    val textPrimary = Color(0xFF2D3748)
    val textSecondary = Color(0xFF718096)
    val accentColor = Color(0xFFED8936)
    val errorColor = Color(0xFFE53E3E)
    val successColor = Color(0xFF38A169)
    val proactiveColor = Color(0xFF8B5CF6)

    // ✅ Scroll tự động khi tin nhắn mới xuất hiện
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(primaryColor, Color(0xFF1A365D))
                                    ),
                                    CircleShape
                                )
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = "AI",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Wendy AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                primaryColor.copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryColor
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { aiViewModel.clearChat() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                textSecondary.copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Xóa đoạn chat",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = textPrimary,
                    navigationIconContentColor = primaryColor,
                    actionIconContentColor = primaryColor
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Chat messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages.size, key = { index -> messages[index].id }) { index ->
                    val message = messages[index]
                    if (message.text.isNotBlank()) {
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInHorizontally(
                                initialOffsetX = { fullWidth ->
                                    if (message.isUser) fullWidth else -fullWidth
                                },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ),
                            exit = slideOutHorizontally(
                                targetOffsetX = { fullWidth ->
                                    if (message.isUser) fullWidth else -fullWidth
                                },
                                animationSpec = tween(400)
                            )
                        ) {
                            ChatBubble(
                                message = message,
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                proactiveColor = proactiveColor
                            )
                        }
                    }
                }

                // AI typing indicator
                item {
                    if (isAITyping) {
                        TypingIndicator(
                            primaryColor = primaryColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            }

            // Hiển thị lỗi
            lastError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = errorColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = "Error",
                            tint = errorColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = errorColor,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { aiViewModel.lastError.value = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = errorColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Input section
            ChatInputSection(
                input = input,
                onInputChange = { input = it },
                onSendClick = {
                    if (input.isNotBlank() && !isAITyping) {
                        aiViewModel.sendUserMessage(input)
                        input = ""
                    }
                },
                isAITyping = isAITyping,
                primaryColor = primaryColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                surfaceColor = surfaceColor,
                backgroundColor = backgroundColor
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    primaryColor: Color,
    secondaryColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    proactiveColor: Color
) {
    val bubbleColor = if (message.isProactive) {
        proactiveColor
    } else if (message.isUser) {
        primaryColor
    } else {
        Color.White
    }

    val textColor = if (message.isProactive || message.isUser) {
        Color.White
    } else {
        textPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isUser && !message.isProactive) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .shadow(3.dp, CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(primaryColor, Color(0xFF1A365D))
                        ),
                        CircleShape
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            modifier = Modifier
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(
                        topStart = if (message.isUser) 16.dp else 4.dp,
                        topEnd = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    ),
                    clip = true
                ),
            colors = CardDefaults.cardColors(
                containerColor = bubbleColor
            ),
            shape = RoundedCornerShape(
                topStart = if (message.isUser) 16.dp else 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (message.isProactive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = "Proactive Tip",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gợi ý từ Wendy",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 18.sp
                )
            }
        }

        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .shadow(3.dp, CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(secondaryColor, secondaryColor.copy(alpha = 0.8f))
                        ),
                        CircleShape
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
@Composable
fun ChatInputSection(
    input: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isAITyping: Boolean,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        backgroundColor,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    singleLine = false,
                    maxLines = 3,
                    textStyle = TextStyle(
                        color = textPrimary,
                        fontSize = 16.sp,
                        lineHeight = 20.sp
                    ),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (input.isEmpty()) {
                                Text(
                                    text = "Nhập câu hỏi về tài chính...",
                                    color = textSecondary,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .shadow(
                        elevation = if (input.isNotBlank() && !isAITyping) 6.dp else 2.dp,
                        shape = CircleShape,
                        clip = true
                    )
                    .background(
                        brush = if (input.isNotBlank() && !isAITyping) {
                            Brush.verticalGradient(
                                colors = listOf(primaryColor, Color(0xFF1A365D))
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    textSecondary.copy(alpha = 0.3f),
                                    textSecondary.copy(alpha = 0.1f)
                                )
                            )
                        },
                        shape = CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSendClick,
                    enabled = input.isNotBlank() && !isAITyping,
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (input.isNotBlank() && !isAITyping) {
                            Color.White
                        } else {
                            textSecondary.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickQuestionItem(
    question: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(primaryColor.copy(alpha = 0.1f), CircleShape)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = question,
                color = textPrimary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun TypingIndicator(
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val dotCount = 3
    val infiniteTransition = rememberInfiniteTransition()
    val alphaValues = List(dotCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600,
                    delayMillis = index * 200,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .shadow(3.dp, CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(primaryColor, Color(0xFF1A365D))
                    ),
                    CircleShape
                )
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "AI",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Card(
            modifier = Modifier.shadow(3.dp, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(4.dp, 12.dp, 12.dp, 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI đang trả lời",
                    color = textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                repeat(dotCount) { i ->
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                primaryColor.copy(alpha = alphaValues[i].value)
                            )
                    )
                    if (i != dotCount - 1) Spacer(modifier = Modifier.width(3.dp))
                }
            }
        }
    }
}