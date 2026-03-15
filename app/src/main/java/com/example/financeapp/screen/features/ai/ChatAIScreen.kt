package com.example.financeapp.screen.features.ai

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.viewmodel.ai.AIViewModel
import com.example.financeapp.viewmodel.ai.ChatMessage
import com.example.financeapp.viewmodel.savings.SavingsViewModel

import com.example.financeapp.components.theme.getAppColors
import androidx.compose.ui.graphics.vector.ImageVector

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAIScreen(
    navController: NavController,
    aiViewModel: AIViewModel = viewModel(),
    savingsViewModel: SavingsViewModel
) {
    val colors = getAppColors()
    val messages by aiViewModel.messages.collectAsState()
    val isAITyping by aiViewModel.isAITyping
    val lastError by aiViewModel.lastError

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Wendy AI", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("Trợ lý tài chính thông minh", fontSize = 11.sp, color = colors.textSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                items(messages.size) { index ->
                    ChatBubbleModern(message = messages[index])
                }

                if (isAITyping) {
                    item { TypingIndicatorModern() }
                }
            }
            
            // Quick Suggestions
            if (messages.size <= 2) {
                QuickSuggestionsRow(onSuggestionClick = { input = it })
            }

            ChatInputModern(
                input = input,
                onInputChange = { input = it },
                onSendClick = {
                    if (input.isNotBlank() && !isAITyping) {
                        aiViewModel.sendUserMessage(input)
                        input = ""
                    }
                },
                isAITyping = isAITyping
            )
        }
    }
}

@Composable
fun ChatBubbleModern(message: ChatMessage) {
    val colors = getAppColors()
    val isUser = message.isUser
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.accentGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = if (isUser) colors.primary else Color.White,
            shape = RoundedCornerShape(
                topStart = if (isUser) 20.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color.White else colors.textPrimary,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ChatInputModern(
    input: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isAITyping: Boolean
) {
    val colors = getAppColors()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Hỏi Wendy bất cứ điều gì...", fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.background,
                    unfocusedContainerColor = colors.background,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            IconButton(
                onClick = onSendClick,
                enabled = input.isNotBlank() && !isAITyping,
                modifier = Modifier
                    .size(48.dp)
                    .background(if (input.isNotBlank()) colors.primary else colors.background, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = if (input.isNotBlank()) Color.White else colors.textMuted)
            }
        }
    }
}

@Composable
fun QuickSuggestionsRow(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf("Thêm chi tiêu 50k ăn phở", "Phân tích tuần này", "Mẹo tiết kiệm", "Sức khỏe tài chính")
    LazyRow(
        modifier = Modifier.padding(bottom = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(suggestion, fontSize = 12.sp) },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun TypingIndicatorModern() {
    val colors = getAppColors()
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp).background(colors.accentGradient, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Wendy đang suy nghĩ...", fontSize = 12.sp, color = colors.textMuted)
    }
}