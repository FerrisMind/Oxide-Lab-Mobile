package com.oxidelabmobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.oxidelabmobile.ui.theme.OxideLabMobileTheme
import com.oxidelabmobile.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    
    if (isVisible) {
        LaunchedEffect(Unit) {
            sheetState.show()
        }
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Medium)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Настройки модели",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.semantics {
                            contentDescription = "Close settings"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Settings content
                Text(
                    text = "Здесь будут настройки модели: температура, максимальная длина токенов, и другие параметры.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.Small)
                )
                
                // Placeholder settings
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    Text(
                        text = "• Температура: 0.7",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Максимальные токены: 2048",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // User parameters checkbox
                    var userParamsChecked by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = userParamsChecked,
                            onCheckedChange = { userParamsChecked = it },
                            modifier = Modifier.semantics { contentDescription = "Пользовательские параметры" }
                        )
                        Text(
                            text = "Пользовательские параметры",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Spacing.Small)
                        )
                    }
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.Medium)
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageContextMenu(
    isVisible: Boolean,
    onCopy: () -> Unit,
    onContinue: () -> Unit,
    onRate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    
    if (isVisible) {
        LaunchedEffect(Unit) {
            sheetState.show()
        }
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Medium)
            ) {
                Text(
                    text = "Действия с сообщением",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Spacing.Medium)
                )
                
                TextButton(
                    onClick = {
                        onCopy()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Копировать")
                }
                
                TextButton(
                    onClick = {
                        onContinue()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Продолжить тему")
                }
                
                TextButton(
                    onClick = {
                        onRate()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Оценить")
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.Medium)
                ) {
                    Text("Отмена")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GestureComponentsPreview() {
    OxideLabMobileTheme {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            ModelSettingsSheet(
                isVisible = true,
                onDismiss = {}
            )
            
            MessageContextMenu(
                isVisible = true,
                onCopy = {},
                onContinue = {},
                onRate = {},
                onDismiss = {}
            )
        }
    }
}
