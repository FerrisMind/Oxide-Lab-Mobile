package com.oxidelabmobile.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oxidelabmobile.ui.components.OxideProgressBar
import androidx.compose.ui.res.painterResource
import com.oxidelabmobile.R
import com.oxidelabmobile.ui.components.StatusDot
import com.oxidelabmobile.ui.theme.Dimensions
import com.oxidelabmobile.ui.theme.OxideLabMobileTheme
import com.oxidelabmobile.ui.theme.Spacing
import kotlinx.coroutines.delay

@Composable
fun ModelSetupScreen(
    onModelLoaded: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var stage by remember { mutableStateOf("Анализ модели...") }
    var isComplete by remember { mutableStateOf(false) }
    
    // Simulate loading process
    LaunchedEffect(Unit) {
        val stages = listOf(
            "Анализ модели..." to 0.2f,
            "Загрузка весов..." to 0.6f,
            "Инициализация..." to 0.9f,
            "Готов к работе" to 1.0f
        )
        
        stages.forEach { (stageText, targetProgress) ->
            stage = stageText
            while (progress < targetProgress) {
                delay(100)
                progress += 0.02f
            }
            progress = targetProgress
            delay(500)
        }
        
        isComplete = true
        delay(1000)
        onModelLoaded()
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!isComplete) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF1B1C3A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Medium)
                        .semantics {
                            contentDescription = "Cancel model loading"
                        }
                ) {
                    Text("Отмена")
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
            // Progress section
            OxideProgressBar(
                progress = progress,
                stage = stage,
                progressText = if (!isComplete) {
                    "Загружено ${(progress * 100).toInt()}% (${(progress * 678).toInt()} MB из 678 MB)"
                } else null
            )
            
            // Available models
            ModelInfoCard(
                modelName = "Gemma 3",
                modelSize = "14 GB",
                parameters = "70B",
                isCompatible = true,
                iconRes = R.drawable.gemma,
                modifier = Modifier.fillMaxWidth()
            )

            ModelInfoCard(
                modelName = "Qwen 3",
                modelSize = "678 MB",
                parameters = "175B",
                isCompatible = true,
                iconRes = R.drawable.qwen,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (isComplete) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onModelLoaded,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFF1B1C3A)
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Start using the model"
                        }
                    ) {
                        Text("Начать работу")
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelInfoCard(
    modelName: String,
    modelSize: String,
    parameters: String,
    isCompatible: Boolean,
    iconRes: Int? = null,
    modifier: Modifier = Modifier
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
