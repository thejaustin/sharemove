package com.thejaustin.sharemove.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thejaustin.sharemove.data.model.HideMode
import com.thejaustin.sharemove.data.repository.Backend
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
    val defaultApps by viewModel.defaultApps.collectAsStateWithLifecycle()
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
                    Text("Execution backend", style = MaterialTheme.typography.titleMedium)

                    Backend.entries.forEach { backend ->
                        val isSelected = state.selectedBackend == backend
                        val isAvailable = when (backend) {
                            Backend.SHIZUKU, Backend.SHIZUKU_PLUS -> state.shizukuAvailable && state.shizukuPermission
                            Backend.ROOT -> state.rootAvailable
                            Backend.DEVICE_OWNER -> state.deviceOwnerActive
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setBackend(backend) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setBackend(backend) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                val title = when (backend) {
                                    Backend.SHIZUKU_PLUS -> "Shizuku+ (Direct Binder wrapper)"
                                    Backend.SHIZUKU -> "Shizuku (OG Shell commands)"
                                    Backend.ROOT -> "Root access (su command)"
                                    Backend.DEVICE_OWNER -> "Device Owner (Enterprise admin API)"
                                }
                                val statusText = if (isAvailable) "Active" else "Unavailable (needs setup)"
                                Text(title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            if (state.isOneUi) {
                val homeUpInstalled = remember {
                    try {
                        context.packageManager.getPackageInfo("com.samsung.android.app.homestar", 0)
                        true
                    } catch (_: Exception) {
                        try {
                            context.packageManager.getPackageInfo("com.samsung.android.goodlock", 0)
                            true
                        } catch (_: Exception) {
                            false
                        }
                    }
                }

                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Samsung share sheet options", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "One UI's share sheet has native customization options via the official Home Up module. You can use it to disable Direct Share (contacts) and clean up other Samsung-specific share targets.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = {
                            val intent = context.packageManager.getLaunchIntentForPackage("com.samsung.android.app.homestar")
                                ?: context.packageManager.getLaunchIntentForPackage("com.samsung.android.goodlock")
                            if (intent != null) {
                                context.startActivity(intent)
                            } else {
                                val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("samsungapps://ProductDetail/com.samsung.android.goodlock"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                try {
                                    context.startActivity(storeIntent)
                                } catch (_: Exception) {
                                    uriHandler.openUri("https://galaxystore.samsung.com/detail/com.samsung.android.goodlock")
                                }
                            }
                        }) {
                            Text(if (homeUpInstalled) "Open Home Up / Good Lock" else "Get Good Lock from Galaxy Store")
                        }
                    }
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Default system applications", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Query and modify default handlers for main system intent roles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. Browser row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Browser", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = defaultApps.browser?.let { "${it.label} (${it.packageName})" } ?: "None selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }) {
                            Text("Configure")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 2. Launcher row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Launcher", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = defaultApps.launcher?.let { "${it.label} (${it.packageName})" } ?: "None selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                try {
                                    context.startActivity(fallback)
                                } catch (_: Exception) {}
                            }
                        }) {
                            Text("Configure")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 3. Dialer row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Phone app", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = defaultApps.dialer?.let { "${it.label} (${it.packageName})" } ?: "None selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }) {
                            Text("Configure")
                        }
                    }
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

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Danger Zone",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "This will restore all hidden and disabled apps and components back to their default visible states, across all categories.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        onClick = { viewModel.resetAllChanges() }
                    ) {
                        Text("Restore all apps")
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
