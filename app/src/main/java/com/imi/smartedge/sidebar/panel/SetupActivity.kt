package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.imi.smartedge.sidebar.panel.databinding.ActivitySetupBinding

class SetupActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private lateinit var binding: ActivitySetupBinding
    private lateinit var panelPrefs: PanelPreferences
    private var hasInteractedWithAutoStart = false

    companion object {
        const val EXTRA_MANAGE_MODE = "extra_manage_mode"
    }

    private var isManageMode = false

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        panelPrefs = PanelPreferences(this)

        supportActionBar?.hide()

        isManageMode = intent.getBooleanExtra(EXTRA_MANAGE_MODE, false)
        if (isManageMode) {
            binding.btnBack.visibility = android.view.View.VISIBLE
            binding.btnBack.setOnClickListener { finish() }
            binding.tvWelcomeTitle.text = getString(R.string.setup_manage_title)
            binding.tvWelcomeSubtitle.text = getString(R.string.setup_manage_subtitle)
            binding.btnContinue.text = getString(R.string.setup_done)
            binding.btnContinue.setOnClickListener { finish() }
        } else {
            binding.btnContinue.setOnClickListener {
                panelPrefs.setupCompleted = true
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

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
        
        binding.btnGrantAll.setOnClickListener {
            if (!hasOverlayPermission()) {
                requestOverlayPermission()
            } else if (!isAccessibilityServiceEnabled()) {
                requestAccessibilityPermission()
            } else if (!isNotificationAccessEnabled()) {
                requestNotificationAccess()
            } else if (!isIgnoringBatteryOptimizations()) {
                requestIgnoreBatteryOptimization()
            } else if (!hasInteractedWithAutoStart) {
                hasInteractedWithAutoStart = true
                requestAutoStartPermission()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val hasOverlay = hasOverlayPermission()
        val hasAccessibility = isAccessibilityServiceEnabled()
        val hasBattery = isIgnoringBatteryOptimizations()
        val hasAutoStart = hasAutoStartPermission()
        val hasNotifications = isNotificationAccessEnabled()
        val hasAutomation = panelPrefs.useAutomationForGestures && AutomationManager.isAutomationPossible()

        updateCardState(binding.cardOverlay, binding.actionOverlay, hasOverlay)
        updateCardState(binding.cardAccessibility, binding.actionAccessibility, hasAccessibility)
        updateCardState(binding.cardBattery, binding.actionBattery, hasBattery)
        updateCardState(binding.cardNotifications, binding.actionNotifications, hasNotifications)
        
        // Update Native Gesture Label with status
        val status = when {
            AutomationManager.isRootAvailable() -> " (Root)"
            AutomationManager.isShizukuAvailable() -> " (Shizuku)"
            else -> ""
        }
        binding.titleAutomation.text = "${getString(R.string.setup_automation_title)}$status"
        updateCardState(binding.cardAutomation, binding.actionAutomation, hasAutomation)
        
        // Auto-start is hard to detect on most OEMs, but we can detect on MIUI
        updateCardState(binding.cardAutoStart, binding.actionAutoStart, hasAutoStart)

        val requiredGranted = hasOverlay && (hasAccessibility || hasAutomation)
        binding.btnContinue.isEnabled = if (isManageMode) true else requiredGranted
        
        val typedValue = android.util.TypedValue()
        if (isManageMode || requiredGranted) {
            binding.btnContinue.alpha = 1.0f
            // Use Primary color when enabled instead of hardcoded neon green
            theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
            binding.btnContinue.setBackgroundColor(typedValue.data)
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true)
            binding.btnContinue.setTextColor(typedValue.data)
        } else {
            binding.btnContinue.alpha = 0.3f // "Blurred" / Faded effect
        }
        
        val allGranted = requiredGranted && hasBattery && hasAutoStart && hasNotifications
        binding.btnGrantAll.isEnabled = !allGranted
        
        if (allGranted) {
            binding.btnGrantAll.text = getString(R.string.setup_all_granted)
            binding.btnGrantAll.alpha = 0.5f
        } else {
            binding.btnGrantAll.text = getString(R.string.setup_grant_all)
            binding.btnGrantAll.alpha = 1.0f
        }
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val cn = android.content.ComponentName.unflattenFromString(name)
                if (cn != null) {
                    if (android.text.TextUtils.equals(pkgName, cn.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun requestNotificationAccess() {
        try {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun hasAutoStartPermission(): Boolean {
        return when {
            MIUIUtils.isMIUI() -> MIUIUtils.isAutoStartEnabled(this)
            VivoUtils.isVivo() -> VivoUtils.isAutoStartEnabled(this)
            else -> hasInteractedWithAutoStart
        }
    }

    private fun updateCardState(
        card: com.google.android.material.card.MaterialCardView,
        action: android.widget.ImageView,
        isGranted: Boolean
    ) {
        val typedValue = android.util.TypedValue()
        
        // Find the title and description to dim them individually instead of the whole card
        val container = card.getChildAt(0) as? android.view.ViewGroup
        val textContainer = container?.getChildAt(1) as? android.view.ViewGroup
        val title = textContainer?.getChildAt(0) as? android.widget.TextView
        val desc = textContainer?.getChildAt(1) as? android.widget.TextView
        
        if (isGranted) {
            theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHigh, typedValue, true)
            card.setCardBackgroundColor(typedValue.data)
            
            action.setImageResource(android.R.drawable.checkbox_on_background)
            action.imageTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#00FF00") // ELECTRIC GREEN
            )
            // Allow clicking to toggle even when granted for internal preferences
            card.isClickable = card.id == binding.cardAutoStart.id || 
                             card.id == binding.cardAutomation.id
            card.alpha = 1.0f // Keep full opacity for the card
            title?.alpha = 1.0f // Set to normal
            desc?.alpha = 1.0f  // Set to normal
        } else {
            theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHigh, typedValue, true)
            card.setCardBackgroundColor(typedValue.data)

            action.setImageResource(R.drawable.ic_chevron_right)
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)
            action.imageTintList = android.content.res.ColorStateList.valueOf(typedValue.data)
            card.isClickable = true
            card.alpha = 1.0f
            title?.alpha = 1.0f
            desc?.alpha = 1.0f
        }
    }

    private fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(this)

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
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
            // Fallback for other OEMs
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e: Exception) {}
        }
    }
}
