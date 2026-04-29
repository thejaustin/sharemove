package com.thejaustin.sharemove.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thejaustin.sharemove.data.model.HideMode
import com.thejaustin.sharemove.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Shizuku", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text  = if (state.shizukuAvailable) "Connected" else "Not running",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.shizukuAvailable)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                    )
                    if (state.shizukuAvailable && !state.shizukuPermission) {
                        Button(onClick = viewModel::requestShizukuPermission) {
                            Text("Grant Shizuku permission")
                        }
                    }
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Root access", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text  = if (state.rootAvailable) "Available" else "Not available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.rootAvailable)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hide mode", style = MaterialTheme.typography.titleMedium)
                    val modeText = when (state.hideMode) {
                        HideMode.COMPONENT -> "Component-level (root) — surgical, app stays fully functional, no launcher effect"
                        HideMode.SUSPEND   -> "Package suspend (Shizuku) — app paused, data intact, icon greyed in launcher"
                    }
                    Text(
                        text  = modeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
