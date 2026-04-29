package com.thejaustin.sharemove.data.model

import android.graphics.drawable.Drawable

data class AppEntry(
    val packageName: String,
    val componentName: String?,   // non-null only for root component-level control
    val label: String,
    val icon: Drawable?,
    val category: IntentCategory,
    /** User's stored intent: should this app be hidden from the chooser? */
    val isHidden: Boolean = false,
    /** User's stored intent: should this app be fully disabled? */
    val isDisabled: Boolean = false,
)
