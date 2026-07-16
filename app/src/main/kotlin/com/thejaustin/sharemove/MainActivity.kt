package com.thejaustin.sharemove

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.thejaustin.sharemove.ui.screens.HomeScreen
import com.thejaustin.sharemove.ui.screens.SettingsScreen
import com.thejaustin.sharemove.ui.theme.ShaRemoveTheme
import com.thejaustin.sharemove.viewmodel.MainViewModel
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { _, _ -> viewModel.refreshCapabilities() }
    private val binderReceivedListener =
        Shizuku.OnBinderReceivedListener { viewModel.refreshCapabilities() }
    private val binderDeadListener =
        Shizuku.OnBinderDeadListener { viewModel.refreshCapabilities() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        setContent {
            ShaRemoveTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShaRemoveApp(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // User may have started Shizuku, granted root, or (un)installed apps meanwhile.
        viewModel.refreshCapabilities()
        viewModel.refreshApps()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
    }
}

@Composable
private fun ShaRemoveApp(viewModel: MainViewModel) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showSettings) { showSettings = false }

    if (showSettings) {
        SettingsScreen(viewModel = viewModel, onBack = { showSettings = false })
    } else {
        HomeScreen(viewModel = viewModel, onNavigateToSettings = { showSettings = true })
    }
}
