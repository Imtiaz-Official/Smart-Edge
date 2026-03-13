package com.originpanel.sidepanel

import android.graphics.drawable.Drawable

/**
 * Represents an installed app that can be placed in the side panel.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    var icon: Drawable?,
    var isInPanel: Boolean = false
)
