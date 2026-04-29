package com.thejaustin.sharemove

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thejaustin.sharemove.ui.screens.HomeScreen
import com.thejaustin.sharemove.ui.screens.SettingsScreen
import com.thejaustin.sharemove.ui.theme.ShaRemoveTheme
import com.thejaustin.sharemove.viewmodel.MainViewModel
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity(), Shizuku.OnRequestPermissionResultListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Shizuku.addRequestPermissionResultListener(this)
        setContent {
            ShaRemoveTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShaRemoveApp()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(this)
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        // ViewModel re-checks on next composition
    }
}

@Composable
private fun ShaRemoveApp() {
    val vm: MainViewModel = viewModel()
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(viewModel = vm, onBack = { showSettings = false })
    } else {
        HomeScreen(viewModel = vm, onNavigateToSettings = { showSettings = true })
    }
}
