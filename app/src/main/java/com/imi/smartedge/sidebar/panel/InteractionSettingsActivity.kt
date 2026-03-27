package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsInteractionBinding
import com.google.android.material.snackbar.Snackbar

class InteractionSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsInteractionBinding
    private lateinit var panelPrefs: PanelPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsInteractionBinding.inflate(layoutInflater)
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
    }

    private fun loadCurrentSettings() {
        if (panelPrefs.panelSide == PanelPreferences.SIDE_LEFT) {
            binding.rgPanelSide.check(R.id.rbLeft)
        } else {
            binding.rgPanelSide.check(R.id.rbRight)
        }

        binding.switchAutoStart.isChecked = panelPrefs.autoStart
        binding.switchGestures.isChecked = panelPrefs.gesturesEnabled
        binding.switchTapOpen.isChecked = panelPrefs.tapToOpen
        binding.switchHaptic.isChecked = panelPrefs.hapticEnabled
        binding.switchShowLogs.isChecked = panelPrefs.showLogs

        val animSpeed = panelPrefs.animSpeed
        binding.tvAnimFeelValue.text = when (animSpeed) {
            200 -> "Calm (Slow)"
            400 -> "Balanced (Default)"
            700 -> "Snappy"
            1000 -> "Instant"
            else -> "Balanced (Default)"
        }

        binding.sbPickerGap.value = panelPrefs.pickerGap.toFloat()
        binding.tvPickerGapValue.text = "${panelPrefs.pickerGap}dp"
    }

    private fun setupListeners() {
        binding.rgPanelSide.setOnCheckedChangeListener { _, checkedId ->
            panelPrefs.panelSide = if (checkedId == R.id.rbLeft)
                PanelPreferences.SIDE_LEFT else PanelPreferences.SIDE_RIGHT
            applyAndShow()
        }

        binding.switchAutoStart.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.autoStart = isChecked
        }

        binding.switchGestures.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.gesturesEnabled = isChecked
            applyOnly()
        }

        binding.switchTapOpen.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.tapToOpen = isChecked
            applyOnly()
        }

        binding.switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.hapticEnabled = isChecked
        }

        binding.switchShowLogs.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showLogs = isChecked
        }

        binding.layoutAnimFeel.setOnClickListener {
            val options = arrayOf("Calm (Slow)", "Balanced (Default)", "Snappy", "Instant")
            val values = intArrayOf(200, 400, 700, 1000)
            
            var selectedIndex = values.indexOf(panelPrefs.animSpeed)
            if (selectedIndex == -1) selectedIndex = 1 // Default to Balanced

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Animation Feel")
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.animSpeed = values[which]
                    binding.tvAnimFeelValue.text = options[which]
                    applyOnly()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.sbPickerGap.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val gap = value.toInt()
                panelPrefs.pickerGap = gap
                binding.tvPickerGapValue.text = "${gap}dp"
            }
        }
        binding.sbPickerGap.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                applyOnly()
            }
        })
    }

    private fun applyOnly() {
        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_REFRESH
        }
        startService(intent)
    }

    private fun applyAndShow() {
        val stop = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_STOP
        }
        startService(stop)
        binding.root.postDelayed({
            val start = Intent(this, FloatingPanelService::class.java).apply {
                action = FloatingPanelService.ACTION_SHOW_TEMP
            }
            startForegroundService(start)
        }, 300)
    }
}
