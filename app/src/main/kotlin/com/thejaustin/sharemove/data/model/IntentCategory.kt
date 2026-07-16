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
    SHARE_TEXT(
        displayName = "Share: Text",
        action      = "android.intent.action.SEND",
        mimeType    = "text/plain",
    ),
    SHARE_IMAGE(
        displayName = "Share: Images",
        action      = "android.intent.action.SEND",
        mimeType    = "image/*",
    ),
    SHARE_FILE(
        displayName = "Share: Files",
        action      = "android.intent.action.SEND",
        mimeType    = "*/*",
    ),
}
