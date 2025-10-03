package com.oxidelabmobile.ui.screens.empty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oxidelabmobile.ui.components.OxideExtendedFAB
import com.oxidelabmobile.ui.theme.Dimensions
import com.oxidelabmobile.ui.theme.OxideLabMobileTheme
import com.oxidelabmobile.ui.theme.Spacing

@Composable
fun EmptyStateScreen(
    onSelectModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            OxideExtendedFAB(
                text = "Выбрать модель",
                onClick = onSelectModel,
                modifier = Modifier.padding(Spacing.Medium)
            )
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                // Logo/Icon
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Oxide Lab Logo",
                    modifier = Modifier
                        .size(Dimensions.LogoSize)
                        .semantics {
                            contentDescription = "Oxide Lab molecular logo"
                        },
                    tint = MaterialTheme.colorScheme.primary
                )
                
                // Title
                Text(
                    text = "Oxide Lab",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                
                // Subtitle
                Text(
                    text = "Загрузите модель для начала работы",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.Large)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStateScreenPreview() {
    OxideLabMobileTheme {
        EmptyStateScreen(
            onSelectModel = {}
        )
    }
}
