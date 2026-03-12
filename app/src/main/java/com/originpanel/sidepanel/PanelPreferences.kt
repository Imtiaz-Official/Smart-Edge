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
        
        private const val KEY_PILL_WIDTH = "pill_width"
        private const val KEY_PILL_COLOR = "pill_color"
        private const val KEY_TAP_TO_OPEN = "tap_to_open"
        
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

        // Perfect Industry-Standard Defaults
        val DEFAULT_SIDE = SIDE_RIGHT
        const val DEFAULT_AUTO_START = true
        const val DEFAULT_SHOW_PILL = true
        const val DEFAULT_HAPTIC = true
        const val DEFAULT_OPACITY = 100
        const val DEFAULT_HANDLE_HEIGHT = 80
        const val DEFAULT_HANDLE_WIDTH = 24
        const val DEFAULT_HANDLE_OFFSET = 0
        const val DEFAULT_ACCENT_COLOR = "#4A9EFF"
        const val DEFAULT_PANEL_COLS = 1
        const val DEFAULT_THEME = THEME_ORIGIN
        const val DEFAULT_PANEL_RADIUS = 48 
        const val DEFAULT_PANEL_BG = "#E61A1C1E"
        const val DEFAULT_HIDE_BG = false
        const val DEFAULT_SHOW_TOOLS = true
        const val DEFAULT_ICON_SHAPE = SHAPE_SQUIRCLE
        const val DEFAULT_GESTURES = true
        const val DEFAULT_PILL_WIDTH = 3
        const val DEFAULT_PILL_COLOR = "#FFFFFF"
        const val DEFAULT_TAP_TO_OPEN = true
    }

    /** Resets all configuration to perfect defaults. */
    fun resetToDefaults() {
        prefs.edit {
            putString(KEY_PANEL_SIDE, DEFAULT_SIDE)
            putBoolean(KEY_AUTO_START, DEFAULT_AUTO_START)
            putBoolean(KEY_SHOW_PILL, DEFAULT_SHOW_PILL)
            putBoolean(KEY_HAPTIC_ENABLED, DEFAULT_HAPTIC)
            putInt(KEY_PANEL_OPACITY, DEFAULT_OPACITY)
            putInt(KEY_HANDLE_HEIGHT, DEFAULT_HANDLE_HEIGHT)
            putInt(KEY_HANDLE_WIDTH, DEFAULT_HANDLE_WIDTH)
            putInt(KEY_HANDLE_OFFSET, DEFAULT_HANDLE_OFFSET)
            putString(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR)
            putInt(KEY_PANEL_COLUMNS, DEFAULT_PANEL_COLS)
            putString(KEY_UI_THEME, DEFAULT_THEME)
            putInt(KEY_PANEL_RADIUS, DEFAULT_PANEL_RADIUS)
            putString(KEY_PANEL_BG_COLOR, DEFAULT_PANEL_BG)
            putBoolean(KEY_HIDE_BG, DEFAULT_HIDE_BG)
            putBoolean(KEY_SHOW_TOOLS, DEFAULT_SHOW_TOOLS)
            putString(KEY_ICON_SHAPE, DEFAULT_ICON_SHAPE)
            putBoolean(KEY_GESTURES_ENABLED, DEFAULT_GESTURES)
            putInt(KEY_PILL_WIDTH, DEFAULT_PILL_WIDTH)
            putString(KEY_PILL_COLOR, DEFAULT_PILL_COLOR)
            putBoolean(KEY_TAP_TO_OPEN, DEFAULT_TAP_TO_OPEN)
        }
    }

    /** Whether tapping the pill should open the panel. */
    var tapToOpen: Boolean
        get() = prefs.getBoolean(KEY_TAP_TO_OPEN, DEFAULT_TAP_TO_OPEN)
        set(value) = prefs.edit { putBoolean(KEY_TAP_TO_OPEN, value) }

    var gesturesEnabled: Boolean
        get() = prefs.getBoolean(KEY_GESTURES_ENABLED, DEFAULT_GESTURES)
        set(value) = prefs.edit { putBoolean(KEY_GESTURES_ENABLED, value) }

    var iconShape: String
        get() = prefs.getString(KEY_ICON_SHAPE, DEFAULT_ICON_SHAPE) ?: DEFAULT_ICON_SHAPE
        set(value) = prefs.edit { putString(KEY_ICON_SHAPE, value) }

    var showPill: Boolean
        get() = prefs.getBoolean(KEY_SHOW_PILL, DEFAULT_SHOW_PILL)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_PILL, value) }

    var hapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_ENABLED, DEFAULT_HAPTIC)
        set(value) = prefs.edit { putBoolean(KEY_HAPTIC_ENABLED, value) }

    var panelOpacity: Int
        get() = prefs.getInt(KEY_PANEL_OPACITY, DEFAULT_OPACITY)
        set(value) = prefs.edit { putInt(KEY_PANEL_OPACITY, value) }

    var handleHeight: Int
        get() = prefs.getInt(KEY_HANDLE_HEIGHT, DEFAULT_HANDLE_HEIGHT)
        set(value) = prefs.edit { putInt(KEY_HANDLE_HEIGHT, value) }

    var handleWidth: Int
        get() = prefs.getInt(KEY_HANDLE_WIDTH, DEFAULT_HANDLE_WIDTH)
        set(value) = prefs.edit { putInt(KEY_HANDLE_WIDTH, value) }

    var pillWidth: Int
        get() = prefs.getInt(KEY_PILL_WIDTH, DEFAULT_PILL_WIDTH)
        set(value) = prefs.edit { putInt(KEY_PILL_WIDTH, value) }

    var pillColor: String
        get() = prefs.getString(KEY_PILL_COLOR, DEFAULT_PILL_COLOR) ?: DEFAULT_PILL_COLOR
        set(value) = prefs.edit { putString(KEY_PILL_COLOR, value) }

    var handleVerticalOffset: Int
        get() = prefs.getInt(KEY_HANDLE_OFFSET, DEFAULT_HANDLE_OFFSET)
        set(value) = prefs.edit { putInt(KEY_HANDLE_OFFSET, value) }

    var accentColor: String
        get() = prefs.getString(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR) ?: DEFAULT_ACCENT_COLOR
        set(value) = prefs.edit { putString(KEY_ACCENT_COLOR, value) }

    var panelColumns: Int
        get() = prefs.getInt(KEY_PANEL_COLUMNS, DEFAULT_PANEL_COLS)
        set(value) = prefs.edit { putInt(KEY_PANEL_COLUMNS, value) }

    var uiTheme: String
        get() = prefs.getString(KEY_UI_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
        set(value) = prefs.edit { putString(KEY_UI_THEME, value) }

    var panelCornerRadius: Int
        get() = prefs.getInt(KEY_PANEL_RADIUS, DEFAULT_PANEL_RADIUS)
        set(value) = prefs.edit { putInt(KEY_PANEL_RADIUS, value) }

    var panelBackgroundColor: String
        get() = prefs.getString(KEY_PANEL_BG_COLOR, DEFAULT_PANEL_BG) ?: DEFAULT_PANEL_BG
        set(value) = prefs.edit { putString(KEY_PANEL_BG_COLOR, value) }

    var hideBackground: Boolean
        get() = prefs.getBoolean(KEY_HIDE_BG, DEFAULT_HIDE_BG)
        set(value) = prefs.edit { putBoolean(KEY_HIDE_BG, value) }

    var showTools: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TOOLS, DEFAULT_SHOW_TOOLS)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_TOOLS, value) }

    var isPremium: Boolean
        get() = prefs.getBoolean(KEY_IS_PREMIUM, false)
        set(value) = prefs.edit { putBoolean(KEY_IS_PREMIUM, value) }

    fun getPanelApps(): List<String> {
        val raw = prefs.getString(KEY_PANEL_APPS, "") ?: ""
        return if (raw.isBlank()) emptyList()
        else raw.split(DELIMITER).filter { it.isNotBlank() }
    }

    fun setPanelApps(packages: List<String>) {
        prefs.edit { putString(KEY_PANEL_APPS, packages.joinToString(DELIMITER)) }
    }

    fun addApp(packageName: String) {
        val current = getPanelApps().toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            setPanelApps(current)
        }
    }

    fun removeApp(packageName: String) {
        val current = getPanelApps().toMutableList()
        current.remove(packageName)
        setPanelApps(current)
    }

    fun isInPanel(packageName: String): Boolean = getPanelApps().contains(packageName)

    var panelSide: String
        get() = prefs.getString(KEY_PANEL_SIDE, DEFAULT_SIDE) ?: DEFAULT_SIDE
        set(value) = prefs.edit { putString(KEY_PANEL_SIDE, value) }

    var autoStart: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, DEFAULT_AUTO_START)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_START, value) }
}
