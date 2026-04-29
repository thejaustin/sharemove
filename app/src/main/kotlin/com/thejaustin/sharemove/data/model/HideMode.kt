package com.thejaustin.sharemove.data.model

/**
 * How an app is hidden from the chooser sheet.
 *
 * SUSPEND  — pm suspend (non-root): whole package paused, app data intact.
 *            Side effect: app icon is greyed out in launcher.
 * COMPONENT — pm disable-user on the specific activity (root only): surgical,
 *            no effect on launcher icon or the rest of the app.
 */
enum class HideMode { SUSPEND, COMPONENT }
