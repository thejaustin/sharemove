package com.thejaustin.sharemove.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thejaustin.sharemove.data.model.HideMode
import com.thejaustin.sharemove.viewmodel.MainViewModel

private const val REPO_URL = "https://github.com/thejaustin/sharemove"

private data class Credit(val name: String, val license: String, val url: String)

private val credits = listOf(
    Credit("Shizuku API — RikkaApps", "MIT", "https://github.com/RikkaApps/Shizuku-API"),
    Credit("Shizuku — privileged access without root", "Apache-2.0", "https://shizuku.rikka.app"),
    Credit("Jetpack Compose, Material 3 & AndroidX", "Apache-2.0", "https://developer.android.com/jetpack/compose"),
    Credit("Kotlin & kotlinx.coroutines — JetBrains", "Apache-2.0", "https://kotlinlang.org"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val versionName = remember { appVersionName(context) }

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
                .verticalScroll(rememberScrollState())
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
                        HideMode.SUSPEND -> {
                            var desc = "Package suspend — the whole app is paused, data intact. " +
                                    "Its icon is greyed out in the launcher."
                            if (state.isOneUi) {
                                desc += "\n\n⚠️ Note: On Samsung One UI, this mode does not hide apps from the system share sheet. Use Component mode instead."
                            }
                            desc
                        }
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

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text  = "ShaRemove $versionName",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text  = "Free software, licensed GPL-3.0. No ads, no analytics, no network access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { uriHandler.openUri(REPO_URL) }) {
                            Text("Source")
                        }
                        TextButton(onClick = { uriHandler.openUri("$REPO_URL/releases") }) {
                            Text("Releases")
                        }
                        TextButton(onClick = { uriHandler.openUri("$REPO_URL/issues") }) {
                            Text("Report issue")
                        }
                    }
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Acknowledgements", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text  = "ShaRemove stands on these projects:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    credits.forEach { credit ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { uriHandler.openUri(credit.url) }
                                .padding(vertical = 6.dp),
                        ) {
                            Text(credit.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text  = "${credit.license} license",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun appVersionName(context: Context): String = try {
    @Suppress("DEPRECATION")
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
} catch (_: Exception) {
    "unknown"
}
