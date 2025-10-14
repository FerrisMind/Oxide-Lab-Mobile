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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
import androidx.compose.foundation.layout.size
import android.util.Log
import com.oxidelabmobile.R
import com.oxidelabmobile.ModelDownloader
import com.oxidelabmobile.DownloadedModel
import com.oxidelabmobile.RustInterface
import com.oxidelabmobile.ui.components.LabHeader
import com.oxidelabmobile.ui.components.MessageBubble
import com.oxidelabmobile.ui.components.MessageContextMenu
import com.oxidelabmobile.ui.components.ModelSettingsSheet
import com.oxidelabmobile.ui.components.ComposerField
import com.oxidelabmobile.ui.theme.OxideLabMobileTheme
import com.oxidelabmobile.ui.theme.Spacing

// Новые импорты для синхронного отслеживания прогресса drawer и offset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

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
    val rustInterface = remember { RustInterface.instance }

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var chatTitle by remember { mutableStateOf("") }
    var downloadedModels by remember { mutableStateOf<List<DownloadedModel>>(emptyList()) }
    var selectedModel by remember { mutableStateOf<DownloadedModel?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var isModelLoading by remember { mutableStateOf(false) }
    var modelLoadingStatus by remember { mutableStateOf("") }
    var modelToLoad by remember { mutableStateOf<DownloadedModel?>(null) }
    var modelToUnload by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var currentAIResponse by remember { mutableStateOf<String?>(null) }
    var generationWasStopped by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Handle model loading when a model is selected
    LaunchedEffect(modelToLoad) {
        val model = modelToLoad ?: return@LaunchedEffect

        isModelLoading = true
        modelLoadingStatus = "Загрузка модели в память..."

        try {
            val modelPath = "/storage/emulated/0/models/${model.fileName}"
            val initResult = rustInterface.initializeModelFromFileSuspend(modelPath)

            if (initResult.startsWith("Error:", ignoreCase = false)) {
                modelLoadingStatus = "Ошибка загрузки модели: ${initResult.substringAfter("Error: ")}"
                // Сбрасываем выбор модели при ошибке
                selectedModel = null
            } else {
                modelLoadingStatus = "Модель ${model.name} готова к работе!"
                selectedModel = model
                // Автоматически скрываем статус через 3 секунды
                delay(3000)
                modelLoadingStatus = ""
            }
        } catch (e: Exception) {
            modelLoadingStatus = "Ошибка загрузки модели: ${e.message}"
            selectedModel = null
            // Оставляем сообщение об ошибке видимым дольше
            delay(5000)
            modelLoadingStatus = ""
        } finally {
            isModelLoading = false
            modelToLoad = null // Сбрасываем флаг загрузки
        }
    }

    // Handle model unloading
    LaunchedEffect(modelToUnload) {
        Log.d("ActiveLabScreen", "LaunchedEffect triggered for modelToUnload: $modelToUnload")
        if (!modelToUnload) return@LaunchedEffect

        try {
            Log.d("ActiveLabScreen", "Starting model unload process")
            // Выгружаем модель из памяти на уровне Rust
            rustInterface.unloadModelSuspend()
            Log.d("ActiveLabScreen", "Model unload completed successfully")

            // Сбрасываем состояние UI
            selectedModel = null
            modelLoadingStatus = "Модель выгружена из памяти"
            // Автоматически скрываем статус через 2 секунды
            delay(2000)
            modelLoadingStatus = ""
        } catch (e: Exception) {
            Log.e("ActiveLabScreen", "Error unloading model", e)
            modelLoadingStatus = "Ошибка выгрузки модели: ${e.message}"
            // Оставляем сообщение об ошибке видимым дольше
            delay(5000)
            modelLoadingStatus = ""
        } finally {
            modelToUnload = false // Сбрасываем флаг выгрузки
            Log.d("ActiveLabScreen", "Model unload process finished, resetting flag")
        }
    }

    // Load downloaded models on startup
    LaunchedEffect(Unit) {
        modelDownloader.syncCacheWithFiles()
        downloadedModels = modelDownloader.getDownloadedModels()
        // Don't auto-select model - let user choose manually
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

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
                            shape = RoundedCornerShape(12.dp),
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
                            shape = RoundedCornerShape(12.dp),
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
                            shape = RoundedCornerShape(12.dp),
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

                    // Show streaming AI response if generating
                    if (isGenerating && !currentAIResponse.isNullOrEmpty()) {
                        item {
                            MessageBubble(
                                text = currentAIResponse!!,
                                isUser = false,
                                timestamp = "...",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Model loading status
                if (isModelLoading || modelLoadingStatus.isNotEmpty()) {
                    androidx.compose.material3.Text(
                        text = if (isModelLoading) modelLoadingStatus else modelLoadingStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isModelLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
                    )
                }

                // Input area
                ComposerField(
                    onSend = { text ->
                        if (text.isNotBlank() && selectedModel != null && !isModelLoading && !isGenerating) {
                            val newMessage = ChatMessage(
                                text = text,
                                isUser = true,
                                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            )
                            // set chat title from first user message
                            if (chatTitle.isBlank()) {
                                chatTitle = text.take(30)
                            }

                            messages = messages + newMessage

                            // Generate AI response with streaming
                            Log.d("ActiveLabScreen", "Starting AI generation")
                            isGenerating = true
                            currentAIResponse = ""
                            generationWasStopped = false
                            coroutineScope.launch {
                                try {
                                    rustInterface.generateTextStreaming(
                                        text,
                                        RustInterface.GenerationConfig(
                                            maxTokens = 512,
                                            temperature = 0.7f,
                                            topP = 0.9f,
                                            repeatPenalty = 1.1f
                                        ),
                                        onToken = { token ->
                                            currentAIResponse = (currentAIResponse ?: "") + token
                                        },
                                        onComplete = {
                                            Log.d("ActiveLabScreen", "Generation completed, currentAIResponse length: ${currentAIResponse?.length}, generationWasStopped: $generationWasStopped")
                                            // Always save the response if we have any content, even if generation was stopped
                                            if (!currentAIResponse.isNullOrEmpty()) {
                                                val aiResponse = ChatMessage(
                                                    text = currentAIResponse!!,
                                                    isUser = false,
                                                    timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                                )
                                                messages = messages + aiResponse
                                                Log.d("ActiveLabScreen", "AI response saved to messages")
                                            } else {
                                                Log.w("ActiveLabScreen", "No AI response to save")
                                            }
                                            // Reset state
                                            currentAIResponse = null
                                            isGenerating = false
                                            generationWasStopped = false
                                            Log.d("ActiveLabScreen", "Generation state reset")
                                        },
                                        onError = { error ->
                                            val errorMessage = ChatMessage(
                                                text = "Произошла ошибка при генерации ответа: $error",
                                                isUser = false,
                                                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                            )
                                            messages = messages + errorMessage
                                            currentAIResponse = null
                                        }
                                    )
                                } catch (e: Exception) {
                                    val errorMessage = ChatMessage(
                                        text = "Произошла ошибка при генерации ответа: ${e.message}",
                                        isUser = false,
                                        timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                    )
                                    messages = messages + errorMessage
                                    currentAIResponse = null
                                } finally {
                                    isGenerating = false
                                    generationWasStopped = false
                                }
                            }
                        }
                    },
                    onSettings = { showSettingsSheet = true },
                    modifier = Modifier.padding(Spacing.Medium),
                    modelName = if (isModelLoading) "Загрузка..." else (selectedModel?.name ?: "Выбрать модель"),
                    modelIconResId = selectedModel?.iconResId ?: R.drawable.sparkle,
                    availableModels = downloadedModels,
                    onModelSelected = { model ->
                        // Не позволяем выбирать модель во время загрузки другой
                        if (isModelLoading) return@ComposerField

                        // Устанавливаем модель для загрузки - LaunchedEffect обработает её
                        modelToLoad = model
                    },
                    onModelUnload = {
                        // Устанавливаем флаг выгрузки модели - LaunchedEffect обработает её
                        Log.d("ActiveLabScreen", "Model unload button clicked, selectedModel: ${selectedModel?.name}, current modelToUnload: $modelToUnload")
                        if (!modelToUnload) {
                            modelToUnload = true
                            Log.d("ActiveLabScreen", "Set modelToUnload to true")
                        } else {
                            Log.d("ActiveLabScreen", "modelToUnload was already true")
                        }
                    },
                    isModelSelected = selectedModel != null,
                    isGenerating = isGenerating,
                    onStopGeneration = {
                        // Stop generation on the Rust backend
                        Log.d("ActiveLabScreen", "Stop generation button clicked, isGenerating: $isGenerating, generationWasStopped: $generationWasStopped")
                        if (!generationWasStopped) {
                            generationWasStopped = true
                            Log.d("ActiveLabScreen", "Setting generationWasStopped to true")
                            coroutineScope.launch {
                                try {
                                    Log.d("ActiveLabScreen", "Calling stopGenerationSuspend()")
                                    // Вызываем остановку несколько раз для надежности
                                    repeat(3) {
                                        rustInterface.stopGenerationSuspend()
                                        delay(10)
                                    }
                                    Log.d("ActiveLabScreen", "Stop generation requested successfully (multiple calls)")
                                    // Add a small delay to show the stop effect
                                    delay(200)
                                    if (!currentAIResponse.isNullOrEmpty()) {
                                        Log.d("ActiveLabScreen", "Generation stopped, saving partial response")
                                        val aiResponse = ChatMessage(
                                            text = currentAIResponse!!,
                                            isUser = false,
                                            timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                        )
                                        messages = messages + aiResponse
                                        currentAIResponse = null
                                    }
                                    isGenerating = false
                                } catch (e: Exception) {
                                    Log.e("ActiveLabScreen", "Error stopping generation", e)
                                    // If stopping fails, at least update UI state
                                    isGenerating = false
                                    currentAIResponse = null
                                }
                            }
                        } else {
                            Log.d("ActiveLabScreen", "Generation was already stopped")
                        }
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
