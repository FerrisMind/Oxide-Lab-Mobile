package com.oxidelabmobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.res.painterResource
import com.oxidelabmobile.R
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.oxidelabmobile.ui.theme.Dimensions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.oxidelabmobile.ui.theme.Spacing
import com.oxidelabmobile.DownloadedModel

@Composable
fun ComposerField(
    onSend: (String) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    modelName: String = "Нет модели",
    modelIconResId: Int = R.drawable.sparkle,
    availableModels: List<DownloadedModel> = emptyList(),
    onModelSelected: (DownloadedModel) -> Unit = {}
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var expanded by remember { mutableStateOf(false) }
    
    // Interaction source для отслеживания фокуса
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
        Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(
                    text = "Опишите задачу...",
                    style = TextStyle(
                        color = Color.Gray.copy(alpha = 0.6f), // Тусклый серый цвет
                        fontStyle = FontStyle.Italic // Курсивный стиль
                    )
                ) 
            },
            singleLine = false,
            minLines = 1,
            maxLines = 3,
            textStyle = MaterialTheme.typography.bodyLarge,
            interactionSource = interactionSource,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left group: model selector button
            Box(modifier = Modifier.padding(start = 8.dp)) {
                Button(
                    onClick = { 
                        if (availableModels.isNotEmpty()) {
                            expanded = true 
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 200.dp, max = 250.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = modelIconResId),
                            contentDescription = "Model logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = modelName,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.caret_up),
                            contentDescription = "Model selector",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.widthIn(min = 200.dp, max = 250.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    availableModels.forEach { model ->
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        painter = painterResource(id = model.iconResId),
                                        contentDescription = "${model.name} Icon",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = model.name,
                                            fontSize = 14.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                        )
                                        Text(
                                            text = "${model.size / 1024 / 1024}MB",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onModelSelected(model)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Right-side controls
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSettings() }) {
                    Icon(painter = painterResource(id = R.drawable.sliders_horizontal), contentDescription = "Model settings")
                }

                FloatingActionButton(
                    onClick = { if (text.text.isNotBlank()) { onSend(text.text); text = TextFieldValue("") } },
                    modifier = Modifier
                        .width(Dimensions.FABSize),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation()
                ) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_up),
                    contentDescription = "Send message",
                    tint = androidx.compose.ui.graphics.Color(0xFF1B1C3A)
                )
                }
            }
        }
    }
}


