package com.oxidelabmobile.ui.screens.lab

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import com.oxidelabmobile.ui.components.LabHeader
import com.oxidelabmobile.ui.components.MessageBubble
import com.oxidelabmobile.ui.components.MessageContextMenu
import com.oxidelabmobile.ui.components.ModelSettingsSheet
import com.oxidelabmobile.ui.components.ComposerField
import com.oxidelabmobile.ui.theme.OxideLabMobileTheme
import com.oxidelabmobile.ui.theme.Spacing

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
    var messages by remember { mutableStateOf(listOf(
        ChatMessage("Привет! Я готов помочь с анализом данных.", false, "14:30"),
        ChatMessage("Отлично! Можешь объяснить принципы машинного обучения?", true, "14:31")
    ))}
    var showSettingsSheet by remember { mutableStateOf(false) }
    var chatTitle by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("Qwen 3") }
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Drawer header
                Text("Меню", modifier = Modifier.padding(Spacing.Medium))

                // Menu items
                TextButton(
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onOpenModelManager()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Medium)
                ) {
                    Text("Менеджер моделей")
                }

                TextButton(
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        showSettingsSheet = true
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Medium)
                ) {
                    Text("Настройки")
                }

                TextButton(
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onSettings()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Medium)
                ) {
                    Text("О программе")
                }
            }
        }
    ) {
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
                modelName = modelName
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
