package com.thejaustin.sharemove.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thejaustin.sharemove.data.model.IntentCategory
import com.thejaustin.sharemove.ui.components.AppToggleCard
import com.thejaustin.sharemove.ui.components.BackendStatusBanner
import com.thejaustin.sharemove.viewmodel.MainViewModel
import com.thejaustin.sharemove.viewmodel.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
) {
    val state            by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context           = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ToggleResult -> {
                    val action = snackbarHostState.showSnackbar(
                        message     = if (event.hidden) "${event.label} hidden" else "${event.label} visible",
                        actionLabel = "Undo",
                        duration    = SnackbarDuration.Short,
                    )
                    if (action == SnackbarResult.ActionPerformed) event.onUndo()
                }
                is UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ShaRemove") },
                actions = {
                    IconButton(onClick = {
                        viewModel.refreshApps()
                        viewModel.refreshCapabilities()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    try {
                        val testIntent = selectedCategory.getTestIntent(context)
                        context.startActivity(testIntent)
                    } catch (e: Exception) {
                        viewModel.showErrorMessage(e.message ?: "Failed to launch chooser")
                    }
                },
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                text = { Text("Test Chooser") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            BackendStatusBanner(
                selectedBackend            = state.selectedBackend,
                shizukuAvailable           = state.shizukuAvailable,
                shizukuPermission          = state.shizukuPermission,
                rootAvailable              = state.rootAvailable,
                deviceOwnerActive          = state.deviceOwnerActive,
                suspendIneffective         = state.suspendIneffective,
                onRequestShizukuPermission = viewModel::requestShizukuPermission,
                onGoToSettings             = onNavigateToSettings,
            )

            // Category tabs
            ScrollableTabRow(
                selectedTabIndex = IntentCategory.entries.indexOf(selectedCategory),
                modifier         = Modifier.fillMaxWidth(),
            ) {
                IntentCategory.entries.forEach { category ->
                    Tab(
                        selected = category == selectedCategory,
                        onClick  = { viewModel.selectCategory(category) },
                        text     = { Text(category.displayName) },
                    )
                }
            }

            OutlinedTextField(
                value         = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape      = RoundedCornerShape(16.dp),
            )

            if (!state.isLoading && state.apps.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val visibleCount = state.apps.count { !it.isHidden && !it.isDisabled }
                    val hiddenCount = state.apps.count { it.isHidden }
                    Text(
                        text = "$visibleCount visible • $hiddenCount hidden",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { viewModel.bulkToggleHidden(true) },
                            enabled = visibleCount > 0
                        ) {
                            Text("Hide All")
                        }
                        TextButton(
                            onClick = { viewModel.bulkToggleHidden(false) },
                            enabled = hiddenCount > 0
                        ) {
                            Text("Unhide All")
                        }
                    }
                }
            }

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.apps.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (state.searchQuery.isNotBlank())
                                "No apps match “${state.searchQuery}”"
                            else
                                "No apps handle this intent",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(state.apps, key = { it.packageName }) { entry ->
                            AppToggleCard(
                                modifier          = Modifier.animateItem(),
                                entry             = entry,
                                showDisableOption = state.canExecute && state.selectedBackend != com.thejaustin.sharemove.data.repository.Backend.DEVICE_OWNER,
                                onToggleHidden    = { viewModel.toggleHidden(entry) },
                                onToggleDisabled  = { viewModel.toggleDisabled(entry) },
                            )
                        }
                    }
                }
            }
        }
    }
}
