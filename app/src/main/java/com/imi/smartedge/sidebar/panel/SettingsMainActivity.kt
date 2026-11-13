package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsMainBinding
import com.imi.smartedge.sidebar.panel.databinding.ItemSearchResultBinding

class SettingsMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsMainBinding
    private lateinit var panelPrefs: PanelPreferences

    private data class SettingItem(
        val title: String,
        val description: String,
        val category: String,
        val keywords: String,
        val targetActivity: Class<*>,
        val targetId: String? = null
    )

    private val allSettings = mutableListOf<SettingItem>()
    private lateinit var searchAdapter: SearchResultsAdapter

    companion object {
        const val EXTRA_SCROLL_TO = "extra_scroll_to"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        binding.toolbar.title = getString(R.string.settings_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        panelPrefs = PanelPreferences(this)
        
        initializeSettingsList()
        setupSearch()
        setupListeners()
        
        binding.btnGithubTop.setOnClickListener {
            openGithub()
        }

        // Handle System Back to close search
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.rvSettingsResults.visibility == View.VISIBLE || 
                    binding.layoutSearchEmpty.visibility == View.VISIBLE || 
                    binding.etSettingsSearch.visibility == View.VISIBLE) {
                    collapseSearch()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initializeSettingsList() {
        allSettings.clear()

        val catAppearance = getString(R.string.section_appearance)
        val catInteraction = getString(R.string.section_interaction)
        val catHandle = getString(R.string.section_handle)
        val catTools = getString(R.string.section_tools)
        val catPermissions = getString(R.string.permissions_dashboard_title)
        val catGeneral = getString(R.string.misc_section_general)

        // ----------------- Category 1: UI & Appearance -----------------
        allSettings.add(SettingItem(
            getString(R.string.app_theme),
            "Dark, Light, or Follow System theme",
            catAppearance,
            "theme mode dark light oled system color appearance style",
            AppearanceSettingsActivity::class.java,
            "feature_theme_mode"
        ))
        allSettings.add(SettingItem(
            getString(R.string.ui_style_theme),
            "Standard floating panel or compact sidebar style",
            catAppearance,
            "ui style floating panel layout appearance compact standard modern",
            AppearanceSettingsActivity::class.java,
            "layoutUIStyle"
        ))
        allSettings.add(SettingItem(
            getString(R.string.blur_title),
            "Frosted glass effect behind sidebar",
            catAppearance,
            "blur background frosted glass translucent transparent effect visual",
            AppearanceSettingsActivity::class.java,
            "feature_blur"
        ))
        allSettings.add(SettingItem(
            getString(R.string.blur_intensity),
            "Adjust frosted glass blur strength",
            catAppearance,
            "blur intensity amount slider frosted strength level",
            AppearanceSettingsActivity::class.java,
            "feature_blur_intensity"
        ))
        allSettings.add(SettingItem(
            getString(R.string.invisible_bg),
            "Hide sidebar background panel completely",
            catAppearance,
            "invisible background transparent hide panel flat clear",
            AppearanceSettingsActivity::class.java,
            "feature_hide_bg"
        ))
        allSettings.add(SettingItem(
            getString(R.string.appearance_show_bottom_arrow),
            "Display indicator arrow below panel",
            catAppearance,
            "arrow indicator bottom chevron toggle expand",
            AppearanceSettingsActivity::class.java,
            "feature_show_bottom_arrow"
        ))
        allSettings.add(SettingItem(
            getString(R.string.panel_columns),
            "Display 1 or 2 columns of apps",
            catAppearance,
            "columns grid layout 1 2 width apps count items",
            AppearanceSettingsActivity::class.java,
            "feature_columns"
        ))
        allSettings.add(SettingItem(
            getString(R.string.appearance_auto_close),
            "Automatically dismiss sidebar after timeout",
            catAppearance,
            "auto close dismiss timer delay sidebar hide timeout",
            AppearanceSettingsActivity::class.java,
            "feature_auto_close"
        ))
        allSettings.add(SettingItem(
            getString(R.string.icon_shape),
            "Squircle, circle, rounded, or teardrop",
            catAppearance,
            "icon shape squircle circle rounded square teardrop mask maskable",
            AppearanceSettingsActivity::class.java,
            "feature_icon_shape"
        ))
        allSettings.add(SettingItem(
            getString(R.string.icon_pack),
            "Apply third-party icon packs",
            catAppearance,
            "icon pack custom icons themes iconpack nova launcher",
            AppearanceSettingsActivity::class.java,
            "feature_icon_pack"
        ))
        allSettings.add(SettingItem(
            getString(R.string.icon_scale),
            "Adjust size of app icons in sidebar",
            catAppearance,
            "icon size scale bigger smaller dimension resize zoom factor",
            AppearanceSettingsActivity::class.java,
            "feature_icon_scale"
        ))
        allSettings.add(SettingItem(
            getString(R.string.appearance_show_app_names),
            getString(R.string.appearance_show_app_names_desc),
            catAppearance,
            "show hide app names labels text titles icons only tablets clean minimal master toggle",
            AppearanceSettingsActivity::class.java,
            "layout_show_app_names_master"
        ))
        allSettings.add(SettingItem(
            "${getString(R.string.appearance_show_app_names)}: ${getString(R.string.appearance_app_names_sidebar)}",
            getString(R.string.appearance_app_names_sidebar_desc),
            catAppearance,
            "show hide app names labels text titles icons only tablets clean minimal sidebar panel",
            AppearanceSettingsActivity::class.java,
            "layout_show_app_names_sidebar"
        ))
        allSettings.add(SettingItem(
            "${getString(R.string.appearance_show_app_names)}: ${getString(R.string.appearance_app_names_drawer)}",
            getString(R.string.appearance_app_names_drawer_desc),
            catAppearance,
            "show hide app names labels text titles icons only tablets clean minimal drawer all apps grid",
            AppearanceSettingsActivity::class.java,
            "layout_show_app_names_drawer"
        ))
        allSettings.add(SettingItem(
            getString(R.string.custom_accent_title),
            "Personalize highlights and accents",
            catAppearance,
            "accent color tint custom hex palette theme highlight color picker",
            AppearanceSettingsActivity::class.java,
            "feature_accent_color"
        ))

        // ----------------- Category 2: Gestures & Interaction -----------------
        allSettings.add(SettingItem(
            getString(R.string.edge_gestures),
            "Swipe from screen edge to open sidebar",
            catInteraction,
            "edge gestures swipe slide trigger activate open sidebar",
            InteractionSettingsActivity::class.java,
            "feature_gestures"
        ))
        allSettings.add(SettingItem(
            getString(R.string.only_on_home),
            "Restrict sidebar trigger to launcher",
            catInteraction,
            "only on home desktop launcher lock apps restrict",
            InteractionSettingsActivity::class.java,
            "feature_only_on_home"
        ))
        allSettings.add(SettingItem(
            getString(R.string.edge_sensitivity),
            "Adjust swipe detection sensitivity",
            catInteraction,
            "swipe sensitivity trigger threshold touch response velocity",
            InteractionSettingsActivity::class.java,
            "layout_swipe_sensitivity"
        ))
        allSettings.add(SettingItem(
            getString(R.string.tap_gestures),
            "Single, double, and long tap actions on edge handle",
            catInteraction,
            "tap gestures single double long press action shortcut click handle",
            InteractionSettingsActivity::class.java,
            "layoutTapGestures"
        ))
        allSettings.add(SettingItem(
            getString(R.string.haptic_feedback),
            "Vibration when touching edge or opening panel",
            catInteraction,
            "haptic feedback vibration vibrate touch buzz tactile response",
            InteractionSettingsActivity::class.java,
            "feature_haptic"
        ))
        allSettings.add(SettingItem(
            getString(R.string.slide_brightness),
            "Slide up/down on edge to adjust screen brightness",
            catInteraction,
            "slide brightness volume screen light control gesture edge",
            InteractionSettingsActivity::class.java,
            "feature_slide_brightness"
        ))
        allSettings.add(SettingItem(
            getString(R.string.slide_volume),
            "Slide up/down on edge to adjust media volume",
            catInteraction,
            "slide volume sound audio media loudness control gesture",
            InteractionSettingsActivity::class.java,
            "feature_slide_volume"
        ))
        allSettings.add(SettingItem(
            getString(R.string.edge_seek_title),
            "Fine-tune edge slide sensitivity",
            catInteraction,
            "slide sensitivity gesture response rate volume brightness",
            InteractionSettingsActivity::class.java,
            "layoutSlideSensitivity"
        ))
        allSettings.add(SettingItem(
            getString(R.string.enable_notch_gestures),
            "Tap camera cutout / notch for actions",
            catInteraction,
            "notch camera cutout gestures hole punch touch tap actions",
            InteractionSettingsActivity::class.java,
            "feature_notch_gestures"
        ))
        allSettings.add(SettingItem(
            getString(R.string.notch_tap_gestures),
            "Configure single, double, long tap on camera notch",
            catInteraction,
            "notch tap gestures actions single double long press cutout",
            InteractionSettingsActivity::class.java,
            "layoutNotchTapGestures"
        ))
        allSettings.add(SettingItem(
            getString(R.string.feature_landscape_label),
            "Enable sidebar when device is rotated",
            catInteraction,
            "landscape orientation horizontal tablet rotation rotate",
            InteractionSettingsActivity::class.java,
            "feature_show_landscape"
        ))
        allSettings.add(SettingItem(
            getString(R.string.feature_notification_apps_label),
            "Show notification counts on sidebar apps",
            catInteraction,
            "notification apps badge preview alerts unread count dots",
            InteractionSettingsActivity::class.java,
            "feature_notification_apps"
        ))
        allSettings.add(SettingItem(
            getString(R.string.feature_remember_scroll_label),
            "Keep your place when reopening app drawer",
            catInteraction,
            "remember scroll position restore last location list drawer",
            InteractionSettingsActivity::class.java,
            "feature_remember_scroll"
        ))
        allSettings.add(SettingItem(
            getString(R.string.feature_auto_show_keyboard_label),
            "Open keyboard immediately in app drawer",
            catInteraction,
            "auto show keyboard search app drawer input text typing",
            InteractionSettingsActivity::class.java,
            "feature_auto_show_keyboard"
        ))
        allSettings.add(SettingItem(
            getString(R.string.interaction_drag_split),
            "Drag app from sidebar to split screen",
            catInteraction,
            "drag split screen multitasking dual window side by side",
            InteractionSettingsActivity::class.java,
            "feature_drag_split"
        ))
        allSettings.add(SettingItem(
            getString(R.string.interaction_freeform),
            "Launch apps in resizable floating windows",
            catInteraction,
            "freeform floating window pop-up popup resizable desktop mode",
            InteractionSettingsActivity::class.java,
            "feature_freeform"
        ))
        allSettings.add(SettingItem(
            getString(R.string.interaction_window_size),
            "Small, medium, or large window size",
            catInteraction,
            "freeform size dimensions window width height scale preset",
            InteractionSettingsActivity::class.java,
            "layout_freeform_size"
        ))
        allSettings.add(SettingItem(
            getString(R.string.freeform_custom_windowing_mode),
            "Advanced activity window flags",
            catInteraction,
            "custom windowing mode flags activity options bounds",
            InteractionSettingsActivity::class.java,
            "layout_custom_windowing_mode"
        ))
        allSettings.add(SettingItem(
            getString(R.string.freeform_use_shell),
            "Use Root/Shizuku for launching freeform windows",
            catInteraction,
            "freeform shell commands shizuku root adb terminal am start",
            InteractionSettingsActivity::class.java,
            "layout_freeform_use_shell"
        ))
        allSettings.add(SettingItem(
            getString(R.string.perm_automation_title),
            "High-speed Root / Shizuku gesture engine",
            catInteraction,
            "automation shizuku root fast gestures native speed input gesture engine",
            InteractionSettingsActivity::class.java,
            "btnAutomationSettings"
        ))
        allSettings.add(SettingItem(
            getString(R.string.picker_title_hide),
            "Disable gestures in selected fullscreen apps",
            catInteraction,
            "fullscreen blacklist whitelist disable in apps games ignore",
            InteractionSettingsActivity::class.java,
            "layoutFullscreenWhitelist"
        ))
        allSettings.add(SettingItem(
            getString(R.string.game_mode),
            "Block accidental gestures while gaming",
            catInteraction,
            "game mode block gestures gaming disable prevent accidental touch",
            InteractionSettingsActivity::class.java,
            "feature_gameMode"
        ))

        // ----------------- Category 3: Handle & Pill -----------------
        allSettings.add(SettingItem(
            getString(R.string.panel_side_label),
            "Place edge handle on left or right screen edge",
            catHandle,
            "handle side left right position edge screen switch placement side picker",
            HandleSettingsActivity::class.java,
            "layoutSidePicker"
        ))
        allSettings.add(SettingItem(
            getString(R.string.visible_pill),
            "Pill, line, bar, or hidden style",
            catHandle,
            "handle mode pill line bar style appearance hidden transparent",
            HandleSettingsActivity::class.java,
            "feature_handle_mode"
        ))
        allSettings.add(SettingItem(
            getString(R.string.pill_color),
            "Customize pill background and transparency",
            catHandle,
            "handle color pill tint custom hex opacity transparency color picker",
            HandleSettingsActivity::class.java,
            "feature_pill_color"
        ))
        allSettings.add(SettingItem(
            getString(R.string.pill_thickness),
            "Adjust visual thickness of handle",
            catHandle,
            "handle thickness width size visible dimension",
            HandleSettingsActivity::class.java,
            "feature_thickness"
        ))
        allSettings.add(SettingItem(
            getString(R.string.touch_area_width),
            "Set swipe detection touch area width",
            catHandle,
            "trigger width touch sensitivity edge catch swipe area",
            HandleSettingsActivity::class.java,
            "feature_trigger_width"
        ))
        allSettings.add(SettingItem(
            getString(R.string.handle_height),
            "Adjust length of edge handle",
            catHandle,
            "handle height length tall short dimension vertical size",
            HandleSettingsActivity::class.java,
            "feature_handle_height"
        ))
        allSettings.add(SettingItem(
            "Vertical Position",
            "Move handle up or down along screen edge",
            catHandle,
            "handle vertical position vpos up down offset align",
            HandleSettingsActivity::class.java,
            "feature_vpos"
        ))

        // ----------------- Category 4: Dashboard Tools -----------------
        allSettings.add(SettingItem(
            getString(R.string.tools_enable_master),
            "Quick system utility tiles in sidebar",
            catTools,
            "tools shortcuts master utility toggle quick settings dashboard",
            ToolsSettingsActivity::class.java,
            "feature_tools_master"
        ))
        allSettings.add(SettingItem(
            getString(R.string.tools_dedicated_folder),
            "Separate tools into their own expandable folder",
            catTools,
            "dedicated tools folder separate panel apps group drawer",
            ToolsSettingsActivity::class.java,
            "feature_tools_panel"
        ))
        allSettings.add(SettingItem(
            getString(R.string.tools_sys_info),
            "Live RAM, CPU, battery, and storage monitor",
            catTools,
            "system info ram memory cpu battery storage hardware monitor stats",
            ToolsSettingsActivity::class.java,
            "feature_sys_info"
        ))
        allSettings.add(SettingItem(
            getString(R.string.tools_power_menu),
            "One-tap restart, shutdown, and lock options",
            catTools,
            "power menu reboot restart shutdown power off lock sleep",
            ToolsSettingsActivity::class.java,
            "feature_power_menu"
        ))
        allSettings.add(SettingItem(
            getString(R.string.tools_volume_controls),
            "Quick volume up, down, and mute keys",
            catTools,
            "volume keys sound audio media controls buttons mute loud audio",
            ToolsSettingsActivity::class.java,
            "feature_volume_keys"
        ))
        allSettings.add(SettingItem(
            getString(R.string.tools_brightness_controls),
            "Quick screen brightness adjustments",
            catTools,
            "brightness keys display screen light controls dim bright lux",
            ToolsSettingsActivity::class.java,
            "feature_brightness_keys"
        ))
        allSettings.add(SettingItem(
            getString(R.string.tools_screenshot_button),
            "Instant screen capture shortcut",
            catTools,
            "screenshot capture screen grab snip image photo screen capture",
            ToolsSettingsActivity::class.java,
            "feature_screenshot"
        ))
        allSettings.add(SettingItem(
            getString(R.string.tools_shizuku_service),
            "Grant elevated permissions for system tools",
            catTools,
            "shizuku root permissions elevated adb tools power screenshot",
            ToolsSettingsActivity::class.java,
            "cardShizuku"
        ))

        // ----------------- Category 5: System Permissions -----------------
        allSettings.add(SettingItem(
            getString(R.string.permissions_dashboard_title),
            "Health status and required system access",
            catPermissions,
            "permissions dashboard security health overview status system access",
            PermissionsActivity::class.java,
            "cardHealthStatus"
        ))
        allSettings.add(SettingItem(
            getString(R.string.perm_overlay_title),
            getString(R.string.perm_overlay_desc),
            catPermissions,
            "display over other apps overlay floating window draw permission",
            PermissionsActivity::class.java,
            "cardOverlay"
        ))
        allSettings.add(SettingItem(
            getString(R.string.perm_accessibility_title),
            getString(R.string.perm_accessibility_desc),
            catPermissions,
            "accessibility service system back home gestures permission dismiss",
            PermissionsActivity::class.java,
            "cardAccessibility"
        ))
        allSettings.add(SettingItem(
            getString(R.string.perm_battery_title),
            getString(R.string.perm_battery_desc),
            catPermissions,
            "battery optimization doze background killing whitelist unrestricted power",
            PermissionsActivity::class.java,
            "cardBattery"
        ))
        allSettings.add(SettingItem(
            getString(R.string.perm_autostart_title),
            getString(R.string.perm_autostart_desc),
            catPermissions,
            "auto start background persistence keep alive restart boot recover",
            PermissionsActivity::class.java,
            "cardAutoStart"
        ))
        allSettings.add(SettingItem(
            getString(R.string.perm_notifications_title),
            getString(R.string.perm_notifications_desc),
            catPermissions,
            "notification access hub listener permission badges unread previews",
            PermissionsActivity::class.java,
            "cardNotifications"
        ))
        allSettings.add(SettingItem(
            getString(R.string.perm_automation_title),
            getString(R.string.perm_automation_desc),
            catPermissions,
            "root shizuku gesture engine fast automation native permissions gestures",
            PermissionsActivity::class.java,
            "cardAutomation"
        ))

        // ----------------- Category 6: General & Misc -----------------
        allSettings.add(SettingItem(
            getString(R.string.misc_app_language),
            "English, Español, Українська, বাংলা, हिन्दी",
            catGeneral,
            "language idioma мова বাংলা হিন্দি bengali hindi english spanish ukrainian locale translate",
            MiscellaneousSettingsActivity::class.java,
            "feature_language"
        ))
        allSettings.add(SettingItem(
            getString(R.string.misc_export_title),
            getString(R.string.misc_export_desc),
            catGeneral,
            "backup export settings json configuration save file share",
            MiscellaneousSettingsActivity::class.java,
            "btnExportSettings"
        ))
        allSettings.add(SettingItem(
            getString(R.string.misc_import_title),
            getString(R.string.misc_import_desc),
            catGeneral,
            "restore import settings load config json recovery backup file",
            MiscellaneousSettingsActivity::class.java,
            "btnImportSettings"
        ))
        allSettings.add(SettingItem(
            getString(R.string.misc_crash_logs_title),
            getString(R.string.misc_crash_logs_desc),
            catGeneral,
            "crash logs diagnostics debug errors reports view bug report",
            MiscellaneousSettingsActivity::class.java,
            "btnCrashLogs"
        ))
        allSettings.add(SettingItem(
            getString(R.string.add_apps),
            getString(R.string.manage_apps_desc),
            catGeneral,
            "add apps choose select picker customize rearrange order drawer apps list",
            AppPickerActivity::class.java
        ))
        allSettings.add(SettingItem(
            getString(R.string.btn_support_dev),
            "Donate via PayPal, Patreon, or Buy Me a Coffee",
            catGeneral,
            "support donate donation sponsor paypal patreon buy coffee developer contribute",
            SupportActivity::class.java
        ))
        allSettings.add(SettingItem(
            getString(R.string.settings_reset_defaults),
            getString(R.string.reset_defaults_desc),
            catGeneral,
            "reset defaults factory restore wipe clean revert settings",
            SettingsMainActivity::class.java,
            "btnReset"
        ))
        allSettings.add(SettingItem(
            getString(R.string.view_repo_title),
            getString(R.string.view_repo_desc),
            "Project",
            "github source code open repo smartedge issue bug git",
            SettingsMainActivity::class.java,
            "btnGithubTop"
        ))

        // ----------------- Category 5: System Permissions -----------------
        allSettings.add(SettingItem(
            getString(R.string.permissions_dashboard_title),
            "Health status and required system access",
            catPermissions,
            "permissions dashboard security health overview status system access",
            PermissionsActivity::class.java,
            "cardHealthStatus"
        ))
        allSettings.add(SettingItem(
            getString(R.string.perm_overlay_title),
            getString(R.string.perm_overlay_desc),
            catPermissions,
            "display over other apps overlay floating window draw permission",
            PermissionsActivity::class.java,
            "cardOverlay"
        ))
        allSettings.add(SettingItem(
            getString(R.string.perm_accessibility_title),
            getString(R.string.perm_accessibility_desc),
            catPermissions,
            "accessibility service system back home gestures permission dismiss",
            PermissionsActivity::class.java,
            "cardAccessibility"
        ))
        allSettings.add(SettingItem(
            getString(R.string.perm_battery_title),
            getString(R.string.perm_battery_desc),
            catPermissions,
            "battery optimization doze background killing whitelist unrestricted power",
            PermissionsActivity::class.java,
            "cardBattery"
        ))
        allSettings.add(SettingItem(
            getString(R.string.perm_autostart_title),
            getString(R.string.perm_autostart_desc),
            catPermissions,
            "auto start background persistence keep alive restart boot recover",
            PermissionsActivity::class.java,
            "cardAutoStart"
        ))
        allSettings.add(SettingItem(
            getString(R.string.perm_notifications_title),
            getString(R.string.perm_notifications_desc),
            catPermissions,
            "notification access hub listener permission badges unread previews",
            PermissionsActivity::class.java,
            "cardNotifications"
        ))
        allSettings.add(SettingItem(
            getString(R.string.perm_automation_title),
            getString(R.string.perm_automation_desc),
            catPermissions,
            "root shizuku gesture engine fast automation native permissions gestures",
            PermissionsActivity::class.java,
            "cardAutomation"
        ))

        // ----------------- Category 6: General & Misc -----------------
        allSettings.add(SettingItem(
            getString(R.string.misc_app_language),
            "English, Español, Українська, বাংলা, हिन्दी",
            catGeneral,
            "language idioma мова বাংলা হিন্দি bengali hindi english spanish ukrainian locale translate",
            MiscellaneousSettingsActivity::class.java,
            "feature_language"
        ))
        allSettings.add(SettingItem(
            getString(R.string.misc_export_title),
            getString(R.string.misc_export_desc),
            catGeneral,
            "backup export settings json configuration save file share",
            MiscellaneousSettingsActivity::class.java,
            "btnExportSettings"
        ))
        allSettings.add(SettingItem(
            getString(R.string.misc_import_title),
            getString(R.string.misc_import_desc),
            catGeneral,
            "restore import settings load config json recovery backup file",
            MiscellaneousSettingsActivity::class.java,
            "btnImportSettings"
        ))
        allSettings.add(SettingItem(
            getString(R.string.misc_crash_logs_title),
            getString(R.string.misc_crash_logs_desc),
            catGeneral,
            "crash logs diagnostics debug errors reports view bug report",
            MiscellaneousSettingsActivity::class.java,
            "btnCrashLogs"
        ))
        allSettings.add(SettingItem(
            getString(R.string.add_apps),
            getString(R.string.manage_apps_desc),
            catGeneral,
            "add apps choose select picker customize rearrange order drawer apps list",
            AppPickerActivity::class.java
        ))
        allSettings.add(SettingItem(
            getString(R.string.btn_support_dev),
            "Donate via PayPal, Patreon, or Buy Me a Coffee",
            catGeneral,
            "support donate donation sponsor paypal patreon buy coffee developer contribute",
            SupportActivity::class.java
        ))
        allSettings.add(SettingItem(
            getString(R.string.settings_reset_defaults),
            getString(R.string.reset_defaults_desc),
            catGeneral,
            "reset defaults factory restore wipe clean revert settings",
            SettingsMainActivity::class.java,
            "btnReset"
        ))
        allSettings.add(SettingItem(
            getString(R.string.view_repo_title),
            getString(R.string.view_repo_desc),
            "Project",
            "github source code open repo smartedge issue bug git",
            SettingsMainActivity::class.java,
            "btnGithubTop"
        ))
    }

    private fun setupSearch() {
        searchAdapter = SearchResultsAdapter { item, itemView ->
            // Smooth tactile micro-pulse on clicked card
            SpringAnimator.scalePulse(itemView)

            // Hide keyboard cleanly
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSettingsSearch.windowToken, 0)

            // Smooth delay to allow click animation and clean transition
            itemView.postDelayed({
                collapseSearch()
                
                if (item.targetActivity == SettingsMainActivity::class.java) {
                    if (item.targetId == "btnReset") {
                        binding.settingsMainContent.scrollToTargetView(binding.btnReset, postDelay = 150L)
                    } else if (item.targetId == "btnGithubTop") {
                        openGithub()
                    }
                    return@postDelayed
                }

                val intent = Intent(this, item.targetActivity)
                if (item.targetId != null) {
                    intent.putExtra(EXTRA_SCROLL_TO, item.targetId)
                }

                val options = androidx.core.app.ActivityOptionsCompat.makeCustomAnimation(
                    this,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                startActivity(intent, options.toBundle())
            }, 120L)
        }
        binding.rvSettingsResults.layoutManager = LinearLayoutManager(this)
        binding.rvSettingsResults.adapter = searchAdapter

        binding.etSettingsSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSettings(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSettingsSearch.setText("")
        }

        // Setup expansion animation
        binding.searchCard.setOnClickListener {
            if (binding.etSettingsSearch.visibility == View.GONE) {
                expandSearch()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (binding.rvSettingsResults.visibility == View.VISIBLE || 
            binding.layoutSearchEmpty.visibility == View.VISIBLE ||
            binding.etSettingsSearch.visibility == View.VISIBLE) {
            collapseSearch()
            return true
        }
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupListeners() {
        binding.btnAppearance.setOnClickListener {
            startActivity(Intent(this, AppearanceSettingsActivity::class.java))
        }

        binding.btnInteraction.setOnClickListener {
            startActivity(Intent(this, InteractionSettingsActivity::class.java))
        }

        binding.btnHandle.setOnClickListener {
            startActivity(Intent(this, HandleSettingsActivity::class.java))
        }

        binding.btnTools.setOnClickListener {
            startActivity(Intent(this, ToolsSettingsActivity::class.java))
        }

        binding.btnDonation.setOnClickListener {
            startActivity(Intent(this, SupportActivity::class.java))
        }

        binding.btnReset.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_reset_title)
                .setMessage(R.string.dialog_reset_msg)
                .setPositiveButton(R.string.btn_reset) { _, _ ->
                    panelPrefs.resetToDefaults()
                    applyGlobalRefresh()
                    binding.root.showModernToast(getString(R.string.toast_reset_success))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.btnMisc.setOnClickListener {
            startActivity(Intent(this, MiscellaneousSettingsActivity::class.java))
        }
    }

    private fun openGithub() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Imtiaz-Official/Smart-Edge"))
            startActivity(intent)
        } catch (e: Exception) {
            binding.root.showModernToast("Could not open browser")
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun expandSearch() {
        val transition = android.transition.TransitionSet().apply {
            addTransition(android.transition.ChangeBounds())
            addTransition(android.transition.Fade())
            duration = 300
            interpolator = android.view.animation.DecelerateInterpolator()
        }
        
        android.transition.TransitionManager.beginDelayedTransition(binding.searchCard.parent as ViewGroup, transition)
        
        val lp = binding.searchCard.layoutParams as FrameLayout.LayoutParams
        lp.width = FrameLayout.LayoutParams.MATCH_PARENT
        lp.setMargins(dpToPx(20), 0, dpToPx(20), 0)
        binding.searchCard.layoutParams = lp

        val containerLp = binding.searchContainer.layoutParams
        containerLp.width = ViewGroup.LayoutParams.MATCH_PARENT
        binding.searchContainer.layoutParams = containerLp
        
        binding.tvSearchHint.visibility = View.GONE
        binding.etSettingsSearch.visibility = View.VISIBLE
        binding.etSettingsSearch.requestFocus()
        
        // Show keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.etSettingsSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun collapseSearch() {
        val transition = android.transition.TransitionSet().apply {
            addTransition(android.transition.ChangeBounds())
            addTransition(android.transition.Fade())
            duration = 250
            interpolator = android.view.animation.AccelerateInterpolator()
        }
        
        android.transition.TransitionManager.beginDelayedTransition(binding.searchCard.parent as ViewGroup, transition)
        
        binding.etSettingsSearch.setText("")
        binding.etSettingsSearch.visibility = View.GONE
        binding.btnClearSearch.visibility = View.GONE
        binding.tvSearchHint.visibility = View.VISIBLE
        binding.etSettingsSearch.clearFocus()
        
        val lp = binding.searchCard.layoutParams as FrameLayout.LayoutParams
        lp.width = FrameLayout.LayoutParams.WRAP_CONTENT
        lp.setMargins(0, 0, 0, 0)
        binding.searchCard.layoutParams = lp

        val containerLp = binding.searchContainer.layoutParams
        containerLp.width = ViewGroup.LayoutParams.WRAP_CONTENT
        binding.searchContainer.layoutParams = containerLp

        binding.layoutSearchEmpty.visibility = View.GONE
        binding.rvSettingsResults.visibility = View.GONE
        binding.settingsCategoriesContainer.visibility = View.VISIBLE

        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSettingsSearch.windowToken, 0)
    }

    private fun filterSettings(query: String) {
        val lowerQuery = query.lowercase().trim()
        
        binding.btnClearSearch.visibility = if (lowerQuery.isNotEmpty()) View.VISIBLE else View.GONE

        if (lowerQuery.isEmpty()) {
            binding.rvSettingsResults.visibility = View.GONE
            binding.layoutSearchEmpty.visibility = View.GONE
            binding.settingsCategoriesContainer.visibility = View.VISIBLE
            return
        }

        val filtered = allSettings.filter { item ->
            item.title.lowercase().contains(lowerQuery) ||
            item.description.lowercase().contains(lowerQuery) ||
            item.keywords.lowercase().contains(lowerQuery) ||
            item.category.lowercase().contains(lowerQuery)
        }

        binding.settingsCategoriesContainer.visibility = View.GONE
        if (filtered.isEmpty()) {
            binding.rvSettingsResults.visibility = View.GONE
            binding.layoutSearchEmpty.visibility = View.VISIBLE
        } else {
            binding.layoutSearchEmpty.visibility = View.GONE
            binding.rvSettingsResults.visibility = View.VISIBLE
            searchAdapter.submitList(filtered)
        }
    }

    private fun applyGlobalRefresh() {
        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_REFRESH
        }
        startService(intent)
    }

    // --- Modernized Search Adapter ---
    private inner class SearchResultsAdapter(private val onClick: (SettingItem, View) -> Unit) : 
        ListAdapter<SettingItem, SearchResultsAdapter.ViewHolder>(DiffCallback) {

        inner class ViewHolder(private val itemBinding: ItemSearchResultBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(item: SettingItem) {
                itemBinding.tvSearchItemTitle.text = item.title
                itemBinding.tvSearchItemDesc.text = item.description
                itemBinding.tvSearchItemCategory.text = item.category

                // Vibrant tonal colors matching Material 3 Security & Settings design
                val (textColor, bgColor) = when (item.category) {
                    getString(R.string.section_appearance) -> Color.parseColor("#3B82F6") to Color.parseColor("#1F3B82F6")
                    getString(R.string.section_interaction) -> Color.parseColor("#8B5CF6") to Color.parseColor("#1F8B5CF6")
                    getString(R.string.section_handle) -> Color.parseColor("#10B981") to Color.parseColor("#1F10B981")
                    getString(R.string.section_tools) -> Color.parseColor("#F59E0B") to Color.parseColor("#1FF59E0B")
                    getString(R.string.permissions_dashboard_title) -> Color.parseColor("#06B6D4") to Color.parseColor("#1F06B6D4")
                    else -> Color.parseColor("#EC4899") to Color.parseColor("#1FEC4899")
                }
                itemBinding.tvSearchItemCategory.setTextColor(textColor)
                itemBinding.tvSearchItemCategory.backgroundTintList = ColorStateList.valueOf(bgColor)

                itemBinding.root.setOnClickListener { onClick(item, itemBinding.root) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SettingItem>() {
        override fun areItemsTheSame(oldItem: SettingItem, newItem: SettingItem) = oldItem.title == newItem.title
        override fun areContentsTheSame(oldItem: SettingItem, newItem: SettingItem) = oldItem == newItem
    }
}
