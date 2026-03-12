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
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_PANEL_OPACITY = "panel_opacity"
        private const val KEY_HANDLE_HEIGHT = "handle_height"
        private const val KEY_HANDLE_WIDTH = "handle_width"
        private const val KEY_HANDLE_OFFSET = "handle_offset"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_PANEL_COLUMNS = "panel_columns"
        private const val KEY_UI_THEME = "ui_theme"
        private const val KEY_IS_PREMIUM = "is_premium"
        
        private const val KEY_PANEL_RADIUS = "panel_radius"
        private const val KEY_PANEL_BG_COLOR = "panel_bg_color"
        private const val KEY_HIDE_BG = "hide_bg"
        private const val KEY_SHOW_TOOLS = "show_tools"
        private const val KEY_ICON_SHAPE = "icon_shape"
        private const val KEY_GESTURES_ENABLED = "gestures_enabled"
        
        private const val DELIMITER = ","

        const val SIDE_RIGHT = "right"
        const val SIDE_LEFT = "left"
        
        const val THEME_ORIGIN = "origin"
        const val THEME_HYPEROS = "hyperos"
        const val THEME_REALME = "realme"
        const val THEME_RICH = "rich"

        const val SHAPE_CIRCLE = "circle"
        const val SHAPE_SQUIRCLE = "squircle"
        const val SHAPE_SQUARE = "square"
        const val SHAPE_ROUNDED = "rounded"
    }

    /** Whether the edge swipe gesture is enabled. */
    var gesturesEnabled: Boolean
        get() = prefs.getBoolean(KEY_GESTURES_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_GESTURES_ENABLED, value) }

    /** Icon shape: "circle", "squircle", "square", "rounded". */
    var iconShape: String
        get() = prefs.getString(KEY_ICON_SHAPE, SHAPE_CIRCLE) ?: SHAPE_CIRCLE
        set(value) = prefs.edit { putString(KEY_ICON_SHAPE, value) }

    /** Whether to show a visible pill handle at the edge. */
    var showPill: Boolean
        get() = prefs.getBoolean(KEY_SHOW_PILL, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_PILL, value) }

    /** Whether haptic feedback is enabled. */
    var hapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_HAPTIC_ENABLED, value) }

    /** Panel opacity (0 to 100). */
    var panelOpacity: Int
        get() = prefs.getInt(KEY_PANEL_OPACITY, 100)
        set(value) = prefs.edit { putInt(KEY_PANEL_OPACITY, value) }

    /** Edge handle height in DP. */
    var handleHeight: Int
        get() = prefs.getInt(KEY_HANDLE_HEIGHT, 80)
        set(value) = prefs.edit { putInt(KEY_HANDLE_HEIGHT, value) }

    /** Edge handle width in DP. */
    var handleWidth: Int
        get() = prefs.getInt(KEY_HANDLE_WIDTH, 24)
        set(value) = prefs.edit { putInt(KEY_HANDLE_WIDTH, value) }

    /** Edge handle vertical offset (-100 to 100). */
    var handleVerticalOffset: Int
        get() = prefs.getInt(KEY_HANDLE_OFFSET, 0)
        set(value) = prefs.edit { putInt(KEY_HANDLE_OFFSET, value) }

    /** Custom accent color (Hex string). */
    var accentColor: String
        get() = prefs.getString(KEY_ACCENT_COLOR, "#4A9EFF") ?: "#4A9EFF"
        set(value) = prefs.edit { putString(KEY_ACCENT_COLOR, value) }

    /** Number of columns in side panel (1 or 2). */
    var panelColumns: Int
        get() = prefs.getInt(KEY_PANEL_COLUMNS, 1)
        set(value) = prefs.edit { putInt(KEY_PANEL_COLUMNS, value) }

    /** UI Theme style: "origin", "hyperos", "realme", "rich". */
    var uiTheme: String
        get() = prefs.getString(KEY_UI_THEME, THEME_ORIGIN) ?: THEME_ORIGIN
        set(value) = prefs.edit { putString(KEY_UI_THEME, value) }

    /** Panel Corner Radius in DP. */
    var panelCornerRadius: Int
        get() = prefs.getInt(KEY_PANEL_RADIUS, 28)
        set(value) = prefs.edit { putInt(KEY_PANEL_RADIUS, value) }

    /** Custom Background Color (Hex string). Default is semi-transparent dark. */
    var panelBackgroundColor: String
        get() = prefs.getString(KEY_PANEL_BG_COLOR, "#E61A1C1E") ?: "#E61A1C1E"
        set(value) = prefs.edit { putString(KEY_PANEL_BG_COLOR, value) }

    /** Whether to hide the panel background completely. */
    var hideBackground: Boolean
        get() = prefs.getBoolean(KEY_HIDE_BG, false)
        set(value) = prefs.edit { putBoolean(KEY_HIDE_BG, value) }

    /** Whether to show the Tools section (Screenshot, etc). */
    var showTools: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TOOLS, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_TOOLS, value) }

    /** Simulated premium status. */
    var isPremium: Boolean
        get() = prefs.getBoolean(KEY_IS_PREMIUM, false)
        set(value) = prefs.edit { putBoolean(KEY_IS_PREMIUM, value) }

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
