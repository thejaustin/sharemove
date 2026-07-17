package com.thejaustin.sharemove.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.app.admin.DevicePolicyManager
import android.content.Context
import com.thejaustin.sharemove.data.model.AppEntry
import com.thejaustin.sharemove.data.model.HideMode
import com.thejaustin.sharemove.data.model.IntentCategory
import com.thejaustin.sharemove.data.repository.Backend
import com.thejaustin.sharemove.data.repository.ChooserRepository
import com.thejaustin.sharemove.data.repository.PreferencesRepository
import com.thejaustin.sharemove.shizuku.RootHelper
import com.thejaustin.sharemove.shizuku.ShizukuHelper
import com.thejaustin.sharemove.util.SamsungUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val apps: List<AppEntry>       = emptyList(),
    val searchQuery: String        = "",
    val isLoading: Boolean         = true,
    val shizukuAvailable: Boolean  = false,
    val shizukuPermission: Boolean = false,
    val rootAvailable: Boolean     = false,
    val deviceOwnerActive: Boolean = false,
    val hideMode: HideMode         = HideMode.SUSPEND,
    val selectedBackend: Backend   = Backend.SHIZUKU_PLUS,
    /** True when running on Samsung One UI — suspend mode won't hide from share sheet. */
    val isOneUi: Boolean           = SamsungUtil.isOneUi,
) {
    val canExecute: Boolean get() = when (selectedBackend) {
        Backend.SHIZUKU, Backend.SHIZUKU_PLUS -> shizukuAvailable && shizukuPermission
        Backend.ROOT -> rootAvailable
        Backend.DEVICE_OWNER -> deviceOwnerActive
    }
    /** True when the user has chosen Suspend but it won't work on this device. */
    val suspendIneffective: Boolean get() = isOneUi && hideMode == HideMode.SUSPEND && selectedBackend != Backend.DEVICE_OWNER
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val chooserRepo = ChooserRepository(app)
    private val prefsRepo   = PreferencesRepository(app)

    private val _selectedCategory = MutableStateFlow(IntentCategory.APK_INSTALLER)
    val selectedCategory: StateFlow<IntentCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery  = MutableStateFlow("")
    private val _capabilities = MutableStateFlow(Capabilities())
    private val _reloadApps   = MutableStateFlow(0)

    private data class Capabilities(
        val shizukuAvailable: Boolean  = false,
        val shizukuPermission: Boolean = false,
        val rootAvailable: Boolean     = false,
        val deviceOwnerActive: Boolean = false,
    )

    /** null = loading (query in flight for the current category). */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val apps: Flow<List<AppEntry>?> =
        combine(_selectedCategory, _reloadApps) { category, _ -> category }
            .flatMapLatest { category ->
                combine(
                    prefsRepo.hiddenPackages(category),
                    prefsRepo.disabledPackages(category),
                    prefsRepo.hiddenComponents(category),
                ) { hidden, disabled, hiddenComps -> Triple(hidden, disabled, hiddenComps) }
                    .map<Triple<Set<String>, Set<String>, Set<String>>, List<AppEntry>?> { (hidden, disabled, hiddenComps) ->
                        chooserRepo.queryApps(category, hidden, disabled, hiddenComps)
                    }
                    .onStart { emit(null) }
            }

    val uiState: StateFlow<UiState> =
        combine(apps, _searchQuery, _capabilities, prefsRepo.hideMode, prefsRepo.backend) { apps, query, caps, hideMode, backend ->
            UiState(
                apps = (apps ?: emptyList()).filter { entry ->
                    query.isBlank() ||
                        entry.label.contains(query, ignoreCase = true) ||
                        entry.packageName.contains(query, ignoreCase = true)
                },
                searchQuery       = query,
                isLoading         = apps == null,
                shizukuAvailable  = caps.shizukuAvailable,
                shizukuPermission = caps.shizukuPermission,
                rootAvailable     = caps.rootAvailable,
                deviceOwnerActive = caps.deviceOwnerActive,
                hideMode          = hideMode,
                selectedBackend   = backend,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    init {
        refreshCapabilities()
    }

    fun refreshCapabilities() {
        viewModelScope.launch(Dispatchers.IO) {
            val dpm = getApplication<Application>().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            _capabilities.value = Capabilities(
                shizukuAvailable  = ShizukuHelper.isAvailable,
                shizukuPermission = ShizukuHelper.hasPermission,
                rootAvailable     = RootHelper.checkAvailable(),
                deviceOwnerActive = dpm.isDeviceOwnerApp(getApplication<Application>().packageName),
            )
        }
    }

    /** Re-query the app list, e.g. after returning to the foreground. */
    fun refreshApps() {
        _reloadApps.value += 1
    }

    fun selectCategory(category: IntentCategory) {
        _selectedCategory.value = category
        _searchQuery.value = ""
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setHideMode(mode: HideMode) {
        viewModelScope.launch { prefsRepo.setHideMode(mode) }
    }

    fun setBackend(backend: Backend) {
        viewModelScope.launch { prefsRepo.setBackend(backend) }
    }

    fun requestShizukuPermission() = ShizukuHelper.requestPermission(1001)

    fun toggleHidden(entry: AppEntry) {
        viewModelScope.launch {
            val state    = uiState.value
            val category = _selectedCategory.value
            val backend  = state.selectedBackend
            val target   = !entry.isHidden

            val result = if (target) {
                if (state.hideMode == HideMode.COMPONENT) {
                    val components = chooserRepo.queryComponentsForPackage(entry.packageName, category)
                    chooserRepo.setComponentHidden(components, true, backend)
                        .onSuccess { prefsRepo.setHiddenComponents(category, components, true) }
                } else {
                    chooserRepo.setPackageHidden(entry.packageName, true, backend)
                }
            } else {
                // Restore the same way the app was hidden, regardless of the current mode.
                val storedComponents = prefsRepo.hiddenComponentsFor(category, entry.packageName)
                if (storedComponents.isNotEmpty()) {
                    chooserRepo.setComponentHidden(storedComponents, false, backend)
                        .onSuccess { prefsRepo.setHiddenComponents(category, storedComponents, false) }
                } else {
                    // Fallback to query dynamically if stored list is empty
                    val components = chooserRepo.queryComponentsForPackage(entry.packageName, category)
                    if (components.isNotEmpty()) {
                        chooserRepo.setComponentHidden(components, false, backend)
                    } else {
                        chooserRepo.setPackageHidden(entry.packageName, false, backend)
                    }
                }
            }

            result.fold(
                onSuccess = { prefsRepo.setHidden(category, entry.packageName, target) },
                onFailure = { _messages.trySend(it.message ?: "Command failed") },
            )
        }
    }

    fun toggleDisabled(entry: AppEntry) {
        viewModelScope.launch {
            val state   = uiState.value
            val backend = state.selectedBackend
            val target  = !entry.isDisabled

            chooserRepo.setPackageDisabled(entry.packageName, target, backend).fold(
                onSuccess = { prefsRepo.setDisabled(_selectedCategory.value, entry.packageName, target) },
                onFailure = { _messages.trySend(it.message ?: "Command failed") },
            )
        }
    }
}
