package com.originpanel.sidepanel

import android.content.Intent
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
import com.originpanel.sidepanel.databinding.ActivityMainBinding

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

    private lateinit var binding: ActivityMainBinding
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide default action bar — our layout provides the header
        supportActionBar?.hide()

        binding.btnGrantPermission.setOnClickListener { requestOverlayPermission() }
        binding.btnStartStop.setOnClickListener { togglePanel() }
        binding.btnTogglePanel.setOnClickListener { triggerPanelToggle() }
        binding.btnManageApps.setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
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

    private fun updatePermissionUI() {
        val granted = hasOverlayPermission()

        binding.cardPermission.visibility = if (granted) View.GONE else View.VISIBLE
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

        if (isPanelRunning) {
            stopPanel()
        } else {
            startPanel()
        }
    }

    private fun startPanel() {
        val intent = Intent(this, FloatingPanelService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isPanelRunning = true
        binding.btnStartStop.text = getString(R.string.stop_panel)
        binding.tvStatus.text = "✓  Panel is active — check your screen edge"
    }

    private fun stopPanel() {
        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_STOP
        }
        startService(intent)
        isPanelRunning = false
        binding.btnStartStop.text = getString(R.string.start_panel)
        binding.tvStatus.text = "Panel is stopped"
    }

    private fun triggerPanelToggle() {
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
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
}
