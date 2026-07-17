package com.thejaustin.sharemove.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.thejaustin.sharemove.data.model.AppEntry

@Composable
fun AppToggleCard(
    entry: AppEntry,
    showDisableOption: Boolean,
    onToggleHidden: () -> Unit,
    onToggleDisabled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isHidden || entry.isDisabled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            entry.icon?.let { drawable ->
                val density = androidx.compose.ui.platform.LocalDensity.current
                val iconBitmap = remember(drawable, density) {
                    val sizePx = (40 * density.density).toInt().coerceAtLeast(1)
                    drawable.toBitmap(sizePx, sizePx).asImageBitmap()
                }
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text  = entry.label,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text  = entry.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (entry.isDisabled) {
                    Text(
                        text  = "Fully disabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (entry.isHidden) {
                    Text(
                        text  = "Hidden from chooser",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = if (entry.isHidden) "Hidden" else "Visible",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked         = !entry.isHidden && !entry.isDisabled,
                        onCheckedChange = { onToggleHidden() },
                        enabled         = !entry.isDisabled,
                    )
                }

                if (showDisableOption) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier         = Modifier.padding(top = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Fully disable",
                            modifier = Modifier.size(14.dp),
                            tint     = if (entry.isDisabled)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text  = if (entry.isDisabled) "Disabled" else "Enabled",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.isDisabled)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked         = !entry.isDisabled,
                            onCheckedChange = { onToggleDisabled() },
                        )
                    }
                }
            }
        }
    }
}
