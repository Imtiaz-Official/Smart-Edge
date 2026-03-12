package com.originpanel.sidepanel

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.originpanel.sidepanel.databinding.ActivitySettingsBinding

/**
 * Settings screen for panel configuration.
 * Changes take effect on next panel restart.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var panelPrefs: PanelPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = getString(R.string.settings_title)
            setDisplayHomeAsUpEnabled(true)
        }

        panelPrefs = PanelPreferences(this)
        loadCurrentSettings()
        setupListeners()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun loadCurrentSettings() {
        // Panel side
        if (panelPrefs.panelSide == PanelPreferences.SIDE_LEFT) {
            binding.rgPanelSide.check(R.id.rbLeft)
        } else {
            binding.rgPanelSide.check(R.id.rbRight)
        }

        // Auto-start
        binding.switchAutoStart.isChecked = panelPrefs.autoStart

        // Show Pill
        binding.switchShowPill.isChecked = panelPrefs.showPill

        // Haptic Feedback
        binding.switchHaptic.isChecked = panelPrefs.hapticEnabled

        // Opacity
        binding.sbOpacity.progress = panelPrefs.panelOpacity

        // Handle Height
        binding.sbHandleHeight.progress = panelPrefs.handleHeight

        // Handle Width
        binding.sbHandleWidth.progress = panelPrefs.handleWidth
    }

    private fun setupListeners() {
        binding.rgPanelSide.setOnCheckedChangeListener { _, checkedId ->
            panelPrefs.panelSide = if (checkedId == R.id.rbLeft)
                PanelPreferences.SIDE_LEFT else PanelPreferences.SIDE_RIGHT

            // Restart service so edge strip repositions immediately
            restartServiceIfRunning()
        }

        binding.switchAutoStart.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.autoStart = isChecked
        }

        binding.switchShowPill.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showPill = isChecked
            restartServiceIfRunning()
        }

        binding.switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.hapticEnabled = isChecked
        }

        binding.sbOpacity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) panelPrefs.panelOpacity = progress
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                restartServiceIfRunning()
            }
        })

        binding.sbHandleHeight.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) panelPrefs.handleHeight = progress
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                restartServiceIfRunning()
            }
        })

        binding.sbHandleWidth.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) panelPrefs.handleWidth = progress
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                restartServiceIfRunning()
            }
        })
    }

    private fun restartServiceIfRunning() {
        // Stop then restart
        val stop = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_STOP
        }
        startService(stop)
        // Small delay then restart so the new side takes effect
        binding.root.postDelayed({
            val start = Intent(this, FloatingPanelService::class.java)
            startForegroundService(start)
        }, 400)
    }
}
