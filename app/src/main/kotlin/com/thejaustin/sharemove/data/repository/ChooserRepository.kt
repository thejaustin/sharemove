package com.thejaustin.sharemove.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.thejaustin.sharemove.data.model.AppEntry
import com.thejaustin.sharemove.data.model.HideMode
import com.thejaustin.sharemove.data.model.IntentCategory
import com.thejaustin.sharemove.shizuku.RootHelper
import com.thejaustin.sharemove.shizuku.ShizukuHelper

class ChooserRepository(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    /** Enumerate all apps that handle [category], annotated with stored preferences. */
    fun queryApps(
        category: IntentCategory,
        hiddenPackages: Set<String>,
        disabledPackages: Set<String>,
    ): List<AppEntry> {
        val intent = Intent(category.action).apply {
            category.mimeType?.let { type = it }
            category.scheme?.let { data = android.net.Uri.parse("$it://example.com") }
        }

        @Suppress("DEPRECATION")
        val resolved: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)

        return resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                val pkg = ri.activityInfo.packageName
                val comp = "${ri.activityInfo.packageName}/${ri.activityInfo.name}"
                AppEntry(
                    packageName   = pkg,
                    componentName = comp,
                    label         = ri.loadLabel(pm).toString(),
                    icon          = ri.loadIcon(pm),
                    category      = category,
                    isHidden      = pkg in hiddenPackages,
                    isDisabled    = pkg in disabledPackages,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    // ── Hide / show (non-root: pm suspend) ──────────────────────────────────

    fun hidePackage(packageName: String): Result<Unit> =
        ShizukuHelper.runCommand("pm suspend --user 0 $packageName").map { }

    fun showPackage(packageName: String): Result<Unit> =
        ShizukuHelper.runCommand("pm unsuspend --user 0 $packageName").map { }

    // ── Hide / show component (root only) ───────────────────────────────────

    fun hideComponent(componentName: String): Result<Unit> =
        RootHelper.runCommand("pm disable-user --user 0 $componentName").map { }

    fun showComponent(componentName: String): Result<Unit> =
        RootHelper.runCommand("pm enable --user 0 $componentName").map { }

    // ── Full disable / enable ───────────────────────────────────────────────

    fun disablePackage(packageName: String): Result<Unit> =
        ShizukuHelper.runCommand("pm disable-user --user 0 $packageName").map { }

    fun enablePackage(packageName: String): Result<Unit> =
        ShizukuHelper.runCommand("pm enable --user 0 $packageName").map { }

    // ── Root component disable (full package) ───────────────────────────────

    fun disablePackageRoot(packageName: String): Result<Unit> =
        RootHelper.runCommand("pm disable-user --user 0 $packageName").map { }

    fun enablePackageRoot(packageName: String): Result<Unit> =
        RootHelper.runCommand("pm enable --user 0 $packageName").map { }

    /** Determine available hide mode based on runtime capabilities. */
    val hideMode: HideMode
        get() = if (RootHelper.isAvailable) HideMode.COMPONENT else HideMode.SUSPEND
}
