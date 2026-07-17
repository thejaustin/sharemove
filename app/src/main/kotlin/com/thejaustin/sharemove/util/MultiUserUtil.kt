package com.thejaustin.sharemove.util

import android.content.Context
import android.os.UserHandle
import android.os.UserManager

/**
 * Utility to query all active user profiles on the device.
 *
 * This allows the app to suspend or hide apps not just in the primary user space (User 0),
 * but across all profiles including Samsung Secure Folder, Work Profiles, and parallel apps.
 */
object MultiUserUtil {

    /**
     * Retrieve all active profile IDs associated with the current user.
     */
    fun getUserIds(context: Context): List<Int> {
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            ?: return listOf(0)

        return try {
            userManager.userProfiles.mapNotNull { handle ->
                // UserHandle.getIdentifier() is public since API 33 (Android 13).
                // We use reflection as a fallback to support API 31-32.
                try {
                    val method = handle.javaClass.getMethod("getIdentifier")
                    method.invoke(handle) as? Int
                } catch (_: Exception) {
                    try {
                        val field = handle.javaClass.getDeclaredField("mHandle")
                        field.isAccessible = true
                        field.get(handle) as? Int
                    } catch (_: Exception) {
                        null
                    }
                }
            }.distinct()
        } catch (_: Exception) {
            listOf(0)
        }
    }
}
