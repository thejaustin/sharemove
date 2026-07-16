package com.thejaustin.sharemove.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thejaustin.sharemove.data.model.HideMode
import com.thejaustin.sharemove.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                        text  = when {
                            state.shizukuAvailable && state.shizukuPermission -> "Connected"
                            state.shizukuAvailable                            -> "Running — permission needed"
                            else                                              -> "Not running"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.shizukuAvailable && state.shizukuPermission)
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

                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = state.hideMode == HideMode.SUSPEND,
                            onClick  = { viewModel.setHideMode(HideMode.SUSPEND) },
                            shape    = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) { Text("Suspend") }
                        SegmentedButton(
                            selected = state.hideMode == HideMode.COMPONENT,
                            onClick  = { viewModel.setHideMode(HideMode.COMPONENT) },
                            shape    = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) { Text("Component") }
                    }

                    val modeText = when (state.hideMode) {
                        HideMode.SUSPEND ->
                            "Package suspend — the whole app is paused, data intact. " +
                                "Its icon is greyed out in the launcher."
                        HideMode.COMPONENT ->
                            "Component-level — only the activity handling this intent is " +
                                "disabled. Surgical, but can affect the app's launcher entry " +
                                "if it routes everything through one activity."
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
