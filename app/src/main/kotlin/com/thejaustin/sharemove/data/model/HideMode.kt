package com.thejaustin.sharemove.data.model

/**
 * How an app is hidden from the chooser sheet.
 *
 * SUSPEND   — pm suspend: whole package paused, app data intact.
 *             Side effect: app icon is greyed out in launcher.
 * COMPONENT — pm disable-user on the specific activity: surgical, but can affect
 *             the launcher entry when the app routes both through one activity.
 *
 * Both work through Shizuku (shell uid) or root.
 */
enum class HideMode { SUSPEND, COMPONENT }
