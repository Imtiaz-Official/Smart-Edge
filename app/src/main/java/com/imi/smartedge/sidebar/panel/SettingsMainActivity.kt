package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsMainBinding
import com.google.android.material.snackbar.Snackbar

class SettingsMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsMainBinding
    private lateinit var panelPrefs: PanelPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color and icons
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window.statusBarColor = typedValue.data
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        panelPrefs = PanelPreferences(this)
        setupListeners()
    }

    override fun onSupportNavigateUp(): Boolean {
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
                .setTitle("Reset All Settings?")
                .setMessage("This will restore all configurations to their original defaults. Your added apps will NOT be removed.")
                .setPositiveButton("Reset") { _, _ ->
                    panelPrefs.resetToDefaults()
                    applyGlobalRefresh()
                    binding.root.showModernToast("Settings Reset Successfully")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun applyGlobalRefresh() {
        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_REFRESH
        }
        startService(intent)
    }
}
