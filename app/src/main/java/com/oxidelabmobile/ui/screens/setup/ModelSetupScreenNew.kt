package com.oxidelabmobile.ui.screens.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import com.oxidelabmobile.ModelDownloader
import com.oxidelabmobile.ProgressListener
import com.oxidelabmobile.R
import com.oxidelabmobile.RustInterface
import kotlinx.coroutines.launch
import java.io.File
import android.os.Environment
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Новый экран настройки модели с использованием Kotlin HTTP клиента
 * Стабилен на Android и не вызывает SIGABRT крашей
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSetupScreenNew(
    onModelLoaded: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    openedFromMenu: Boolean = false
) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val modelDownloader = remember { ModelDownloader(context) }
        val rustInterface = remember { RustInterface.instance }

        var downloadProgress by remember { mutableStateOf(0f) }
        var downloadStatus by remember { mutableStateOf("") }
        var isDownloading by remember { mutableStateOf(false) }
        var downloadedModelPath by remember { mutableStateOf<String?>(null) }
        var isModelDownloaded by remember { mutableStateOf(false) }
        var modelFileSize by remember { mutableStateOf<Long>(-1) }

        // Состояние для второй модели (Gemma)
        var isGemmaDownloaded by remember { mutableStateOf(false) }
        var isGemmaDownloading by remember { mutableStateOf(false) }
        var gemmaDownloadProgress by remember { mutableStateOf(0f) }
        var gemmaDownloadStatus by remember { mutableStateOf("") }
        var gemmaFileSize by remember { mutableStateOf<Long>(-1) }
        var gemmaDownloadedPath by remember { mutableStateOf<String?>(null) }

    // Модели будут сохраняться в папку models в корне устройства
    val modelsDir = File(Environment.getExternalStorageDirectory(), "models").absolutePath

    // Константы для модели
    val modelName = "unsloth/Qwen3-0.6B-GGUF"
    val fileName = "Qwen3-0.6B-Q5_K_M.gguf"

    // Константы для второй модели (Gemma)
    val gemmaModelName = "unsloth/gemma-3-270m-it-qat-GGUF"
    val gemmaFileName = "gemma-3-270m-it-qat-Q5_K_M.gguf"
    val cardListState = rememberLazyListState()

    // Проверяем состояние моделей при запуске
    LaunchedEffect(Unit) {
        modelDownloader.syncCacheWithFiles()

        // Проверяем первую модель (Qwen)
        isModelDownloaded = modelDownloader.isModelDownloaded(modelName, fileName)
        modelFileSize = modelDownloader.getModelFileSize(fileName)

        if (isModelDownloaded) {
            downloadedModelPath = File(Environment.getExternalStorageDirectory(), "models/$fileName").absolutePath
            downloadStatus = "Модель уже загружена"
        }

        // Проверяем вторую модель (Gemma)
        isGemmaDownloaded = modelDownloader.isModelDownloaded(gemmaModelName, gemmaFileName)
        gemmaFileSize = modelDownloader.getModelFileSize(gemmaFileName)

        if (isGemmaDownloaded) {
            gemmaDownloadedPath = File(Environment.getExternalStorageDirectory(), "models/$gemmaFileName").absolutePath
            gemmaDownloadStatus = "Модель уже загружена"
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (openedFromMenu) {
                // Opened from menu: always show Back
                TextButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF1B1C3A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Назад")
                }
            } else {
                // Opened from startup flow: always show Skip button
                TextButton(
                    onClick = {
                        val prefs = context.getSharedPreferences("oxide_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("first_launch", false).apply()
                        // Skip setup and go directly to chat
                        onModelLoaded()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF1B1C3A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .semantics {
                            contentDescription = "Skip model setup"
                        }
                ) {
                    Text("Пропустить")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Настройка модели",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                state = cardListState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.qwen),
                                    contentDescription = "Qwen Icon",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Qwen3-0.6B-GGUF",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "Модель для чат-бота с поддержкой русского языка",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (isDownloading) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = downloadStatus,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!isModelDownloaded) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    isDownloading = true
                                                    downloadStatus = "Проверка доступности модели..."
                                                    downloadProgress = 0.1f

                                                    try {
                                                        downloadStatus = "Начинаем загрузку модели..."
                                                        downloadProgress = 0.0f

                                                        // Загружаем модель через Kotlin HTTP клиент (resolve) в папку models в корне
                                                        val modelPath = modelDownloader.downloadModel(
                                                            modelName,
                                                            fileName,
                                                            object : ProgressListener {
                                                                override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                                                                    if (contentLength > 0) {
                                                                        val progress = (bytesRead.toFloat() / contentLength.toFloat())
                                                                        downloadProgress = progress
                                                                        downloadStatus = "Загрузка: ${(progress * 100).toInt()}% (${bytesRead / 1024 / 1024}MB / ${contentLength / 1024 / 1024}MB)"
                                                                    }
                                                                }
                                                            }
                                                        )

                                                        if (modelPath != null) {
                                                            downloadStatus = "Модель загружена, инициализация..."
                                                            downloadProgress = 0.8f

                                                            // Инициализируем модель в Rust
                                                            val initResult = rustInterface.initializeModelFromFile(modelPath)

                                                            if (initResult.startsWith("Error:")) {
                                                                downloadStatus = "Ошибка инициализации: $initResult"
                                                            } else {
                                                                downloadStatus = "Модель успешно инициализирована!"
                                                                downloadProgress = 1.0f
                                                                downloadedModelPath = modelPath
                                                                isModelDownloaded = true
                                                                modelFileSize = modelDownloader.getModelFileSize(fileName)
                                                            }
                                                        } else {
                                                            downloadStatus = "Ошибка загрузки модели"
                                                        }

                                                    } catch (e: Exception) {
                                                        downloadStatus = "Ошибка: ${e.message}"
                                                    } finally {
                                                        isDownloading = false
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !isDownloading
                                        ) {
                                            Text(if (isDownloading) "Загрузка..." else "Загрузить модель")
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        val deleted = modelDownloader.deleteModel(modelName, fileName)
                                                        if (deleted) {
                                                            isModelDownloaded = false
                                                            downloadedModelPath = null
                                                            modelFileSize = -1
                                                            downloadStatus = "Модель удалена"
                                                        } else {
                                                            downloadStatus = "Ошибка удаления модели"
                                                        }
                                                    } catch (e: Exception) {
                                                        downloadStatus = "Ошибка: ${e.message}"
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !isDownloading,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text("Удалить модель")
                                        }
                                    }

                                    if (downloadedModelPath != null) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        val result = rustInterface.runCandleExample()
                                                        downloadStatus = "Результат: $result"
                                                    } catch (e: Exception) {
                                                        downloadStatus = "Ошибка выполнения: ${e.message}"
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !isDownloading
                                        ) {
                                            Text("Тест модели")
                                        }
                                    }
                                }

                            if (downloadedModelPath != null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "✅ Модель готова к использованию",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Размер: ${if (modelFileSize > 0) "${modelFileSize / 1024 / 1024}MB" else "Неизвестно"}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Файл: $fileName",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                }
                item {
                    // Вторая карточка для модели Gemma
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.gemma),
                                    contentDescription = "Gemma Icon",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Gemma 3 270M IT QAT",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "Компактная модель для инструкций с оптимизацией QAT",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (isGemmaDownloading) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { gemmaDownloadProgress },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = gemmaDownloadStatus,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isGemmaDownloaded) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isGemmaDownloading = true
                                                gemmaDownloadStatus = "Проверка доступности модели..."
                                                gemmaDownloadProgress = 0.1f

                                                try {
                                                    gemmaDownloadStatus = "Начинаем загрузку модели..."
                                                    gemmaDownloadProgress = 0.0f

                                                    val modelPath = modelDownloader.downloadModel(
                                                        gemmaModelName,
                                                        gemmaFileName,
                                                        object : ProgressListener {
                                                            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                                                                if (contentLength > 0) {
                                                                    val progress = (bytesRead.toFloat() / contentLength.toFloat())
                                                                    gemmaDownloadProgress = progress
                                                                    gemmaDownloadStatus = "Загрузка: ${(progress * 100).toInt()}% (${bytesRead / 1024 / 1024}MB / ${contentLength / 1024 / 1024}MB)"
                                                                }
                                                            }
                                                        }
                                                    )

                                                    gemmaDownloadedPath = modelPath
                                                    isGemmaDownloaded = true
                                                    gemmaDownloadStatus = "Модель успешно загружена!"
                                                    gemmaFileSize = modelDownloader.getModelFileSize(gemmaFileName)
                                                } catch (e: Exception) {
                                                    gemmaDownloadStatus = "Ошибка загрузки: ${e.message}"
                                                } finally {
                                                    isGemmaDownloading = false
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isGemmaDownloading
                                    ) {
                                        Text("Загрузить Gemma")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    modelDownloader.deleteModel(gemmaModelName, gemmaFileName)
                                                    isGemmaDownloaded = false
                                                    gemmaDownloadedPath = null
                                                    gemmaFileSize = -1
                                                    gemmaDownloadStatus = "Модель удалена"
                                                } catch (e: Exception) {
                                                    gemmaDownloadStatus = "Ошибка удаления: ${e.message}"
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Text("Удалить модель")
                                    }
                                }

                                if (gemmaDownloadedPath != null) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val result = rustInterface.runCandleExample()
                                                    gemmaDownloadStatus = "Результат: $result"
                                                } catch (e: Exception) {
                                                    gemmaDownloadStatus = "Ошибка выполнения: ${e.message}"
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isGemmaDownloading
                                    ) {
                                        Text("Тест модели")
                                    }
                                }
                            }

                            if (gemmaDownloadedPath != null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Модель загружена",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Размер: ${if (gemmaFileSize > 0) "${gemmaFileSize / 1024 / 1024}MB" else "Неизвестно"}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Файл: $gemmaFileName",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
