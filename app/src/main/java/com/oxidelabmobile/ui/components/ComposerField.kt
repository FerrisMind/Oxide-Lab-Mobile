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
import androidx.compose.ui.res.painterResource
import com.oxidelabmobile.R
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.Color
import com.oxidelabmobile.ui.theme.Spacing
import com.oxidelabmobile.DownloadedModel

@Composable
fun ComposerField(
    onSend: (String) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    modelName: String = "",
    modelIconResId: Int = R.drawable.qwen,
    availableModels: List<DownloadedModel> = emptyList(),
    onModelSelected: (DownloadedModel) -> Unit = {}
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Опишите задачу...") },
            singleLine = false,
            minLines = 1,
            maxLines = 4,
            textStyle = MaterialTheme.typography.bodyLarge
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
                    modifier = Modifier.height(48.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = modelIconResId),
                            contentDescription = "Model logo",
                            modifier = Modifier.size(if (modelIconResId == com.oxidelabmobile.R.drawable.gemma) 32.dp else 24.dp)
                        )
                        Text(
                            text = modelName,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_down),
                            contentDescription = "Dropdown arrow",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableModels.forEach { model ->
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = model.iconResId),
                                        contentDescription = "${model.name} Icon",
                                        modifier = Modifier.size(if (model.iconResId == com.oxidelabmobile.R.drawable.gemma) 28.dp else 20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
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
                    tint = Color(0xFF1B1C3A)
                )
                }
            }
        }
    }
}


