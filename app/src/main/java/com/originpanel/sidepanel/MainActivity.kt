package com.originpanel.sidepanel

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import android.widget.Toast
import com.originpanel.sidepanel.databinding.ActivityMainM3Binding

/**
 * Entry-point activity.
 *
 * Responsibilities:
 * 1. Check SYSTEM_ALERT_WINDOW permission — guide user if not granted
 * 2. Start / Stop FloatingPanelService
 * 3. Launch AppPickerActivity and SettingsActivity
 * 4. Show Android 13 sideload restriction notice if relevant
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainM3Binding
    private var isPanelRunning = false

    // Launcher for overlay permission settings screen
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Re-check permission when user returns from settings
        updatePermissionUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainM3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide default action bar — our layout provides the header
        supportActionBar?.hide()

        binding.btnGrantPermission.setOnClickListener { requestOverlayPermission() }
        binding.btnIgnoreBattery.setOnClickListener { requestIgnoreBatteryOptimization() }
        binding.btnStartStop.setOnClickListener { togglePanel() }
        binding.btnTogglePanel.setOnClickListener { triggerPanelToggle() }
        binding.btnManageApps.setOnClickListener {
            if (!hasOverlayPermission()) {
                Toast.makeText(this, "Please grant 'Display over other apps' permission first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Please enable 'SidePanel' in Accessibility Settings", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }
            sendBroadcast(Intent("com.originpanel.sidepanel.ACTION_OPEN_PICKER"))
            Toast.makeText(this, "Opening panel editor...", Toast.LENGTH_SHORT).show()
            // Close MainActivity so user sees the panel picker immediately
            finish()
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Load Tutorial Animation
        Glide.with(this)
            .asGif()
            .load(R.drawable.tutorial_anim)
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
            .into(binding.ivTutorialAnim)
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUI()
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        val isRunning = FloatingPanelService.isRunning
        isPanelRunning = isRunning
        
        binding.btnStartStop.text = if (isRunning) "Stop" else "Start"
        binding.tvStatus.text = if (isRunning) "Service is Active" else "Service is Stopped"
        binding.tvStatus.setTextColor(if (isRunning) Color.parseColor("#00C853") else Color.parseColor("#80FFFFFF"))
        
        val dot = findViewById<android.widget.ImageView>(R.id.statusDot)
        dot?.imageTintList = android.content.res.ColorStateList.valueOf(
            if (isRunning) Color.parseColor("#00C853") else Color.parseColor("#FF5252")
        )
    }

    // ── Permission ────────────────────────────────────────────────────────────

    private fun hasOverlayPermission(): Boolean =
        Settings.canDrawOverlays(this)

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestIgnoreBatteryOptimization() {
        // First, attempt the standard Android way
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e2: Exception) {}
        }

        // For OriginOS / Vivo / iQOO devices, the standard way often isn't enough.
        // We provide a small delay and then attempt to open their specific "High Power Consumption" or "BgStartUp" manager.
        val manufacturer = Build.MANUFACTURER.lowercase()
        if (manufacturer.contains("vivo") || manufacturer.contains("iqoo")) {
            binding.root.postDelayed({
                openVivoSpecificSettings()
            }, 1000)
        }
    }

    private fun openVivoSpecificSettings() {
        val intents = arrayOf(
            Intent().apply { setClassName("com.vivo.abe", "com.vivo.abe.unifiedpower.HighPowerConsumptionActivity") },
            Intent().apply { setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity") },
            Intent().apply { setClassName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager") },
            Intent().apply { setClassName("com.vivo.abe", "com.vivo.abe.unifiedpower.UnifiedPowerActivity") }
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return // Success
            } catch (e: Exception) {
                // Try next one
            }
        }
    }

    private fun updatePermissionUI() {
        val granted = hasOverlayPermission()
        val standardBatteryIgnored = isIgnoringBatteryOptimizations()
        
        // On Vivo/iQOO, the standard check isn't enough to guarantee stability.
        // We keep the card visible on these devices even if standard optimization is "ignored"
        // so they can access the deep-link to "High Background Power Consumption".
        val manufacturer = Build.MANUFACTURER.lowercase()
        val isVivo = manufacturer.contains("vivo") || manufacturer.contains("iqoo")
        val batteryCardVisible = !standardBatteryIgnored || isVivo

        binding.cardPermission.visibility = if (granted) View.GONE else View.VISIBLE
        binding.cardBatteryOptimization.visibility = if (batteryCardVisible) View.VISIBLE else View.GONE
        
        binding.btnStartStop.isEnabled = granted

        // Show Android 13 sideload note for users who can't find the permission toggle
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.tvSideloadNote.visibility = View.VISIBLE
        }
    }

    // ── Service Toggle ────────────────────────────────────────────────────────

    private fun togglePanel() {
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
            return
        }

        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Please enable 'SidePanel' in Accessibility Settings", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }

        if (isPanelRunning) {
            stopPanel()
        } else {
            startPanel()
        }
    }

    private fun startPanel() {
        // Optimistic UI: flip state immediately for speed
        binding.btnStartStop.text = "Stop"
        binding.tvStatus.text = "Service is Active"
        binding.tvStatus.setTextColor(Color.parseColor("#00C853"))
        val dot = findViewById<android.widget.ImageView>(R.id.statusDot)
        dot?.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#00C853"))

        val intent = Intent(this, FloatingPanelService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        // Final sync after a tiny bit just to be sure
        binding.root.postDelayed({ updateServiceStatus() }, 200)
    }

    private fun stopPanel() {
        // Optimistic UI: flip state immediately for speed
        binding.btnStartStop.text = "Start"
        binding.tvStatus.text = "Service is Stopped"
        binding.tvStatus.setTextColor(Color.parseColor("#80FFFFFF"))
        val dot = findViewById<android.widget.ImageView>(R.id.statusDot)
        dot?.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5252"))

        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_STOP
        }
        startService(intent)
        
        // Final sync after a tiny bit just to be sure
        binding.root.postDelayed({ updateServiceStatus() }, 200)
    }

    private fun triggerPanelToggle() {
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
            return
        }
        
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Please enable 'SidePanel' in Accessibility Settings", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        
        // Ensure service is started first
        if (!isPanelRunning) {
            startPanel()
        }

        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_OPEN
        }
        startForegroundService(intent)
    }

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
}
