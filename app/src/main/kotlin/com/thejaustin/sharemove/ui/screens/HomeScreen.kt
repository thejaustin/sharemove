package com.thejaustin.sharemove.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thejaustin.sharemove.data.model.IntentCategory
import com.thejaustin.sharemove.ui.components.AppToggleCard
import com.thejaustin.sharemove.ui.components.ShizukuBanner
import com.thejaustin.sharemove.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
) {
    val state           by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbar.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ShaRemove") },
                actions = {
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
                available          = state.shizukuAvailable,
                hasPermission      = state.shizukuPermission,
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

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(state.apps, key = { it.packageName }) { entry ->
                        AppToggleCard(
                            entry             = entry,
                            showDisableOption = state.shizukuAvailable && state.shizukuPermission,
                            onToggleHidden    = { viewModel.toggleHidden(entry) },
                            onToggleDisabled  = { viewModel.toggleDisabled(entry) },
                        )
                    }
                }
            }
        }
    }
}
