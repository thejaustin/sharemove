package com.thejaustin.sharemove.data.repository

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.thejaustin.sharemove.data.model.AppEntry
import com.thejaustin.sharemove.data.model.IntentCategory
import com.thejaustin.sharemove.receiver.AdminReceiver
import com.thejaustin.sharemove.shizuku.RootHelper
import com.thejaustin.sharemove.shizuku.ShizukuHelper
import com.thejaustin.sharemove.shizuku.ShizukuPlusHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Which privileged backend executes app-hiding commands. */
enum class Backend {
    SHIZUKU,        // OG Shizuku (spawn shell process)
    SHIZUKU_PLUS,   // Shizuku+ (direct IPC Binder wrapper)
    ROOT,           // Root access (su shell process)
    DEVICE_OWNER,   // Device Policy Manager (Enterprise API)
}

private val DISABLED_STATES = setOf(
    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
    PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
    PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
)

class ChooserRepository(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager
    private val dpm by lazy { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    private val adminComponent by lazy { ComponentName(context, AdminReceiver::class.java) }

    /**
     * Enumerate all apps that handle [category].
     *
     * Hidden/disabled status is read back from the system (suspended flag, enabled
     * settings) rather than trusted from stored prefs, so the UI stays correct even
     * when state was changed externally (adb, another manager app, a reboot quirk).
     */
    suspend fun queryApps(
        category: IntentCategory,
        hiddenPackages: Set<String>,
        disabledPackages: Set<String>,
        hiddenComponents: Set<String>,
    ): List<AppEntry> = withContext(Dispatchers.IO) {
        val intent = Intent(category.action).apply {
            category.mimeType?.let { type = it }
            category.scheme?.let { data = Uri.parse("$it://example.com") }
        }

        // MATCH_DISABLED_COMPONENTS keeps pm-disabled apps in the list so they
        // can be re-enabled from the UI after being hidden or disabled.
        val flags = PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_COMPONENTS
        @Suppress("DEPRECATION")
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
        } else {
            pm.queryIntentActivities(intent, flags)
        }

        val entries = resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { ri ->
                val pkg  = ri.activityInfo.packageName
                val comp = "$pkg/${ri.activityInfo.name}"
                // The component the user actually hid may differ from the one the
                // resolver happened to return first — prefer the recorded one.
                val storedComp = hiddenComponents.firstOrNull { it.substringBefore('/') == pkg }
                AppEntry(
                    packageName   = pkg,
                    componentName = comp,
                    label         = ri.loadLabel(pm).toString(),
                    icon          = ri.loadIcon(pm),
                    category      = category,
                    isHidden      = isPackageSuspended(pkg) || isComponentDisabled(storedComp ?: comp) || isAppHiddenByDeviceOwner(pkg),
                    isDisabled    = isPackageDisabled(pkg),
                )
            }

        // Apps with stored state that dropped out of resolution entirely (some OEMs
        // exclude suspended apps) must stay listed so the user can restore them.
        val resolvedPackages = entries.mapTo(mutableSetOf()) { it.packageName }
        val ghosts = (hiddenPackages + disabledPackages)
            .filterNot { it in resolvedPackages }
            .mapNotNull { pkg ->
                installedEntryOrNull(pkg, category, hiddenPackages, hiddenComponents)
            }

        (entries + ghosts).sortedBy { it.label.lowercase() }
    }

    private fun installedEntryOrNull(
        packageName: String,
        category: IntentCategory,
        hiddenPackages: Set<String>,
        hiddenComponents: Set<String>,
    ): AppEntry? = try {
        @Suppress("DEPRECATION")
        val info = pm.getApplicationInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        val storedComp = hiddenComponents.firstOrNull { it.substringBefore('/') == packageName }
        AppEntry(
            packageName   = packageName,
            componentName = null,
            label         = info.loadLabel(pm).toString(),
            icon          = info.loadIcon(pm),
            category      = category,
            isHidden      = isPackageSuspended(packageName) ||
                (storedComp != null && isComponentDisabled(storedComp)) ||
                packageName in hiddenPackages ||
                isAppHiddenByDeviceOwner(packageName),
            isDisabled    = isPackageDisabled(packageName),
        )
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    // ── System-truth state checks ────────────────────────────────────────────

    private fun isPackageSuspended(packageName: String): Boolean = try {
        // PackageManager.isPackageSuspended() only checks the calling package (self).
        // To check another package we must read the FLAG_SUSPENDED bit from ApplicationInfo,
        // which is readable without any special permission.
        @Suppress("DEPRECATION")
        (pm.getApplicationInfo(packageName, 0).flags and ApplicationInfo.FLAG_SUSPENDED) != 0
    } catch (_: Exception) {
        false
    }

    private fun isPackageDisabled(packageName: String): Boolean = try {
        pm.getApplicationEnabledSetting(packageName) in DISABLED_STATES
    } catch (_: Exception) {
        false
    }

    private fun isComponentDisabled(componentName: String): Boolean = try {
        val cn = ComponentName.unflattenFromString(componentName)
        cn != null && pm.getComponentEnabledSetting(cn) in DISABLED_STATES
    } catch (_: Exception) {
        false
    }

    private fun isAppHiddenByDeviceOwner(packageName: String): Boolean = try {
        dpm.isDeviceOwnerApp(context.packageName) && dpm.isApplicationHidden(adminComponent, packageName)
    } catch (_: Exception) {
        false
    }

    // ── Privileged pm commands ───────────────────────────────────────────────

    suspend fun setPackageHidden(packageName: String, hidden: Boolean, backend: Backend): Result<Unit> = when (backend) {
        Backend.DEVICE_OWNER -> runCatching {
            dpm.setApplicationHidden(adminComponent, packageName, hidden)
        }
        Backend.SHIZUKU_PLUS -> ShizukuPlusHelper.setPackageSuspended(packageName, hidden, context.packageName)
        else -> run(
            if (hidden) "pm suspend --user 0 $packageName"
            else        "pm unsuspend --user 0 $packageName",
            backend
        )
    }

    suspend fun setComponentHidden(componentName: String, hidden: Boolean, backend: Backend): Result<Unit> = when (backend) {
        Backend.DEVICE_OWNER -> Result.failure(Exception("Component-level hiding is not supported in Device Owner mode"))
        Backend.SHIZUKU_PLUS -> {
            val cn = ComponentName.unflattenFromString(componentName)
                ?: return Result.failure(Exception("Invalid component name: $componentName"))
            val state = if (hidden) PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            ShizukuPlusHelper.setComponentEnabledSetting(cn, state, PackageManager.DONT_KILL_APP)
        }
        else -> run(
            // Single quotes: component names may contain $ (inner-class activities)
            if (hidden) "pm disable-user --user 0 '$componentName'"
            else        "pm enable --user 0 '$componentName'",
            backend
        )
    }

    suspend fun setPackageDisabled(packageName: String, disabled: Boolean, backend: Backend): Result<Unit> = when (backend) {
        Backend.DEVICE_OWNER -> Result.failure(Exception("Full app disabling is not supported in Device Owner mode"))
        Backend.SHIZUKU_PLUS -> {
            val state = if (disabled) PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            ShizukuPlusHelper.setApplicationEnabledSetting(packageName, state, PackageManager.DONT_KILL_APP, callingPackage = context.packageName)
        }
        else -> run(
            if (disabled) "pm disable-user --user 0 $packageName"
            else          "pm enable --user 0 $packageName",
            backend
        )
    }

    private suspend fun run(command: String, backend: Backend): Result<Unit> = when (backend) {
        Backend.ROOT    -> RootHelper.runCommand(command)
        Backend.SHIZUKU -> ShizukuHelper.runCommand(command)
        else            -> Result.failure(Exception("Backend $backend cannot execute shell commands"))
    }.map { }
}
