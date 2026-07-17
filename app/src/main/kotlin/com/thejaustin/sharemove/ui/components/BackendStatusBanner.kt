package com.thejaustin.sharemove.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thejaustin.sharemove.data.model.HideMode
import com.thejaustin.sharemove.data.repository.Backend

@Composable
fun BackendStatusBanner(
    selectedBackend: Backend,
    shizukuAvailable: Boolean,
    shizukuPermission: Boolean,
    rootAvailable: Boolean,
    deviceOwnerActive: Boolean,
    suspendIneffective: Boolean,
    onRequestShizukuPermission: () -> Unit,
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = when (selectedBackend) {
        Backend.SHIZUKU, Backend.SHIZUKU_PLUS -> {
            if (shizukuAvailable && shizukuPermission) null
            else if (!shizukuAvailable) "Shizuku is not running. Start Shizuku to manage the chooser sheet."
            else "ShaRemove needs Shizuku permission to hide apps."
        }
        Backend.ROOT -> {
            if (rootAvailable) null
            else "Root access is not available. Grant root access or select another backend in Settings."
        }
        Backend.DEVICE_OWNER -> {
            if (deviceOwnerActive) null
            else "Device Owner is not active. Provision the app via ADB or select another backend in Settings."
        }
    }

    Column(modifier = modifier) {
        if (message != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        if ((selectedBackend == Backend.SHIZUKU || selectedBackend == Backend.SHIZUKU_PLUS) &&
                            shizukuAvailable && !shizukuPermission
                        ) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onRequestShizukuPermission) {
                                Text("Grant permission")
                            }
                        }
                    }
                }
            }
        }

        if (suspendIneffective) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "On Samsung One UI, Suspend mode does not hide apps from the share sheet. Switch to Component mode in Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = onGoToSettings) {
                            Text("Go to Settings")
                        }
                    }
                }
            }
        }
    }
}
