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
    PDF_VIEWER(
        displayName = "PDF Viewer",
        action      = "android.intent.action.VIEW",
        mimeType    = "application/pdf",
    ),
    EMAIL(
        displayName = "Email",
        action      = "android.intent.action.SENDTO",
        scheme      = "mailto",
    ),
    MAPS(
        displayName = "Maps",
        action      = "android.intent.action.VIEW",
        scheme      = "geo",
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
    AUDIO_PLAYER(
        displayName = "Audio",
        action      = "android.intent.action.VIEW",
        mimeType    = "audio/*",
    ),
    VIDEO_PLAYER(
        displayName = "Video",
        action      = "android.intent.action.VIEW",
        mimeType    = "video/*",
    ),
    PHONE_DIALER(
        displayName = "Phone / Dialer",
        action      = "android.intent.action.DIAL",
    ),
    CALENDAR(
        displayName = "Calendar",
        action      = "android.intent.action.INSERT",
        scheme      = "content",
    ),
}
