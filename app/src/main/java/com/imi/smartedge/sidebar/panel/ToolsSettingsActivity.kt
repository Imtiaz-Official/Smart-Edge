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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsToolsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        handleDeepLink()
    }

    private fun handleDeepLink() {
        val targetId = intent.getStringExtra(SettingsMainActivity.EXTRA_SCROLL_TO) ?: return
        val viewId = resources.getIdentifier(targetId, "id", packageName)
        if (viewId != 0) {
            val targetView = findViewById<View>(viewId)
            targetView?.post {
                val rect = android.graphics.Rect()
                targetView.getDrawingRect(rect)
                binding.root.offsetDescendantRectToMyCoords(targetView, rect)
                binding.toolsScrollView.smoothScrollTo(0, rect.top - 200)
                targetView.highlightView()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }

    private fun loadCurrentSettings() {
        binding.featureToolsMaster.isChecked = panelPrefs.showTools
        binding.layoutToolsSubOptions.alpha = if (panelPrefs.showTools) 1.0f else 0.5f
        binding.layoutToolsSubOptions.isEnabled = panelPrefs.showTools
        binding.divTools.visibility = if (panelPrefs.showTools) View.VISIBLE else View.GONE

        binding.featureSysInfo.isChecked = panelPrefs.showSysInfo
        binding.featurePowerMenu.isChecked = panelPrefs.showPowerMenu
    }

    private fun setupListeners() {
        binding.featureToolsMaster.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showTools = isChecked
            binding.layoutToolsSubOptions.alpha = if (isChecked) 1.0f else 0.5f
            binding.layoutToolsSubOptions.isEnabled = isChecked
            binding.divTools.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyOnly()
        }

        binding.featureSysInfo.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showSysInfo = isChecked
            applyOnly()
        }

        binding.featurePowerMenu.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showPowerMenu = isChecked
            applyOnly()
        }
    }

    private fun applyOnly() {
        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_REFRESH
        }
        startService(intent)
    }
}
