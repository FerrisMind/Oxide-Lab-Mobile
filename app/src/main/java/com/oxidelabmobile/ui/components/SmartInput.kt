package com.oxidelabmobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oxidelabmobile.ui.theme.OxideLabMobileTheme
import com.oxidelabmobile.ui.theme.Spacing

@Composable
fun SmartInputField(
    inputText: TextFieldValue,
    onInputTextChange: (TextFieldValue) -> Unit,
    isThinkingEnabled: Boolean,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var isCodeMode by remember { mutableStateOf(false) }
    
    // Detect code patterns
    LaunchedEffect(inputText.text) {
        val text = inputText.text
        val codePatterns = listOf(
            "```", "def ", "function ", "class ", "import ", "from ",
            "const ", "let ", "var ", "if ", "for ", "while ",
            "public ", "private ", "protected ", "static ",
            "int ", "string ", "bool ", "void ", "return "
        )
        
        isCodeMode = codePatterns.any { pattern ->
            text.contains(pattern, ignoreCase = true)
        }
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.Small)
    ) {
        // Code mode indicator (textual marker to avoid icon compatibility issues)
        if (isCodeMode) {
            Text(
                text = "</>",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics {
                    contentDescription = "Code mode active"
                }
            )
        }
        
        // Input field and send button
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.Small)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = {
                    Text(
                        text = if (isCodeMode) "Введите код..." else "Опишите задачу...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "Message input field"
                    },
                minLines = 1,
                maxLines = if (isCodeMode) 8 else 4,
                singleLine = false,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.text.isNotBlank()) {
                            onSendMessage()
                        }
                    }
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = if (isCodeMode) FontFamily.Monospace else FontFamily.Default
                )
            )
            
            FloatingActionButton(
                onClick = {
                    keyboardController?.hide()
                    onSendMessage()
                },
                modifier = Modifier.semantics {
                    contentDescription = "Send message"
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message"
                )
            }
        }
        
        // Smart suggestions
        if (isCodeMode) {
            Text(
                text = "💡 Обнаружен код. Используется моноширинный шрифт.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = Spacing.Small)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SmartInputFieldPreview() {
    OxideLabMobileTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.Medium)
        ) {
            SmartInputField(
                inputText = TextFieldValue("def hello_world():"),
                onInputTextChange = {},
                isThinkingEnabled = false,
                onSendMessage = {}
            )
            
            SmartInputField(
                inputText = TextFieldValue("Привет! Как дела?"),
                onInputTextChange = {},
                isThinkingEnabled = true,
                onSendMessage = {}
            )
        }
    }
}
