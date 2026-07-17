package com.thejaustin.sharemove.util

import android.os.Build

/**
 * Samsung One UI detection helpers.
 *
 * On Samsung One UI, `pm suspend` does NOT remove an app from the share sheet —
 * Samsung's custom ChooserActivity enumerates candidates independently of the
 * standard suspend flag. Users on One UI should use COMPONENT or DISABLE mode instead.
 */
object SamsungUtil {

    /**
     * Returns true if the device is running Samsung One UI.
     * Checked by the presence of "samsung" in the manufacturer field and the
     * One UI version system property.
     */
    val isOneUi: Boolean by lazy {
        Build.MANUFACTURER.equals("samsung", ignoreCase = true) &&
            runCatching {
                val prop = Class.forName("android.os.SystemProperties")
                    .getMethod("get", String::class.java, String::class.java)
                    .invoke(null, "ro.build.version.oneui", "") as String
                prop.isNotBlank()
            }.getOrDefault(false)
    }
}
