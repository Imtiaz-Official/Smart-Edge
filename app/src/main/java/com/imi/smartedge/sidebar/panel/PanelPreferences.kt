package com.imi.smartedge.sidebar.panel

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
        private const val KEY_USE_CUSTOM_ACCENT = "use_custom_accent"
        private const val KEY_PANEL_COLUMNS = "panel_columns"
        private const val KEY_UI_THEME = "ui_theme"

        private const val KEY_PANEL_RADIUS = "panel_radius"
        private const val KEY_PANEL_BG_COLOR = "panel_bg_color"
        private const val KEY_HIDE_BG = "hide_bg"
        private const val KEY_SHOW_TOOLS = "show_tools"
        private const val KEY_ICON_SHAPE = "icon_shape"
        private const val KEY_GESTURES_ENABLED = "gestures_enabled"
        private const val KEY_SHOW_IN_LANDSCAPE = "show_in_landscape"

        private const val KEY_PILL_WIDTH = "pill_width"
        private const val KEY_PILL_COLOR = "pill_color"
        private const val KEY_TAP_TO_OPEN = "tap_to_open"
        private const val KEY_DOUBLE_TAP_TO_OPEN = "double_tap_to_open"
        private const val KEY_TRIPLE_TAP_TO_OPEN = "triple_tap_to_open"
        private const val KEY_ICON_PACK = "selected_icon_pack"
        private const val KEY_ICON_PACK_LABEL = "selected_icon_pack_label"
        private const val KEY_BLUR_ENABLED = "blur_enabled"
        private const val KEY_BLUR_AMOUNT = "blur_amount"
        private const val KEY_SHOW_LOGS = "show_logs"
        private const val KEY_ANIM_SPEED = "animation_speed"
        private const val KEY_PICKER_GAP = "picker_gap"
        private const val KEY_SHOW_SYS_INFO = "show_sys_info"
        private const val KEY_SHOW_POWER_MENU = "show_power_menu"
        private const val KEY_SHOW_VOLUME_KEYS = "show_volume_keys"
        private const val KEY_SHOW_BRIGHTNESS_KEYS = "show_brightness_keys"
        private const val KEY_HOME_BUTTON_STYLE = "home_button_style"
        private const val KEY_FREEFORM_ENABLED = "freeform_enabled"
        private const val KEY_FREEFORM_WINDOW_MODE = "freeform_window_mode"
        private const val KEY_FREEFORM_CUSTOM_W = "freeform_custom_width"
        private const val KEY_FREEFORM_CUSTOM_H = "freeform_custom_height"
        private const val KEY_SCALE_FACTOR = "scale_factor"
        private const val KEY_PANEL_MAX_HEIGHT = "panel_max_height"
        private const val KEY_PICKER_MAX_HEIGHT = "picker_max_height"

        private const val DELIMITER = ","

        const val SIDE_RIGHT = "right"
        const val SIDE_LEFT = "left"

        const val THEME_ORIGIN = "origin"
        const val THEME_HYPEROS = "hyperos"
        const val THEME_REALME = "realme"
        const val THEME_RICH = "rich"

        const val SHAPE_SYSTEM = "system"
        const val SHAPE_CIRCLE = "circle"
        const val SHAPE_SQUIRCLE = "squircle"
        const val SHAPE_SQUARE = "square"
        const val SHAPE_ROUNDED = "rounded"

        const val STYLE_POWER = "power"
        const val STYLE_CLASSIC = "classic"

        // Freeform window size modes
        const val FREEFORM_MODE_STANDARD  = "standard"  // 80% screen, centered
        const val FREEFORM_MODE_PORTRAIT  = "portrait"  // Narrow tall window
        const val FREEFORM_MODE_MAXIMIZED = "maximized" // Full screen freeform
        const val FREEFORM_MODE_CUSTOM    = "custom"    // User-defined width & height %

        // Defaults
        val DEFAULT_SIDE = SIDE_RIGHT
        const val DEFAULT_AUTO_START = true
        const val DEFAULT_SHOW_PILL = true
        const val DEFAULT_HAPTIC = true
        const val DEFAULT_OPACITY = 100
        const val DEFAULT_HANDLE_HEIGHT = 80
        const val DEFAULT_HANDLE_WIDTH = 32
        const val DEFAULT_HANDLE_OFFSET = 0
        const val DEFAULT_ACCENT_COLOR = "#4A9EFF"
        const val DEFAULT_USE_CUSTOM_ACCENT = false
        const val DEFAULT_PANEL_COLS = 1
        const val DEFAULT_THEME = THEME_ORIGIN
        const val DEFAULT_PANEL_RADIUS = 20
        const val DEFAULT_PANEL_BG = "#E61A1C1E"
        const val DEFAULT_PILL_COLOR = "#FFFFFF"
        const val DEFAULT_HIDE_BG = false
        const val DEFAULT_SHOW_TOOLS = true
        val DEFAULT_ICON_SHAPE = SHAPE_SQUIRCLE
        const val DEFAULT_GESTURES = true
        const val DEFAULT_SHOW_LANDSCAPE = true
        const val DEFAULT_PILL_WIDTH = 5
        const val DEFAULT_TAP_TO_OPEN = false
        const val DEFAULT_DOUBLE_TAP_TO_OPEN = false
        const val DEFAULT_TRIPLE_TAP_TO_OPEN = false
        const val DEFAULT_ICON_PACK = "none"
        const val DEFAULT_SHOW_LOGS = false
        const val DEFAULT_BLUR_AMOUNT = 15
        const val DEFAULT_ANIM_SPEED = 400
        const val DEFAULT_PICKER_GAP = 20
        const val DEFAULT_HOME_BUTTON_STYLE = STYLE_POWER
    }

    /** Resets only UI colors (Accent and Background). */
    fun resetUIColors() {
        prefs.edit {
            putString(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR)
            putString(KEY_PANEL_BG_COLOR, DEFAULT_PANEL_BG)
            putBoolean(KEY_USE_CUSTOM_ACCENT, DEFAULT_USE_CUSTOM_ACCENT)
        }
    }

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
            putBoolean(KEY_USE_CUSTOM_ACCENT, DEFAULT_USE_CUSTOM_ACCENT)
            putInt(KEY_PANEL_COLUMNS, DEFAULT_PANEL_COLS)
            putString(KEY_UI_THEME, DEFAULT_THEME)
            putInt(KEY_PANEL_RADIUS, DEFAULT_PANEL_RADIUS)
            putString(KEY_PANEL_BG_COLOR, DEFAULT_PANEL_BG)
            putBoolean(KEY_HIDE_BG, DEFAULT_HIDE_BG)
            putBoolean(KEY_SHOW_TOOLS, DEFAULT_SHOW_TOOLS)
            putString(KEY_ICON_SHAPE, DEFAULT_ICON_SHAPE)
            putBoolean(KEY_GESTURES_ENABLED, DEFAULT_GESTURES)
            putBoolean(KEY_SHOW_IN_LANDSCAPE, DEFAULT_SHOW_LANDSCAPE)
            putInt(KEY_PILL_WIDTH, DEFAULT_PILL_WIDTH)
            putString(KEY_PILL_COLOR, DEFAULT_PILL_COLOR)
            putBoolean(KEY_TAP_TO_OPEN, DEFAULT_TAP_TO_OPEN)
            putBoolean(KEY_DOUBLE_TAP_TO_OPEN, DEFAULT_DOUBLE_TAP_TO_OPEN)
            putBoolean(KEY_TRIPLE_TAP_TO_OPEN, DEFAULT_TRIPLE_TAP_TO_OPEN)
            putString(KEY_ICON_PACK, DEFAULT_ICON_PACK)
            putString(KEY_ICON_PACK_LABEL, "System Default")
            putInt(KEY_BLUR_AMOUNT, DEFAULT_BLUR_AMOUNT)
            putBoolean(KEY_BLUR_ENABLED, false)
            putBoolean(KEY_SHOW_LOGS, false)
            putInt(KEY_ANIM_SPEED, DEFAULT_ANIM_SPEED)
            putInt(KEY_PICKER_GAP, DEFAULT_PICKER_GAP)
            putBoolean(KEY_SHOW_SYS_INFO, false)
            putBoolean(KEY_SHOW_POWER_MENU, false)
            putString(KEY_HOME_BUTTON_STYLE, DEFAULT_HOME_BUTTON_STYLE)
            putBoolean(KEY_FREEFORM_ENABLED, false)
            putString(KEY_FREEFORM_WINDOW_MODE, FREEFORM_MODE_STANDARD)
            putInt(KEY_FREEFORM_CUSTOM_W, 80)
            putInt(KEY_FREEFORM_CUSTOM_H, 80)
            putFloat(KEY_SCALE_FACTOR, 1.0f)
            putInt(KEY_PANEL_MAX_HEIGHT, 350)
            putInt(KEY_PICKER_MAX_HEIGHT, 450)
        }
    }

    var panelMaxHeight: Int
        get() = prefs.getInt(KEY_PANEL_MAX_HEIGHT, 350)
        set(value) = prefs.edit { putInt(KEY_PANEL_MAX_HEIGHT, value) }

    var pickerMaxHeight: Int
        get() = prefs.getInt(KEY_PICKER_MAX_HEIGHT, 450)
        set(value) = prefs.edit { putInt(KEY_PICKER_MAX_HEIGHT, value) }

    var pillColor: String
        get() = prefs.getString(KEY_PILL_COLOR, DEFAULT_PILL_COLOR) ?: DEFAULT_PILL_COLOR
        set(value) = prefs.edit { putString(KEY_PILL_COLOR, value) }

    var freeformEnabled: Boolean
        get() = prefs.getBoolean(KEY_FREEFORM_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_FREEFORM_ENABLED, value) }

    var freeformWindowMode: String
        get() = prefs.getString(KEY_FREEFORM_WINDOW_MODE, FREEFORM_MODE_STANDARD) ?: FREEFORM_MODE_STANDARD
        set(value) = prefs.edit { putString(KEY_FREEFORM_WINDOW_MODE, value) }

    var freeformCustomWidth: Int
        get() = prefs.getInt(KEY_FREEFORM_CUSTOM_W, 80)
        set(value) = prefs.edit { putInt(KEY_FREEFORM_CUSTOM_W, value) }

    var freeformCustomHeight: Int
        get() = prefs.getInt(KEY_FREEFORM_CUSTOM_H, 80)
        set(value) = prefs.edit { putInt(KEY_FREEFORM_CUSTOM_H, value) }

    var scaleFactor: Float
        get() = prefs.getFloat(KEY_SCALE_FACTOR, 1.0f)
        set(value) = prefs.edit { putFloat(KEY_SCALE_FACTOR, value) }

    var showSysInfo: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SYS_INFO, false)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_SYS_INFO, value) }

    var showPowerMenu: Boolean
        get() = prefs.getBoolean(KEY_SHOW_POWER_MENU, false)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_POWER_MENU, value) }

    var showVolumeKeys: Boolean
        get() = prefs.getBoolean(KEY_SHOW_VOLUME_KEYS, false)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_VOLUME_KEYS, value) }

    var showBrightnessKeys: Boolean
        get() = prefs.getBoolean(KEY_SHOW_BRIGHTNESS_KEYS, false)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_BRIGHTNESS_KEYS, value) }

    var homeButtonStyle: String
        get() = prefs.getString(KEY_HOME_BUTTON_STYLE, DEFAULT_HOME_BUTTON_STYLE) ?: DEFAULT_HOME_BUTTON_STYLE
        set(value) = prefs.edit { putString(KEY_HOME_BUTTON_STYLE, value) }

    var pickerGap: Int
        get() = prefs.getInt(KEY_PICKER_GAP, DEFAULT_PICKER_GAP)
        set(value) = prefs.edit { putInt(KEY_PICKER_GAP, value) }

    var useCustomAccent: Boolean
        get() = prefs.getBoolean(KEY_USE_CUSTOM_ACCENT, DEFAULT_USE_CUSTOM_ACCENT)
        set(value) = prefs.edit { putBoolean(KEY_USE_CUSTOM_ACCENT, value) }

    var selectedIconPack: String
        get() = prefs.getString(KEY_ICON_PACK, DEFAULT_ICON_PACK) ?: DEFAULT_ICON_PACK
        set(value) = prefs.edit { putString(KEY_ICON_PACK, value) }

    var iconPackLabel: String
        get() = prefs.getString(KEY_ICON_PACK_LABEL, "System Default") ?: "System Default"
        set(value) = prefs.edit { putString(KEY_ICON_PACK_LABEL, value) }

    var tapToOpen: Boolean
        get() = prefs.getBoolean(KEY_TAP_TO_OPEN, DEFAULT_TAP_TO_OPEN)
        set(value) = prefs.edit { putBoolean(KEY_TAP_TO_OPEN, value) }

    var doubleTapToOpen: Boolean
        get() = prefs.getBoolean(KEY_DOUBLE_TAP_TO_OPEN, DEFAULT_DOUBLE_TAP_TO_OPEN)
        set(value) = prefs.edit { putBoolean(KEY_DOUBLE_TAP_TO_OPEN, value) }

    var tripleTapToOpen: Boolean
        get() = prefs.getBoolean(KEY_TRIPLE_TAP_TO_OPEN, DEFAULT_TRIPLE_TAP_TO_OPEN)
        set(value) = prefs.edit { putBoolean(KEY_TRIPLE_TAP_TO_OPEN, value) }

    var gesturesEnabled: Boolean
        get() = prefs.getBoolean(KEY_GESTURES_ENABLED, DEFAULT_GESTURES)
        set(value) = prefs.edit { putBoolean(KEY_GESTURES_ENABLED, value) }

    var showInLandscape: Boolean
        get() = prefs.getBoolean(KEY_SHOW_IN_LANDSCAPE, DEFAULT_SHOW_LANDSCAPE)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_IN_LANDSCAPE, value) }

    var iconShape: String
        get() = prefs.getString(KEY_ICON_SHAPE, DEFAULT_ICON_SHAPE) ?: DEFAULT_ICON_SHAPE
        set(value) = prefs.edit { putString(KEY_ICON_SHAPE, value) }

    var showPill: Boolean
        get() = prefs.getBoolean(KEY_SHOW_PILL, DEFAULT_SHOW_PILL)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_PILL, value) }

    var hapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_ENABLED, DEFAULT_HAPTIC)
        set(value) = prefs.edit { putBoolean(KEY_HAPTIC_ENABLED, value) }

    var blurEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLUR_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_BLUR_ENABLED, value) }

    var blurAmount: Int
        get() = prefs.getInt(KEY_BLUR_AMOUNT, DEFAULT_BLUR_AMOUNT)
        set(value) = prefs.edit { putInt(KEY_BLUR_AMOUNT, value) }

    var panelOpacity: Int
        get() = prefs.getInt(KEY_PANEL_OPACITY, DEFAULT_OPACITY)
        set(value) = prefs.edit { putInt(KEY_PANEL_OPACITY, value) }

    var handleHeight: Int
        get() = prefs.getInt(KEY_HANDLE_HEIGHT, DEFAULT_HANDLE_HEIGHT)
        set(value) = prefs.edit { putInt(KEY_HANDLE_HEIGHT, value) }

    var handleWidth: Int
        get() = prefs.getInt(KEY_HANDLE_WIDTH, DEFAULT_HANDLE_WIDTH)
        set(value) = prefs.edit { putInt(KEY_HANDLE_WIDTH, value) }

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

    var pillWidth: Int
        get() = prefs.getInt(KEY_PILL_WIDTH, DEFAULT_PILL_WIDTH)
        set(value) = prefs.edit { putInt(KEY_PILL_WIDTH, value) }

    var showTools: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TOOLS, DEFAULT_SHOW_TOOLS)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_TOOLS, value) }

    var showLogs: Boolean
        get() = prefs.getBoolean(KEY_SHOW_LOGS, DEFAULT_SHOW_LOGS)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_LOGS, value) }

    var animSpeed: Int
        get() = prefs.getInt(KEY_ANIM_SPEED, DEFAULT_ANIM_SPEED)
        set(value) = prefs.edit { putInt(KEY_ANIM_SPEED, value) }

    var setupCompleted: Boolean
        get() = prefs.getBoolean("setup_completed_new", false)
        set(value) = prefs.edit { putBoolean("setup_completed_new", value) }

    var serviceEnabled: Boolean
        get() = prefs.getBoolean("service_enabled", true)
        set(value) = prefs.edit { putBoolean("service_enabled", value) }

    fun getPanelApps(): List<String> {
        val raw = prefs.getString(KEY_PANEL_APPS, "") ?: ""
        return if (raw.isBlank()) emptyList()
        else raw.split(DELIMITER)
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun setPanelApps(packages: List<String>) {
        val uniquePackages = packages.filter { it.isNotBlank() }.distinct()
        prefs.edit { putString(KEY_PANEL_APPS, uniquePackages.joinToString(DELIMITER)) }
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
