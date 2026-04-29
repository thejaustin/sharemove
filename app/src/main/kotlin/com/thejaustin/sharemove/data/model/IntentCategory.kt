package com.thejaustin.sharemove.data.model

enum class IntentCategory(
    val displayName: String,
    val action: String,
    val mimeType: String? = null,
    val scheme: String? = null,
) {
    APK_INSTALLER(
        displayName = "App Installer",
        action      = "android.intent.action.VIEW",
        mimeType    = "application/vnd.android.package-archive",
    ),
    BROWSER(
        displayName = "Browser",
        action      = "android.intent.action.VIEW",
        scheme      = "https",
    ),
}
