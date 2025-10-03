package com.oxidelabmobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.oxidelabmobile.ui.theme.Dimensions
import com.oxidelabmobile.ui.theme.OxideLabMobileTheme
import com.oxidelabmobile.ui.theme.Spacing

@Composable
fun OxideProgressBar(
    progress: Float,
    stage: String,
    modifier: Modifier = Modifier,
    progressText: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Progress: ${(progress * 100).toInt()}%, Stage: $stage"
            }
    ) {
        // Stage label
        Text(
            text = stage,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = Spacing.Small)
        )
        
        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimensions.ProgressBarHeight),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        // Progress text (optional)
        progressText?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = Spacing.Small)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressBarPreview() {
    OxideLabMobileTheme {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.Medium)
        ) {
            OxideProgressBar(
                progress = 0.3f,
                stage = "Анализ модели...",
                progressText = "Загружено 23% (156 MB из 678 MB)"
            )
            
            OxideProgressBar(
                progress = 0.7f,
                stage = "Загрузка весов...",
                progressText = "Загружено 67% (456 MB из 678 MB)"
            )
            
            OxideProgressBar(
                progress = 1.0f,
                stage = "Готов к работе"
            )
        }
    }
}
