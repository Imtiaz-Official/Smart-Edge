package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsToolsBinding
// import rikka.shizuku.Shizuku
import android.content.pm.PackageManager

class ToolsSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsToolsBinding
    private lateinit var panelPrefs: PanelPreferences
    // private val SHIZUKU_CODE = 1001

    /*
    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                binding.root.showModernToast("Shizuku permission granted!")
            } else {
                binding.root.showModernToast("Shizuku permission denied")
            }
            updateShizukuStatus()
        }
    }
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsToolsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)

        // Set status bar color and icons
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window.statusBarColor = typedValue.data
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        panelPrefs = PanelPreferences(this)
        
        loadCurrentSettings()
        setupListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }

    private fun loadCurrentSettings() {
        binding.switchTools.isChecked = panelPrefs.showTools
        binding.layoutToolsSubOptions.alpha = if (panelPrefs.showTools) 1.0f else 0.5f
        binding.layoutToolsSubOptions.isEnabled = panelPrefs.showTools
        binding.divTools.visibility = if (panelPrefs.showTools) View.VISIBLE else View.GONE

        binding.switchSysInfo.isChecked = panelPrefs.showSysInfo
        binding.switchPowerMenu.isChecked = panelPrefs.showPowerMenu

        // updateShizukuStatus()
    }

    /*
    private fun updateShizukuStatus() {
        val available = ShizukuHelper.isShizukuAvailable()
        val hasPermission = ShizukuHelper.hasShizukuPermission()

        if (!available) {
            binding.tvShizukuStatus.text = "Service Not Running"
            binding.tvShizukuStatus.setTextColor(Color.RED)
            binding.btnLinkShizuku.text = "How to Start Shizuku"
            binding.btnLinkShizuku.isEnabled = true
        } else if (!hasPermission) {
            binding.tvShizukuStatus.text = "Service Running (No Permission)"
            binding.tvShizukuStatus.setTextColor(Color.parseColor("#FFA500"))
            binding.btnLinkShizuku.text = "Authorize Shizuku"
            binding.btnLinkShizuku.isEnabled = true
        } else {
            binding.tvShizukuStatus.text = "Service Running & Authorized"
            binding.tvShizukuStatus.setTextColor(Color.parseColor("#4CAF50"))
            binding.btnLinkShizuku.text = "Shizuku Ready"
            binding.btnLinkShizuku.isEnabled = false
        }
    }
    */

    private fun setupListeners() {
        binding.switchTools.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showTools = isChecked
            binding.layoutToolsSubOptions.alpha = if (isChecked) 1.0f else 0.5f
            binding.layoutToolsSubOptions.isEnabled = isChecked
            binding.divTools.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyOnly()
        }

        binding.switchSysInfo.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showSysInfo = isChecked
            applyOnly()
        }

        binding.switchPowerMenu.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showPowerMenu = isChecked
            applyOnly()
        }

        /*
        binding.btnLinkShizuku.setOnClickListener {
            if (!ShizukuHelper.isShizukuAvailable()) {
                try {
                    val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://shizuku.rikka.app/download/"))
                        startActivity(browserIntent)
                    }
                } catch (e: Exception) {
                    binding.root.showModernToast("Could not open Shizuku")
                }
            } else if (!ShizukuHelper.hasShizukuPermission()) {
                ShizukuHelper.requestShizukuPermission(SHIZUKU_CODE)
            }
        }
        */
    }

    private fun applyOnly() {
        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_REFRESH
        }
        startService(intent)
    }
}
