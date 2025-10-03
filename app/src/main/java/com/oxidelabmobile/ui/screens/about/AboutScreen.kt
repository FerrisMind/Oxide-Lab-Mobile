package com.oxidelabmobile.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oxidelabmobile.BuildConfig
import com.oxidelabmobile.ui.theme.OxideLabMobileTheme
import com.oxidelabmobile.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("О программе") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium, Alignment.CenterVertically)
        ) {
            // App icon
            Image(
                painter = painterResource(id = com.oxidelabmobile.R.mipmap.ic_launcher),
                contentDescription = "Иконка приложения",
                modifier = Modifier.size(96.dp)
            )
            Text(
                text = "Oxide Lab Mobile",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Версия ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Это мобильное приложение — демонстрация возможностей Oxide AI, работающего на базе передовых языковых моделей",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Разработано FerrisMind",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    OxideLabMobileTheme {
        AboutScreen(onNavigateUp = {})
    }
}
