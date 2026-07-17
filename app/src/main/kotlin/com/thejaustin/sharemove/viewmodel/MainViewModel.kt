package com.thejaustin.sharemove.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Events emitted from the ViewModel to the UI layer. */
sealed class UiEvent {
    /** A simple informational message (errors, etc.). */
    data class Message(val text: String) : UiEvent()
    /** Fired after a successful hide/show toggle — carries an undo action. */
    data class ToggleResult(
        val label: String,
        val hidden: Boolean,
        val onUndo: () -> Unit,
    ) : UiEvent()
}

/** Information about a default system application handler. */
data class DefaultAppInfo(
    val packageName: String,
    val label: String,
)

/** State representing the current default app selections. */
data class DefaultAppsState(
    val browser: DefaultAppInfo? = null,
    val launcher: DefaultAppInfo? = null,
    val dialer: DefaultAppInfo? = null,
)

/** Information about an installed custom icon pack. */
data class IconPackInfo(
    val packageName: String,
    val label: String,
)

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

    private val _defaultApps = MutableStateFlow(DefaultAppsState())
    val defaultApps: StateFlow<DefaultAppsState> = _defaultApps.asStateFlow()

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
        combine(_selectedCategory, _reloadApps, prefsRepo.selectedIconPack) { category, _, iconPack -> Triple(category, 0, iconPack) }
            .flatMapLatest { (category, _, iconPack) ->
                combine(
                    prefsRepo.hiddenPackages(category),
                    prefsRepo.disabledPackages(category),
                    prefsRepo.hiddenComponents(category),
                ) { hidden, disabled, hiddenComps -> Triple(hidden, disabled, hiddenComps) }
                    .map<Triple<Set<String>, Set<String>, Set<String>>, List<AppEntry>?> { (hidden, disabled, hiddenComps) ->
                        chooserRepo.queryApps(category, hidden, disabled, hiddenComps, iconPack)
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

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        refreshCapabilities()
        refreshDefaultApps()
        refreshIconPacks()
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
        refreshDefaultApps()
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

    fun refreshDefaultApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager

            // 1. Browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            val browserResolve = pm.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val browserInfo = browserResolve?.activityInfo?.packageName?.let { pkg ->
                val label = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (_: Exception) {
                    pkg
                }
                DefaultAppInfo(pkg, label)
            }

            // 2. Launcher
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val homeResolve = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val homeInfo = homeResolve?.activityInfo?.packageName?.let { pkg ->
                val label = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (_: Exception) {
                    pkg
                }
                DefaultAppInfo(pkg, label)
            }

            // 3. Dialer
            val dialIntent = Intent(Intent.ACTION_DIAL)
            val dialResolve = pm.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val dialInfo = dialResolve?.activityInfo?.packageName?.let { pkg ->
                val label = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (_: Exception) {
                    pkg
                }
                DefaultAppInfo(pkg, label)
            }

            _defaultApps.value = DefaultAppsState(browserInfo, homeInfo, dialInfo)
        }
    }

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
                onSuccess = {
                    prefsRepo.setHidden(category, entry.packageName, target)
                    _events.trySend(
                        UiEvent.ToggleResult(
                            label  = entry.label,
                            hidden = target,
                            onUndo = { toggleHidden(entry.copy(isHidden = target)) },
                        )
                    )
                },
                onFailure = { _events.trySend(UiEvent.Message(it.message ?: "Command failed")) },
            )
        }
    }

    fun toggleDisabled(entry: AppEntry) {
        viewModelScope.launch {
            val state   = uiState.value
            val backend = state.selectedBackend
            val target  = !entry.isDisabled

            chooserRepo.setPackageDisabled(entry.packageName, target, backend).fold(
                onSuccess = {
                    prefsRepo.setDisabled(_selectedCategory.value, entry.packageName, target)
                    _events.trySend(
                        UiEvent.ToggleResult(
                            label  = entry.label,
                            hidden = target,
                            onUndo = { toggleDisabled(entry.copy(isDisabled = target)) },
                        )
                    )
                },
                onFailure = { _events.trySend(UiEvent.Message(it.message ?: "Command failed")) },
            )
        }
    }

    fun bulkToggleHidden(hidden: Boolean) {
        viewModelScope.launch {
            val state = uiState.value
            val category = _selectedCategory.value
            val backend = state.selectedBackend
            val targetApps = state.apps.filter { it.isHidden != hidden && !it.isDisabled }
            if (targetApps.isEmpty()) return@launch

            var successCount = 0
            var failureMessage: String? = null

            for (entry in targetApps) {
                val result = if (hidden) {
                    if (state.hideMode == HideMode.COMPONENT) {
                        val components = chooserRepo.queryComponentsForPackage(entry.packageName, category)
                        chooserRepo.setComponentHidden(components, true, backend)
                            .onSuccess { prefsRepo.setHiddenComponents(category, components, true) }
                    } else {
                        chooserRepo.setPackageHidden(entry.packageName, true, backend)
                    }
                } else {
                    val storedComponents = prefsRepo.hiddenComponentsFor(category, entry.packageName)
                    if (storedComponents.isNotEmpty()) {
                        chooserRepo.setComponentHidden(storedComponents, false, backend)
                            .onSuccess { prefsRepo.setHiddenComponents(category, storedComponents, false) }
                    } else {
                        val components = chooserRepo.queryComponentsForPackage(entry.packageName, category)
                        if (components.isNotEmpty()) {
                            chooserRepo.setComponentHidden(components, false, backend)
                        } else {
                            chooserRepo.setPackageHidden(entry.packageName, false, backend)
                        }
                    }
                }

                result.fold(
                    onSuccess = {
                        prefsRepo.setHidden(category, entry.packageName, hidden)
                        successCount++
                    },
                    onFailure = {
                        failureMessage = it.message
                    }
                )
            }

            if (successCount > 0) {
                _events.trySend(
                    UiEvent.ToggleResult(
                        label = if (successCount == 1) targetApps.first().label else "$successCount apps",
                        hidden = hidden,
                        onUndo = {
                            bulkToggleHidden(!hidden)
                        }
                    )
                )
            } else if (failureMessage != null) {
                _events.trySend(UiEvent.Message(failureMessage ?: "Bulk operation failed"))
            }
        }
    }

    fun showErrorMessage(message: String) {
        _events.trySend(UiEvent.Message(message))
    }

    val selectedIconPack: StateFlow<String> = prefsRepo.selectedIconPack
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setSelectedIconPack(packageName: String) {
        viewModelScope.launch {
            prefsRepo.setSelectedIconPack(packageName)
            refreshApps()
        }
    }

    private val _installedIconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val installedIconPacks: StateFlow<List<IconPackInfo>> = _installedIconPacks.asStateFlow()

    fun refreshIconPacks() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val intent = Intent("org.adw.launcher.THEMES")
            @Suppress("DEPRECATION")
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val list = resolveInfos.map {
                IconPackInfo(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString()
                )
            }.sortedBy { it.label.lowercase() }
            _installedIconPacks.value = list
        }
    }

    fun resetAllChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = uiState.value
            val backend = state.selectedBackend

            try {
                // 1. Restore all components hidden in COMPONENT mode across all categories
                for (category in IntentCategory.entries) {
                    val hiddenComps = prefsRepo.hiddenComponents(category).first()
                    if (hiddenComps.isNotEmpty()) {
                        chooserRepo.setComponentHidden(hiddenComps, false, backend)
                        prefsRepo.setHiddenComponents(category, emptySet(), false)
                    }
                }

                // 2. Restore all packages suspended or disabled across all categories
                for (category in IntentCategory.entries) {
                    val hiddenPkgs = prefsRepo.hiddenPackages(category).first()
                    for (pkg in hiddenPkgs) {
                        chooserRepo.setPackageHidden(pkg, false, backend)
                        prefsRepo.setHidden(category, pkg, false)
                    }

                    val disabledPkgs = prefsRepo.disabledPackages(category).first()
                    for (pkg in disabledPkgs) {
                        chooserRepo.setPackageDisabled(pkg, false, backend)
                        prefsRepo.setDisabled(category, pkg, false)
                    }
                }

                _events.trySend(UiEvent.Message("All apps and components restored to default"))
                refreshApps()
            } catch (e: Exception) {
                _events.trySend(UiEvent.Message("Reset failed: ${e.message}"))
            }
        }
    }
}
