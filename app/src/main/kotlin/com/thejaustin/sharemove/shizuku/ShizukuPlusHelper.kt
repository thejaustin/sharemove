package com.thejaustin.sharemove.shizuku

import android.content.ComponentName
import android.os.IBinder
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Shizuku+ (Direct IPC Binder Wrapping) Backend.
 *
 * Rather than spawning command-line subshells (e.g. `pm suspend`), this backend
 * wraps system Binder tokens in a [ShizukuBinderWrapper] and invokes transactions
 * directly on [IPackageManager] using reflection.
 *
 * This provides near-instant execution (<5ms) and proper stack trace propagation.
 */
object ShizukuPlusHelper {

    private var ipmCache: Any? = null

    private fun getIPackageManager(): Any {
        ipmCache?.let { return it }
        val rawBinder = SystemServiceHelper.getSystemService("package")
            ?: throw IllegalStateException("Failed to get package service binder")
        val wrappedBinder = ShizukuBinderWrapper(rawBinder)
        val stubClass = Class.forName("android.content.pm.IPackageManager\$Stub")
        val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
        val ipm = asInterfaceMethod.invoke(null, wrappedBinder)
            ?: throw IllegalStateException("Failed to instantiate IPackageManager stub interface")
        ipmCache = ipm
        return ipm
    }

    /** Suspend or unsuspend a package. Returns Result.success or Result.failure. */
    fun setPackageSuspended(packageName: String, suspended: Boolean, callingPackage: String, userId: Int = 0): Result<Unit> = runCatching {
        val ipm = getIPackageManager()
        val method = ipm.javaClass.methods.firstOrNull { it.name == "setPackagesSuspendedAsUser" }
            ?: throw NoSuchMethodException("setPackagesSuspendedAsUser not found on IPackageManager")

        val packages = arrayOf(packageName)
        val result = method.invoke(
            ipm,
            packages,
            suspended,
            null, // appExtras (PersistableBundle)
            null, // launcherExtras (PersistableBundle)
            null, // dialogInfo (SuspendDialogInfo)
            callingPackage,
            userId
        ) as? Array<*>

        if (result != null && result.contains(packageName)) {
            throw Exception("Package manager refused to suspend/unsuspend package $packageName")
        }
    }

    /** Enable or disable a specific activity component within a package. */
    fun setComponentEnabledSetting(componentName: ComponentName, newState: Int, flags: Int, userId: Int = 0): Result<Unit> = runCatching {
        val ipm = getIPackageManager()
        val method = ipm.javaClass.getMethod(
            "setComponentEnabledSetting",
            ComponentName::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java
        )
        method.invoke(ipm, componentName, newState, flags, userId)
    }

    /** Enable or disable an entire package. */
    fun setApplicationEnabledSetting(packageName: String, newState: Int, flags: Int, userId: Int = 0, callingPackage: String): Result<Unit> = runCatching {
        val ipm = getIPackageManager()
        val method = ipm.javaClass.getMethod(
            "setApplicationEnabledSetting",
            String::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            String::class.java
        )
        method.invoke(ipm, packageName, newState, flags, userId, callingPackage)
    }
}
