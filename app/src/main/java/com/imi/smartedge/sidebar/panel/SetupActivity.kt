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

    private lateinit var binding: ActivitySetupBinding
    private lateinit var panelPrefs: PanelPreferences
    private var hasInteractedWithAutoStart = false

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

        // Set status bar color to match background
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window.statusBarColor = typedValue.data
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        binding.cardOverlay.setOnClickListener { requestOverlayPermission() }
        binding.cardAccessibility.setOnClickListener { requestAccessibilityPermission() }
        binding.cardBattery.setOnClickListener { requestIgnoreBatteryOptimization() }
        binding.cardAutoStart.setOnClickListener { 
            hasInteractedWithAutoStart = true
            requestAutoStartPermission() 
        }
        
        binding.btnGrantAll.setOnClickListener {
            if (!hasOverlayPermission()) {
                requestOverlayPermission()
            } else if (!isAccessibilityServiceEnabled()) {
                requestAccessibilityPermission()
            } else if (!isIgnoringBatteryOptimizations()) {
                requestIgnoreBatteryOptimization()
            } else if (!hasInteractedWithAutoStart) {
                hasInteractedWithAutoStart = true
                requestAutoStartPermission()
            }
        }

        binding.btnContinue.setOnClickListener {
            panelPrefs.setupCompleted = true
            startActivity(Intent(this, MainActivity::class.java))
            finish()
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

        updateCardState(binding.cardOverlay, binding.actionOverlay, hasOverlay)
        updateCardState(binding.cardAccessibility, binding.actionAccessibility, hasAccessibility)
        updateCardState(binding.cardBattery, binding.actionBattery, hasBattery)
        
        // Auto-start is hard to detect on most OEMs, but we can detect on MIUI
        updateCardState(binding.cardAutoStart, binding.actionAutoStart, hasAutoStart)

        val requiredGranted = hasOverlay && hasAccessibility
        binding.btnContinue.isEnabled = requiredGranted
        
        if (requiredGranted) {
            binding.btnContinue.alpha = 1.0f
            // Set to solid VIBRANT green color when enabled
            binding.btnContinue.setBackgroundColor(android.graphics.Color.parseColor("#00FF00"))
            binding.btnContinue.setTextColor(android.graphics.Color.BLACK) // Black text for better contrast on neon green
        } else {
            binding.btnContinue.alpha = 0.3f // \"Blurred\" / Faded effect
        }
        
        val allGranted = requiredGranted && hasBattery && hasAutoStart
        binding.btnGrantAll.isEnabled = !allGranted
        
        if (allGranted) {
            binding.btnGrantAll.text = "All granted"
            binding.btnGrantAll.alpha = 0.5f
        } else {
            binding.btnGrantAll.text = "Grant all"
            binding.btnGrantAll.alpha = 1.0f
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
            card.isClickable = card.id == binding.cardAutoStart.id 
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

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals("$packageName/${PanelAccessibilityService::class.java.name}", ignoreCase = true)) {
                return true
            }
        }
        return false
    }

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
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
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
