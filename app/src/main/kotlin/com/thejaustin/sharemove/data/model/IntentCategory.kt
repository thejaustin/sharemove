package com.thejaustin.sharemove.data.model

import android.content.Context
import android.content.Intent
import android.net.Uri

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
    );

    /**
     * Create a system chooser test intent matching this category.
     */
    fun getTestIntent(context: Context): Intent {
        val baseIntent = Intent(action).apply {
            mimeType?.let { type = it }
            scheme?.let { data = Uri.parse("$it://example.com") }
        }
        when (this) {
            APK_INSTALLER -> {
                baseIntent.data = Uri.parse("content://com.thejaustin.sharemove.fileprovider/test.apk")
            }
            EMAIL -> {
                baseIntent.data = Uri.parse("mailto:test@example.com")
            }
            SHARE_TEXT -> {
                baseIntent.putExtra(Intent.EXTRA_TEXT, "Sample test share text from ShaRemove.")
            }
            SHARE_IMAGE -> {
                baseIntent.putExtra(Intent.EXTRA_TEXT, "Sample image share")
                baseIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://media/external/images/media/1"))
            }
            SHARE_FILE -> {
                baseIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://media/external/downloads/1"))
            }
            PHONE_DIALER -> {
                baseIntent.data = Uri.parse("tel:5550199")
            }
            CALENDAR -> {
                baseIntent.putExtra("title", "Test event from ShaRemove")
            }
            else -> {}
        }
        return Intent.createChooser(baseIntent, "Test Intent Chooser: $displayName")
    }
}
