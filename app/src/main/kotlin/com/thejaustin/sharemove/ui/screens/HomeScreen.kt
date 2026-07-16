package com.thejaustin.sharemove.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thejaustin.sharemove.data.model.IntentCategory
import com.thejaustin.sharemove.ui.components.AppToggleCard
import com.thejaustin.sharemove.ui.components.ShizukuBanner
import com.thejaustin.sharemove.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
) {
    val state            by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            ShizukuBanner(
                available           = state.shizukuAvailable,
                hasPermission       = state.shizukuPermission,
                onRequestPermission = viewModel::requestShizukuPermission,
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
                                entry             = entry,
                                showDisableOption = state.canExecute,
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
