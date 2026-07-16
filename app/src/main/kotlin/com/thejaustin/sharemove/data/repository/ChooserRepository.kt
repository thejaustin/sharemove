package com.thejaustin.sharemove.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.thejaustin.sharemove.data.model.AppEntry
import com.thejaustin.sharemove.data.model.IntentCategory
import com.thejaustin.sharemove.shizuku.RootHelper
import com.thejaustin.sharemove.shizuku.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Which privileged backend executes pm commands. */
enum class Backend { SHIZUKU, ROOT }

class ChooserRepository(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    /** Enumerate all apps that handle [category], annotated with stored preferences. */
    suspend fun queryApps(
        category: IntentCategory,
        hiddenPackages: Set<String>,
        disabledPackages: Set<String>,
    ): List<AppEntry> = withContext(Dispatchers.IO) {
        val intent = Intent(category.action).apply {
            category.mimeType?.let { type = it }
            category.scheme?.let { data = Uri.parse("$it://example.com") }
        }

        // MATCH_DISABLED_COMPONENTS keeps pm-disabled apps in the list so they
        // can be re-enabled from the UI after being hidden or disabled.
        @Suppress("DEPRECATION")
        val resolved = pm.queryIntentActivities(
            intent,
            PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_COMPONENTS,
        )

        val entries = resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { ri ->
                val pkg = ri.activityInfo.packageName
                AppEntry(
                    packageName   = pkg,
                    componentName = "$pkg/${ri.activityInfo.name}",
                    label         = ri.loadLabel(pm).toString(),
                    icon          = ri.loadIcon(pm),
                    category      = category,
                    isHidden      = pkg in hiddenPackages,
                    isDisabled    = pkg in disabledPackages,
                )
            }

        // Apps with stored state that dropped out of resolution entirely (some OEMs
        // exclude suspended apps) must stay listed so the user can restore them.
        val resolvedPackages = entries.mapTo(mutableSetOf()) { it.packageName }
        val ghosts = (hiddenPackages + disabledPackages)
            .filterNot { it in resolvedPackages }
            .mapNotNull { pkg ->
                installedEntryOrNull(pkg, category, hiddenPackages, disabledPackages)
            }

        (entries + ghosts).sortedBy { it.label.lowercase() }
    }

    private fun installedEntryOrNull(
        packageName: String,
        category: IntentCategory,
        hiddenPackages: Set<String>,
        disabledPackages: Set<String>,
    ): AppEntry? = try {
        @Suppress("DEPRECATION")
        val info = pm.getApplicationInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        AppEntry(
            packageName   = packageName,
            componentName = null,
            label         = info.loadLabel(pm).toString(),
            icon          = info.loadIcon(pm),
            category      = category,
            isHidden      = packageName in hiddenPackages,
            isDisabled    = packageName in disabledPackages,
        )
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    // ── Privileged pm commands ───────────────────────────────────────────────

    suspend fun setPackageHidden(packageName: String, hidden: Boolean, backend: Backend): Result<Unit> =
        run(
            if (hidden) "pm suspend --user 0 $packageName"
            else        "pm unsuspend --user 0 $packageName",
            backend,
        )

    suspend fun setComponentHidden(componentName: String, hidden: Boolean, backend: Backend): Result<Unit> =
        run(
            // Single quotes: component names may contain $ (inner-class activities)
            if (hidden) "pm disable-user --user 0 '$componentName'"
            else        "pm enable --user 0 '$componentName'",
            backend,
        )

    suspend fun setPackageDisabled(packageName: String, disabled: Boolean, backend: Backend): Result<Unit> =
        run(
            if (disabled) "pm disable-user --user 0 $packageName"
            else          "pm enable --user 0 $packageName",
            backend,
        )

    private suspend fun run(command: String, backend: Backend): Result<Unit> = when (backend) {
        Backend.ROOT    -> RootHelper.runCommand(command)
        Backend.SHIZUKU -> ShizukuHelper.runCommand(command)
    }.map { }
}
