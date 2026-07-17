package com.thejaustin.sharemove.util

import android.content.Context
import android.graphics.drawable.Drawable

object IconPackHelper {

    /**
     * Attempts to resolve and load an app icon from the user's custom icon pack.
     * Falls back to the default icon if the pack is not found or does not theme this package.
     */
    fun loadIcon(context: Context, iconPackPackage: String?, targetPackage: String, defaultIcon: Drawable?): Drawable? {
        if (iconPackPackage.isNullOrEmpty()) return defaultIcon
        val pm = context.packageManager
        try {
            val iconPackRes = pm.getResourcesForApplication(iconPackPackage)
            
            // Common convention 1: package name with dots replaced by underscores (e.g. com_android_chrome)
            val resName1 = targetPackage.replace('.', '_').lowercase()
            var resId = iconPackRes.getIdentifier(resName1, "drawable", iconPackPackage)
            
            // Common convention 2: pure package name (e.g. com.android.chrome)
            if (resId == 0) {
                val resName2 = targetPackage.lowercase()
                resId = iconPackRes.getIdentifier(resName2, "drawable", iconPackPackage)
            }
            
            if (resId != 0) {
                @Suppress("DEPRECATION")
                return iconPackRes.getDrawable(resId, null)
            }
        } catch (_: Exception) {}
        
        return defaultIcon
    }
}
