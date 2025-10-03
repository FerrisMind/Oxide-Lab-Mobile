package com.oxidelabmobile.ui.screens.lab

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import com.oxidelabmobile.R
import com.oxidelabmobile.ModelDownloader
import com.oxidelabmobile.DownloadedModel
import com.oxidelabmobile.ui.components.LabHeader
import com.oxidelabmobile.ui.components.MessageBubble
import com.oxidelabmobile.ui.components.MessageContextMenu
import com.oxidelabmobile.ui.components.ModelSettingsSheet
import com.oxidelabmobile.ui.components.ComposerField
import com.oxidelabmobile.ui.theme.OxideLabMobileTheme
import com.oxidelabmobile.ui.theme.Spacing

// Новые импорты для синхронного отслеживания прогресса drawer и offset
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveLabScreen(
    onThinkingMode: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenModelManager: () -> Unit = {}
) {
    val context = LocalContext.current
    val modelDownloader = remember { ModelDownloader(context) }
    
    var messages by remember { mutableStateOf(listOf(
        ChatMessage("Привет! Я готов помочь с анализом данных.", false, "14:30"),
        ChatMessage("Отлично! Можешь объяснить принципы машинного обучения?", true, "14:31")
    ))}
    var showSettingsSheet by remember { mutableStateOf(false) }
    var chatTitle by remember { mutableStateOf("") }
    var downloadedModels by remember { mutableStateOf<List<DownloadedModel>>(emptyList()) }
    var selectedModel by remember { mutableStateOf<DownloadedModel?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Load downloaded models on startup
    LaunchedEffect(Unit) {
        modelDownloader.syncCacheWithFiles()
        downloadedModels = modelDownloader.getDownloadedModels()
        // Select first model if available
        if (downloadedModels.isNotEmpty() && selectedModel == null) {
            selectedModel = downloadedModels.first()
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Fixed drawer width and a fraction that follows drawer progress exactly
    val drawerWidth = 256.dp
    var drawerFraction by remember { mutableStateOf(0f) }

    // Update drawer fraction based on drawer state
    drawerFraction = when (drawerState.targetValue) {
        DrawerValue.Closed -> 0f
        DrawerValue.Open -> 1f
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(drawerWidth),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Drawer header
                    Text(
                        text = "Меню", 
                        modifier = Modifier.padding(Spacing.Medium),
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                    )

                    // Spacer to push menu items to bottom
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

                    // Menu items aligned to bottom left
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onOpenModelManager()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.Medium)
                                .height(48.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.Medium, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.folder_simple),
                                    contentDescription = "Менеджер моделей",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Менеджер моделей")
                            }
                        }

                        TextButton(
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                showSettingsSheet = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.Medium)
                                .height(48.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.Medium, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.gear_fill),
                                    contentDescription = "Настройки",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Настройки")
                            }
                        }

                        TextButton(
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onSettings()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.Medium)
                                .height(48.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.Medium, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.info),
                                    contentDescription = "О программе",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text("О программе")
                            }
                        }
                    }
                }
            }
        }
    ) {
        // Смещаем основной контент синхронно с drawerFraction
        Box(modifier = Modifier.fillMaxSize().offset(x = drawerWidth * drawerFraction)) {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    LabHeader(
                        title = if (chatTitle.isBlank()) "Oxide Lab" else chatTitle,
                        statusText = "",
                        isOnline = true,
                        onMenuClick = { coroutineScope.launch { drawerState.open() } },
                        onSettingsClick = { showSettingsSheet = true }
                    )
                }
            ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    items(messages) { message ->
                        MessageBubble(
                            text = message.text,
                            isUser = message.isUser,
                            timestamp = message.timestamp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(message) {
                                    detectTapGestures(
                                        onLongPress = {
                                            if (!message.isUser) {
                                                selectedMessage = message
                                                showContextMenu = true
                                            }
                                        }
                                    )
                                }
                        )
                    }
                }

                // Input area
                ComposerField(
                    onSend = { text ->
                        if (text.isNotBlank()) {
                            val newMessage = ChatMessage(
                                text = text,
                                isUser = true,
                                timestamp = "14:32"
                            )
                            // set chat title from first user message
                            if (chatTitle.isBlank()) {
                                chatTitle = text.take(30)
                            }

                            messages = messages + newMessage
                            // Simulate AI response
                            val aiResponse = ChatMessage(
                                text = "Это отличный вопрос! Машинное обучение основано на...",
                                isUser = false,
                                timestamp = "14:32"
                            )
                            messages = messages + aiResponse
                        }
                    },
                    onSettings = { showSettingsSheet = true },
                    modifier = Modifier.padding(Spacing.Medium),
                    modelName = selectedModel?.name ?: "Нет модели",
                    modelIconResId = selectedModel?.iconResId ?: R.drawable.sparkle,
                    availableModels = downloadedModels,
                    onModelSelected = { model ->
                        selectedModel = model
                    }
                )
            }

            // Gesture overlays
            ModelSettingsSheet(
                isVisible = showSettingsSheet,
                onDismiss = { showSettingsSheet = false }
            )

            MessageContextMenu(
                isVisible = showContextMenu,
                onCopy = {
                    // TODO: Copy message to clipboard
                },
                onContinue = {
                    // TODO: Continue conversation with this context
                },
                onRate = {
                    // TODO: Show rating dialog
                },
                onDismiss = {
                    showContextMenu = false
                    selectedMessage = null
                }
            )
        }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActiveLabScreenPreview() {
    OxideLabMobileTheme {
        ActiveLabScreen(
            onThinkingMode = {},
            onSettings = {}
        )
    }
}
