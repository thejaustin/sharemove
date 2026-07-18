package com.thejaustin.sharemove.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.InputStreamReader

object IconPackHelper {

    private var cachedIconPack: String? = null
    private val packageToDrawableMap = mutableMapOf<String, String>()

    @Synchronized
    private fun loadAppFilter(context: Context, iconPackPackage: String) {
        if (cachedIconPack == iconPackPackage) return
        
        cachedIconPack = iconPackPackage
        packageToDrawableMap.clear()
        
        val pm = context.packageManager
        try {
            val iconPackRes = pm.getResourcesForApplication(iconPackPackage)
            val assets = iconPackRes.assets
            
            var inputStream: InputStream? = null
            var xmlParser: XmlPullParser? = null
            
            try {
                inputStream = assets.open("appfilter.xml")
            } catch (_: Exception) {
                // Try to find the resource ID for XML/appfilter
                val resId = iconPackRes.getIdentifier("appfilter", "xml", iconPackPackage)
                if (resId != 0) {
                    xmlParser = iconPackRes.getXml(resId)
                }
            }

            if (inputStream != null || xmlParser != null) {
                val parser = xmlParser ?: Xml.newPullParser().apply {
                    setInput(InputStreamReader(inputStream!!))
                }
                
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (!component.isNullOrEmpty() && !drawable.isNullOrEmpty()) {
                            val pkg = extractPackage(component)
                            if (pkg.isNotEmpty()) {
                                packageToDrawableMap[pkg] = drawable
                            }
                        }
                    }
                    eventType = parser.next()
                }
                
                try {
                    inputStream?.close()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractPackage(component: String): String {
        // Formats: ComponentInfo{com.android.chrome/com.google.android.apps.chrome.Main}
        val start = component.indexOf('{')
        val end = component.indexOf('/')
        if (start != -1 && end != -1 && end > start + 1) {
            return component.substring(start + 1, end).trim()
        }
        if (component.contains("/")) {
            return component.substringBefore("/").replace("ComponentInfo{", "").trim()
        }
        return ""
    }

    /**
     * Attempts to resolve and load an app icon from the user's custom icon pack.
     * Falls back to the default icon if the pack is not found or does not theme this package.
     */
    fun loadIcon(context: Context, iconPackPackage: String?, targetPackage: String, defaultIcon: Drawable?): Drawable? {
        if (iconPackPackage.isNullOrEmpty()) return defaultIcon
        
        loadAppFilter(context, iconPackPackage)
        
        val pm = context.packageManager
        try {
            val iconPackRes = pm.getResourcesForApplication(iconPackPackage)
            
            // Try 1: Lookup via appfilter.xml mapping
            val mappedDrawableName = packageToDrawableMap[targetPackage]
            if (!mappedDrawableName.isNullOrEmpty()) {
                val resId = iconPackRes.getIdentifier(mappedDrawableName, "drawable", iconPackPackage)
                if (resId != 0) {
                    @Suppress("DEPRECATION")
                    return iconPackRes.getDrawable(resId, null)
                }
            }
            
            // Try 2: package name with dots replaced by underscores (e.g. com_android_chrome)
            val resName1 = targetPackage.replace('.', '_').lowercase()
            var resId = iconPackRes.getIdentifier(resName1, "drawable", iconPackPackage)
            
            // Try 3: pure package name (e.g. com.android.chrome)
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
