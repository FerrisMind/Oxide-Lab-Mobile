package com.oxidelabmobile.ui.screens.setup

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.oxidelabmobile.R
import com.oxidelabmobile.ui.theme.OxideLabMobileTheme
import com.oxidelabmobile.RustInterface
import com.oxidelabmobile.ui.theme.Spacing
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

// removed initialization progress and related coroutines

@Composable
fun ModelSetupScreen(
    onModelLoaded: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    openedFromMenu: Boolean = false
) {
    // No automatic initialization progress. Show static model list.
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("oxide_prefs", Context.MODE_PRIVATE) }
    var pendingProceed by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { results ->
            val granted = results.values.all { it }
            if (granted) {
                pendingProceed?.invoke()
            }
            pendingProceed = null
        }
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (openedFromMenu) {
                // Opened from menu: always show Back
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF1B1C3A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Medium)
                ) {
                    Text("Назад")
                }
            } else {
                // Opened from startup flow: always show Skip button
                TextButton(
                    onClick = {
                        prefs.edit().putBoolean("first_launch", false).apply()
                        // Skip setup and go directly to chat
                        onModelLoaded()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF1B1C3A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Medium)
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
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large)
        ) {
            // Available models
            ModelInfoCard(
                modelName = "Gemma 3 270M IT QAT ",
                modelSize = "14 GB",
                parameters = "70B",
                isCompatible = true,
                modifier = Modifier.fillMaxWidth(),
                iconRes = R.drawable.gemma
            )

            QwenModelCard(
                onDownloaded = { localPath ->
                    val ctx = context
                    val hasWrite = ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    val hasRead = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

                    val proceed: () -> Unit = {
                        val storageRoot = Environment.getExternalStorageDirectory()
                        val modelsDir = File(storageRoot, "Oxide Lab/models")
                        if (!modelsDir.exists()) modelsDir.mkdirs()

                        val source = File(localPath)
                        val target = File(modelsDir, source.name)
                        try {
                            copyFile(source, target)
                            prefs.edit().putString("qwen_model_path", target.absolutePath).apply()
                        } catch (_: Exception) {
                            prefs.edit().putString("qwen_model_path", localPath).apply()
                        }
                    }

                    if (hasWrite && hasRead) {
                        proceed()
                    } else {
                        pendingProceed = proceed
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Start action handled elsewhere; model cards are selectable to begin loading
        }
    }
}

@Composable
private fun ModelInfoCard(
    modelName: String,
    modelSize: String,
    parameters: String,
    isCompatible: Boolean,
    modifier: Modifier = Modifier,
    iconRes: Int? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            // Model name with optional icon
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                iconRes?.let { res ->
                    Icon(painter = painterResource(id = res), contentDescription = "$modelName icon", tint = MaterialTheme.colorScheme.onSurface)
                }

                Text(
                    text = modelName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Model details
            Text(
                text = "Размер: $modelSize • Параметры: $parameters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            // Compatibility indicator removed as requested
        }
    }
}

@Composable
private fun QwenModelCard(
    onDownloaded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    // Fallback URL (replace with real HF URL if desired)
    val fallbackUrl = "" // e.g. "https://huggingface.co/your-repo/blob/main/qwen.gguf"

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Icon(painter = painterResource(id = R.drawable.qwen), contentDescription = "Qwen icon", tint = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "Qwen3 0.6B GGUF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Размер: ~0.7 GB • Формат: GGUF",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        if (!isDownloading) {
                            resultMessage = ""
                            isDownloading = true
                            scope.launch {
                                try {
                                    // call native loader with a timeout to avoid indefinite blocking
                                    val cacheDir = context.cacheDir.absolutePath
                                    val path = withContext(Dispatchers.IO) {
                                        kotlinx.coroutines.withTimeoutOrNull(120_000) {
                                            try {
                                                RustInterface.instance.downloadQwenModelToPath(cacheDir)
                                            } catch (t: Throwable) {
                                                // Catch any throwable coming out of JNI boundary
                                                null
                                            }
                                        }
                                    }

                                    if (path == null) {
                                        resultMessage = "Ошибка нативной загрузки или таймаут"
                                    } else if (path.startsWith("Error:")) {
                                        resultMessage = "Ошибка загрузки"
                                    } else {
                                        onDownloaded(path)
                                        resultMessage = "Модель загружена"
                                    }
                                } catch (e: Exception) {
                                    resultMessage = "Ошибка: ${e.message}"
                                } finally {
                                    isDownloading = false
                                }
                            }
                        }
                    },
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF1B1C3A)
                    )
                ) {
                    Text(if (isDownloading) "Загрузка..." else "Скачать из Hugging Face")
                }

                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.padding(start = Spacing.Small))
                }

                // Fallback direct download button (visible when native failed or when URL provided)
                if (!isDownloading && (resultMessage.isNotBlank() || fallbackUrl.isNotBlank())) {
                    TextButton(onClick = {
                        if (fallbackUrl.isBlank()) {
                            resultMessage = "Прямой URL не задан"
                            return@TextButton
                        }
                        isDownloading = true
                        resultMessage = ""
                        scope.launch {
                            try {
                                val savedPath = withContext(Dispatchers.IO) {
                                    downloadDirectly(context, fallbackUrl)
                                }
                                onDownloaded(savedPath)
                                resultMessage = "Модель загружена напрямую"
                            } catch (e: Exception) {
                                resultMessage = "Ошибка прямой загрузки: ${e.message}"
                            } finally {
                                isDownloading = false
                            }
                        }
                    }) {
                        Text("Скачать напрямую")
                    }
                }
            }

            if (resultMessage.isNotBlank()) {
                Text(
                    text = resultMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Простая реализация прямой загрузки без внешних зависимостей
@Throws(Exception::class)
private fun downloadDirectly(context: Context, url: String): String {
    val filename = "qwen_direct.gguf"
    val dest = File(context.cacheDir, filename)
    java.net.URL(url).openStream().use { input ->
        dest.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return dest.absolutePath
}

private fun copyFile(from: File, to: File) {
    if (from.absolutePath == to.absolutePath) return
    FileInputStream(from).channel.use { src: FileChannel ->
        // Ensure parent dirs
        to.parentFile?.mkdirs()
        FileOutputStream(to).channel.use { dst: FileChannel ->
            dst.transferFrom(src, 0, src.size())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ModelSetupScreenPreview() {
    OxideLabMobileTheme {
        ModelSetupScreen(
            onModelLoaded = {},
            onCancel = {}
        )
    }
}
