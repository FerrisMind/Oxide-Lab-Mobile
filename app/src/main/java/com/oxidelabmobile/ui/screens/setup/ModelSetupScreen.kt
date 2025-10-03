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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.oxidelabmobile.ui.theme.Spacing

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
                modelName = "Gemma 3",
                modelSize = "14 GB",
                parameters = "70B",
                isCompatible = true,
                modifier = Modifier.fillMaxWidth(),
                iconRes = R.drawable.gemma
            )

            ModelInfoCard(
                modelName = "Qwen 3",
                modelSize = "678 MB",
                parameters = "175B",
                isCompatible = true,
                modifier = Modifier.fillMaxWidth(),
                iconRes = R.drawable.qwen
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
