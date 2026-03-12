package com.originpanel.sidepanel

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Manages the persistent list of apps pinned to the side panel,
 * and other panel settings, using SharedPreferences.
 */
class PanelPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "side_panel_prefs"
        private const val KEY_PANEL_APPS = "panel_apps"
        private const val KEY_PANEL_SIDE = "panel_side"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_SHOW_PILL = "show_pill"
        private const val DELIMITER = ","

        const val SIDE_RIGHT = "right"
        const val SIDE_LEFT = "left"
    }

    /** Whether to show a visible pill handle at the edge. */
    var showPill: Boolean
        get() = prefs.getBoolean(KEY_SHOW_PILL, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_PILL, value) }

    /** Returns the ordered list of package names pinned to the panel. */
    fun getPanelApps(): List<String> {
        val raw = prefs.getString(KEY_PANEL_APPS, "") ?: ""
        android.util.Log.d("PanelPreferences", "getPanelApps raw: $raw")
        return if (raw.isBlank()) emptyList()
        else raw.split(DELIMITER).filter { it.isNotBlank() }
    }

    /** Saves a new ordered list of pinned package names. */
    fun setPanelApps(packages: List<String>) {
        prefs.edit { putString(KEY_PANEL_APPS, packages.joinToString(DELIMITER)) }
    }

    /** Adds an app to the panel (appended to end). No-op if already present. */
    fun addApp(packageName: String) {
        val current = getPanelApps().toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            setPanelApps(current)
        }
    }

    /** Removes an app from the panel. No-op if not present. */
    fun removeApp(packageName: String) {
        val current = getPanelApps().toMutableList()
        current.remove(packageName)
        setPanelApps(current)
    }

    /** Returns true if the given package is pinned to the panel. */
    fun isInPanel(packageName: String): Boolean = getPanelApps().contains(packageName)

    /** Panel side: "right" (default) or "left". */
    var panelSide: String
        get() = prefs.getString(KEY_PANEL_SIDE, SIDE_RIGHT) ?: SIDE_RIGHT
        set(value) = prefs.edit { putString(KEY_PANEL_SIDE, value) }

    /** Whether the panel service should start automatically on boot. */
    var autoStart: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_START, value) }
}
