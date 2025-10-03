package com.oxidelabmobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.oxidelabmobile.R
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun ComposerField(
    onSend: (String) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    modelName: String = ""
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }

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
            // Left group: logo + model name
            Row(modifier = Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.qwen),
                    contentDescription = "Model logo",
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                )

                Text(
                    text = modelName,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                    maxLines = 1
                )
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


