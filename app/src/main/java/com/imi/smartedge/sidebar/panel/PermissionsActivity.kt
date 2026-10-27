package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.imi.smartedge.sidebar.panel.databinding.ActivityPermissionsBinding

class PermissionsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private lateinit var binding: ActivityPermissionsBinding
    private lateinit var panelPrefs: PanelPreferences
    private var hasInteractedWithAutoStart = false

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        panelPrefs = PanelPreferences(this)

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun setupListeners() {
        binding.cardOverlay.setOnClickListener { requestOverlayPermission() }
        binding.cardAccessibility.setOnClickListener { requestAccessibilityPermission() }
        binding.cardBattery.setOnClickListener { requestIgnoreBatteryOptimization() }
        binding.cardAutoStart.setOnClickListener {
            hasInteractedWithAutoStart = true
            requestAutoStartPermission()
        }
        binding.cardNotifications.setOnClickListener { requestNotificationAccess() }

        binding.cardAutomation.setOnClickListener {
            AutomationManager.checkRootAndRequestPermission(this) { success ->
                runOnUiThread {
                    if (success) {
                        panelPrefs.useAutomationForGestures = true
                        updateUI()
                    } else {
                        SecureSettingsDialog.show(this) {
                            updateUI()
                        }
                    }
                }
            }
        }

        binding.btnFixAll.setOnClickListener {
            if (!hasOverlayPermission()) {
                requestOverlayPermission()
            } else if (!isAccessibilityServiceEnabled()) {
                requestAccessibilityPermission()
            } else if (!isIgnoringBatteryOptimizations()) {
                requestIgnoreBatteryOptimization()
            } else if (!hasInteractedWithAutoStart) {
                hasInteractedWithAutoStart = true
                requestAutoStartPermission()
            } else if (!isNotificationAccessEnabled()) {
                requestNotificationAccess()
            }
        }
    }

    private fun updateUI() {
        val hasOverlay = hasOverlayPermission()
        val hasAccessibility = isAccessibilityServiceEnabled()
        val hasBattery = isIgnoringBatteryOptimizations()
        val hasAutoStart = hasAutoStartPermission()
        val hasNotifications = isNotificationAccessEnabled()
        val hasAutomation = panelPrefs.useAutomationForGestures && AutomationManager.isAutomationPossible()

        // 1. Overlay Badge
        setBadgeState(
            binding.badgeOverlay,
            isGranted = hasOverlay,
            grantedText = getString(R.string.perm_status_active),
            missingText = getString(R.string.perm_btn_grant)
        )

        // 2. Accessibility Badge
        setBadgeState(
            binding.badgeAccessibility,
            isGranted = hasAccessibility,
            grantedText = getString(R.string.perm_status_active),
            missingText = getString(R.string.perm_btn_grant)
        )

        // 3. Battery Badge
        setBadgeState(
            binding.badgeBattery,
            isGranted = hasBattery,
            grantedText = getString(R.string.perm_status_unrestricted),
            missingText = getString(R.string.perm_status_optimized)
        )

        // 4. AutoStart Badge
        setBadgeState(
            binding.badgeAutoStart,
            isGranted = hasAutoStart,
            grantedText = getString(R.string.perm_status_configured),
            missingText = getString(R.string.perm_btn_manage)
        )

        // 5. Notifications Badge
        setBadgeState(
            binding.badgeNotifications,
            isGranted = hasNotifications,
            grantedText = getString(R.string.perm_status_active),
            missingText = getString(R.string.perm_status_optional)
        )

        // 6. Automation Badge
        val autoStatusText = when {
            AutomationManager.isRootAvailable() && panelPrefs.useAutomationForGestures -> "Root Active"
            AutomationManager.isShizukuAvailable() && panelPrefs.useAutomationForGestures -> "Shizuku Active"
            else -> getString(R.string.perm_btn_configure)
        }
        setBadgeState(
            binding.badgeAutomation,
            isGranted = hasAutomation,
            grantedText = autoStatusText,
            missingText = autoStatusText
        )

        // Health Banner State
        val coreGranted = hasOverlay && (hasAccessibility || hasAutomation)
        val allGood = coreGranted && hasBattery

        if (allGood) {
            binding.ivHealthIcon.setImageResource(R.drawable.ic_shield_check)
            binding.ivHealthIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#2ECC71"))
            binding.tvHealthTitle.text = getString(R.string.permissions_status_all_good_title)
            binding.tvHealthDesc.text = getString(R.string.permissions_status_all_good_desc)
            binding.btnFixAll.isEnabled = false
            binding.btnFixAll.text = getString(R.string.perm_btn_all_good)
            binding.btnFixAll.alpha = 0.5f
        } else {
            binding.ivHealthIcon.setImageResource(android.R.drawable.stat_notify_error)
            binding.ivHealthIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#F1C40F"))
            binding.tvHealthTitle.text = getString(R.string.permissions_status_warning_title)
            binding.tvHealthDesc.text = getString(R.string.permissions_status_warning_desc)
            binding.btnFixAll.isEnabled = true
            binding.btnFixAll.text = getString(R.string.perm_btn_fix_all)
            binding.btnFixAll.alpha = 1.0f
        }
    }

    private fun setBadgeState(badge: TextView, isGranted: Boolean, grantedText: String, missingText: String) {
        val typedValue = TypedValue()
        if (isGranted) {
            badge.text = grantedText
            badge.setTextColor(Color.parseColor("#2ECC71"))
            badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1A2ECC71"))
        } else {
            badge.text = missingText
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)
            badge.setTextColor(typedValue.data)
            theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHigh, typedValue, true)
            badge.backgroundTintList = ColorStateList.valueOf(typedValue.data)
        }
    }

    private fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(this)

    private fun isAccessibilityServiceEnabled(): Boolean {
        if (PanelAccessibilityService.isRunning) return true
        val serviceId = "$packageName/${PanelAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(":").any { it.equals(serviceId, ignoreCase = true) }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun hasAutoStartPermission(): Boolean {
        return when {
            MIUIUtils.isMIUI() -> MIUIUtils.isAutoStartEnabled(this)
            VivoUtils.isVivo() -> VivoUtils.isAutoStartEnabled(this)
            else -> hasInteractedWithAutoStart
        }
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val cn = android.content.ComponentName.unflattenFromString(name)
                if (cn != null && android.text.TextUtils.equals(packageName, cn.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun requestAccessibilityPermission() {
        AccessibilityGuideDialog.newInstance()
            .show(supportFragmentManager, AccessibilityGuideDialog.TAG)
    }

    private fun requestIgnoreBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e2: Exception) {}
        }
    }

    private fun requestAutoStartPermission() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intent = Intent()
        var found = false

        when {
            manufacturer.contains("xiaomi") -> {
                intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                found = true
            }
            manufacturer.contains("oppo") -> {
                intent.setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                found = true
            }
            manufacturer.contains("vivo") -> {
                intent.setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                found = true
            }
            manufacturer.contains("samsung") -> {
                intent.setClassName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
                found = true
            }
        }

        val appName = getString(R.string.app_name)
        val uri = Uri.parse("package:$packageName")
        intent.data = uri
        intent.putExtra("package_name", packageName)
        intent.putExtra("packageName", packageName)
        intent.putExtra("package", packageName)
        intent.putExtra("pkg", packageName)
        intent.putExtra("app_package", packageName)
        intent.putExtra("android.provider.extra.APP_PACKAGE", packageName)
        intent.putExtra("app_label", appName)
        intent.putExtra("app_name", appName)
        intent.putExtra("title", appName)
        intent.putExtra(":settings:fragment_args_key", packageName)
        val argsBundle = Bundle().apply {
            putString(":settings:fragment_args_key", packageName)
        }
        intent.putExtra(":settings:show_fragment_args", argsBundle)
        intent.putExtra("EXTRA_FRAGMENT_ARG_KEY", packageName)
        intent.putExtra("EXTRA_SHOW_FRAGMENT_ARGUMENTS", argsBundle)
        intent.putExtra("highlight_key", packageName)
        intent.putExtra("highlight_item", packageName)

        if (found) {
            try {
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (e2: Exception) {}
            }
        } else {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e: Exception) {}
        }
    }

    private fun requestNotificationAccess() {
        try {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e2: Exception) {}
        }
    }
}
