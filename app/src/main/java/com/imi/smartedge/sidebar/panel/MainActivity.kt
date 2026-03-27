package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.imi.smartedge.sidebar.panel.databinding.ActivityMainM3Binding

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
    private lateinit var panelPrefs: PanelPreferences
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
        panelPrefs = PanelPreferences(this)
        
        android.util.Log.d("SmartEdge", "MainActivity onCreate: setupCompleted = ${panelPrefs.setupCompleted}")

        if (!panelPrefs.setupCompleted) {
            android.util.Log.d("SmartEdge", "Launching SetupActivity...")
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainM3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color and icons
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window.statusBarColor = typedValue.data
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        // Hide default action bar — our layout provides the header
        supportActionBar?.hide()

        binding.btnGrantPermission.setOnClickListener { requestOverlayPermission() }
        binding.btnIgnoreBattery.setOnClickListener { requestIgnoreBatteryOptimization() }
        binding.btnStartStop.setOnClickListener { togglePanel() }
        binding.btnTogglePanel.setOnClickListener { triggerPanelToggle() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnShowLogs.setOnClickListener {
            showLogsDialog()
        }

        binding.btnHowToUse.setOnClickListener {
            val isVisible = binding.layoutTutorialContent.visibility == View.VISIBLE
            if (isVisible) {
                binding.layoutTutorialContent.visibility = View.GONE
            } else {
                binding.layoutTutorialContent.visibility = View.VISIBLE
                binding.mainScrollView.post {
                    binding.mainScrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
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

    private fun showLogsDialog() {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val time = sdf.format(java.util.Date())
        
        val logBuilder = StringBuilder()
        logBuilder.append("[$time] Log Session Started\n")
        logBuilder.append("[$time] Device: ${android.os.Build.MODEL} (${android.os.Build.VERSION.RELEASE})\n")
        logBuilder.append("[$time] Overlay: ${if (hasOverlayPermission()) "GRANTED" else "MISSING"}\n")
        logBuilder.append("[$time] Accessibility: ${if (isAccessibilityServiceEnabled()) "ACTIVE" else "INACTIVE"}\n")
        logBuilder.append("[$time] Service: ${if (FloatingPanelService.isRunning) "RUNNING" else "STOPPED"}\n")
        logBuilder.append("[$time] Theme: ${panelPrefs.uiTheme.uppercase()}\n")
        logBuilder.append("[$time] Status: FULL VERSION (Open Source)\n")
        logBuilder.append("[$time] --- End of Summary ---")

        val tv = TextView(this).apply {
            text = logBuilder.toString()
            setPadding(64, 32, 64, 32)
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 12f // 12f is the float for sp
            setTextColor(Color.parseColor("#B3FFFFFF"))
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val scroll = android.widget.ScrollView(this).apply {
            addView(tv)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("System Activity Logs")
            .setView(scroll)
            .setPositiveButton("Close", null)
            .show()
    }

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
        
        // Show/Hide Activity Logs button based on user preference
        binding.btnShowLogs.visibility = if (panelPrefs.showLogs) View.VISIBLE else View.GONE

        // For Vivo/iQOO, we want to keep the card visible until standard optimization is ignored,
        // so the user can easily reach the deep-links. Once ignored, it disappears.
        val batteryCardVisible = !standardBatteryIgnored

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
            binding.root.showModernToast("Please enable 'SidePanel' in Accessibility Settings", Snackbar.LENGTH_LONG)
            openAccessibilitySettings()
            return
        }

        if (isPanelRunning) {
            stopPanel()
        } else {
            startPanel()
        }
    }

    private fun startPanel() {
        panelPrefs.serviceEnabled = true
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
        panelPrefs.serviceEnabled = false
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
            binding.root.showModernToast("Please enable 'SidePanel' in Accessibility Settings", Snackbar.LENGTH_LONG)
            openAccessibilitySettings()
            return
        }
        
        // Ensure service is started first
        if (!isPanelRunning) {
            startPanel()
        }

        binding.root.showModernToast("Opening Sidebar...")
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
