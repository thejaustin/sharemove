package com.thejaustin.sharemove.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thejaustin.sharemove.data.model.AppEntry
import com.thejaustin.sharemove.data.model.HideMode
import com.thejaustin.sharemove.data.model.IntentCategory
import com.thejaustin.sharemove.data.repository.ChooserRepository
import com.thejaustin.sharemove.data.repository.PreferencesRepository
import com.thejaustin.sharemove.shizuku.RootHelper
import com.thejaustin.sharemove.shizuku.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val apps: List<AppEntry>     = emptyList(),
    val isLoading: Boolean       = true,
    val errorMessage: String?    = null,
    val shizukuAvailable: Boolean  = false,
    val shizukuPermission: Boolean = false,
    val rootAvailable: Boolean   = false,
    val hideMode: HideMode       = HideMode.SUSPEND,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val chooserRepo  = ChooserRepository(app)
    private val prefsRepo    = PreferencesRepository(app)

    private val _selectedCategory = MutableStateFlow(IntentCategory.APK_INSTALLER)
    val selectedCategory: StateFlow<IntentCategory> = _selectedCategory.asStateFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    init {
        refreshShizuku()
        viewModelScope.launch {
            _selectedCategory.collectLatest { category ->
                loadApps(category)
            }
        }
    }

    fun selectCategory(category: IntentCategory) {
        _selectedCategory.value = category
    }

    fun refreshShizuku() {
        _uiState.update { it.copy(
            shizukuAvailable  = ShizukuHelper.isAvailable,
            shizukuPermission = ShizukuHelper.hasPermission,
            rootAvailable     = RootHelper.isAvailable,
            hideMode          = chooserRepo.hideMode,
        ) }
    }

    fun requestShizukuPermission() = ShizukuHelper.requestPermission(1001)

    private fun loadApps(category: IntentCategory) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            combine(
                prefsRepo.hiddenPackages(category),
                prefsRepo.disabledPackages(category),
            ) { hidden, disabled -> Pair(hidden, disabled) }
                .take(1)
                .collect { (hidden, disabled) ->
                    val apps = withContext(Dispatchers.IO) {
                        chooserRepo.queryApps(category, hidden, disabled)
                    }
                    _uiState.update { it.copy(apps = apps, isLoading = false) }
                }
        }
    }

    fun toggleHidden(entry: AppEntry) {
        viewModelScope.launch {
            val targetHidden = !entry.isHidden
            val result = withContext(Dispatchers.IO) {
                if (targetHidden) {
                    if (uiState.value.hideMode == HideMode.COMPONENT && entry.componentName != null)
                        chooserRepo.hideComponent(entry.componentName)
                    else
                        chooserRepo.hidePackage(entry.packageName)
                } else {
                    if (uiState.value.hideMode == HideMode.COMPONENT && entry.componentName != null)
                        chooserRepo.showComponent(entry.componentName)
                    else
                        chooserRepo.showPackage(entry.packageName)
                }
            }
            result.fold(
                onSuccess = {
                    prefsRepo.setHidden(_selectedCategory.value, entry.packageName, targetHidden)
                    loadApps(_selectedCategory.value)
                },
                onFailure = { _snackbar.emit(it.message ?: "Command failed") },
            )
        }
    }

    fun toggleDisabled(entry: AppEntry) {
        viewModelScope.launch {
            val targetDisabled = !entry.isDisabled
            val result = withContext(Dispatchers.IO) {
                if (targetDisabled) {
                    if (uiState.value.rootAvailable)
                        chooserRepo.disablePackageRoot(entry.packageName)
                    else
                        chooserRepo.disablePackage(entry.packageName)
                } else {
                    if (uiState.value.rootAvailable)
                        chooserRepo.enablePackageRoot(entry.packageName)
                    else
                        chooserRepo.enablePackage(entry.packageName)
                }
            }
            result.fold(
                onSuccess = {
                    prefsRepo.setDisabled(_selectedCategory.value, entry.packageName, targetDisabled)
                    loadApps(_selectedCategory.value)
                },
                onFailure = { _snackbar.emit(it.message ?: "Command failed") },
            )
        }
    }
}
