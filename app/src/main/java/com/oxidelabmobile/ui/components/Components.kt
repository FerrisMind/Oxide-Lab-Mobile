package com.oxidelabmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.oxidelabmobile.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.oxidelabmobile.ui.theme.Dimensions
import com.oxidelabmobile.ui.theme.OxideLabMobileTheme
import com.oxidelabmobile.ui.theme.Spacing
import com.oxidelabmobile.ui.theme.Status
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun OxideExtendedFAB(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(Dimensions.ExtendedFABHeight)
            .semantics {
                contentDescription = text
            },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color(0xFF1B1C3A)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
@Suppress("unused")
fun OxideFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .size(Dimensions.FABSize)
            .semantics {
                contentDescription = "Send message"
            },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color(0xFF1B1C3A)
    ) {
        content()
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun LabHeader(
    title: String,
    modifier: Modifier = Modifier,
    statusText: String = "",
    isOnline: Boolean = true,
    onMenuClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    // Respect system status bar and notches using Compose insets helper
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimensions.HeaderHeight + Spacing.Large)
            .statusBarsPadding()
            .padding(top = Spacing.Medium)
            .padding(horizontal = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // left menu button -> triggers external drawer
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .size(Dimensions.TouchTargetMin)
                .semantics {
                    contentDescription = "Menu"
                }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.sidebar_simple),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // centered title
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        // right spacer to balance left icon
        Spacer(modifier = Modifier.width(Dimensions.TouchTargetMin))
    }
}

@Composable
fun StatusDot(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isOnline) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .size(Status.DotSize)
            .clip(CircleShape)
            .background(color)
            .semantics {
                contentDescription = if (isOnline) "Online" else "Offline"
            }
    )
}

@Composable
fun MessageBubble(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
    timestamp: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUser) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(Spacing.Medium)
        )

        timestamp?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(
                    start = Spacing.Medium,
                    end = Spacing.Medium,
                    bottom = Spacing.Small
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ComponentsPreview() {
    OxideLabMobileTheme {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            OxideExtendedFAB(
                text = "Выбрать модель",
                onClick = {}
            )

            LabHeader(
                title = "Qwen 3",
                statusText = "Готов",
                isOnline = true
            )

            StatusDot(isOnline = true)

            MessageBubble(
                text = "Привет! Как дела?",
                isUser = true
            )

            MessageBubble(
                text = "Привет! Всё отлично, спасибо за вопрос!",
                isUser = false
            )
        }
    }
}
