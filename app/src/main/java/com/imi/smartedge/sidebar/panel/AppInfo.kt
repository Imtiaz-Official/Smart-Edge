package com.imi.smartedge.sidebar.panel

/**
 * Represents an installed app that can be placed in the side panel.
 * Icons are loaded lazily via Glide using the packageName.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    var isInPanel: Boolean = false
)
