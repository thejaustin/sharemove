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

        val paramTypes = method.parameterTypes
        val args = arrayOfNulls<Any>(paramTypes.size)
        for (i in paramTypes.indices) {
            val type = paramTypes[i]
            args[i] = when {
                type == Array<String>::class.java || type.componentType == String::class.java -> arrayOf(packageName)
                type == Boolean::class.javaPrimitiveType || type == Boolean::class.java -> suspended
                type == Int::class.javaPrimitiveType || type == Int::class.java -> {
                    // Match userId parameter location: usually last, or second-to-last if callingPackage string is last
                    if (i == paramTypes.lastIndex || (i == paramTypes.lastIndex - 1 && paramTypes.last() == String::class.java)) userId else 0
                }
                type == String::class.java -> callingPackage
                else -> null // PersistableBundle, SuspendDialogInfo, etc.
            }
        }

        try {
            val result = method.invoke(ipm, *args) as? Array<*>
            if (result != null && result.contains(packageName)) {
                throw Exception("Package manager refused to suspend/unsuspend package $packageName")
            }
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.cause ?: e
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
        try {
            method.invoke(ipm, componentName, newState, flags, userId)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.cause ?: e
        }
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
        try {
            method.invoke(ipm, packageName, newState, flags, userId, callingPackage)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.cause ?: e
        }
    }
}
