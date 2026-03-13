package com.originpanel.sidepanel

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.originpanel.sidepanel.databinding.ActivitySettingsBinding
import yuku.ambilwarna.AmbilWarnaDialog

/**
 * Settings screen for panel configuration.
 * Includes real-time preview and premium dashboard.
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
        if (panelPrefs.panelSide == PanelPreferences.SIDE_LEFT) {
            binding.rgPanelSide.check(R.id.rbLeft)
        } else {
            binding.rgPanelSide.check(R.id.rbRight)
        }

        binding.switchAutoStart.isChecked = panelPrefs.autoStart
        binding.switchGestures.isChecked = panelPrefs.gesturesEnabled
        binding.switchTapOpen.isChecked = panelPrefs.tapToOpen
        binding.switchShowPill.isChecked = panelPrefs.showPill
        binding.switchHaptic.isChecked = panelPrefs.hapticEnabled
        binding.switchBlur.isChecked = panelPrefs.blurEnabled
        binding.sbOpacity.progress = panelPrefs.panelOpacity
        binding.tvOpacityValue.text = "${panelPrefs.panelOpacity}%"
        
        binding.sbPanelRadius.progress = panelPrefs.panelCornerRadius
        binding.tvRadiusValue.text = "${panelPrefs.panelCornerRadius}dp"
        
        binding.sbHandleHeight.progress = panelPrefs.handleHeight 
        binding.tvHeightValue.text = "${panelPrefs.handleHeight}dp"
        
        binding.sbHandleWidth.progress = panelPrefs.handleWidth 
        binding.tvWidthValue.text = "${panelPrefs.handleWidth}dp"
        
        binding.sbHandleOffset.progress = panelPrefs.handleVerticalOffset + 100
        binding.tvOffsetValue.text = "${panelPrefs.handleVerticalOffset}dp"

        when (panelPrefs.uiTheme) {
            PanelPreferences.THEME_HYPEROS -> binding.rgThemes.check(R.id.rbThemeHyper)
            PanelPreferences.THEME_REALME -> binding.rgThemes.check(R.id.rbThemeRealme)
            PanelPreferences.THEME_RICH -> binding.rgThemes.check(R.id.rbThemeRich)
            else -> binding.rgThemes.check(R.id.rbThemeOrigin)
        }

        when (panelPrefs.iconShape) {
            PanelPreferences.SHAPE_SQUIRCLE -> binding.rgIconShape.check(R.id.rbShapeSquircle)
            PanelPreferences.SHAPE_SQUARE -> binding.rgIconShape.check(R.id.rbShapeSquare)
            else -> binding.rgIconShape.check(R.id.rbShapeCircle)
        }

        binding.switchTools.isChecked = panelPrefs.showTools
        binding.switchHideBg.isChecked = panelPrefs.hideBackground
        binding.switchUseCustomAccent.isChecked = panelPrefs.useCustomAccent

        val pack = panelPrefs.selectedIconPack
        binding.tvCurrentIconPack.text = if (pack == "none") "System Default" else pack

        try {
            val accentColor = Color.parseColor(panelPrefs.accentColor)
            binding.btnPickAccent.backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
            
            val bgColor = Color.parseColor(panelPrefs.panelBackgroundColor)
            binding.btnPickBg.backgroundTintList = android.content.res.ColorStateList.valueOf(bgColor)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        updatePremiumUI()
    }

    private fun updatePremiumUI() {
        val isPremium = panelPrefs.isPremium
        binding.sbHandleOffset.isEnabled = isPremium
        binding.rgThemes.isEnabled = isPremium
        binding.rbThemeOrigin.isEnabled = isPremium
        binding.rbThemeHyper.isEnabled = isPremium
        binding.rbThemeRealme.isEnabled = isPremium
        binding.rbThemeRich.isEnabled = isPremium
        
        binding.rgIconShape.isEnabled = isPremium
        binding.rbShapeCircle.isEnabled = isPremium
        binding.rbShapeSquircle.isEnabled = isPremium
        binding.rbShapeSquare.isEnabled = isPremium

        binding.switchTools.isEnabled = isPremium
        binding.switchHideBg.isEnabled = isPremium
        
        binding.btnPickAccent.isEnabled = isPremium
        binding.btnPickBg.isEnabled = isPremium
        binding.btnSelectIconPack.isEnabled = isPremium

        if (isPremium) {
            binding.tvPremiumStatus.text = "Premium Active"
            binding.btnGoPremium.visibility = View.GONE
            binding.cardPremium.setCardBackgroundColor(Color.parseColor("#1A00C853"))
        } else {
            binding.tvPremiumStatus.text = "Unlock Full Customization"
            binding.btnGoPremium.visibility = View.VISIBLE
            binding.cardPremium.setCardBackgroundColor(Color.parseColor("#1A4A9EFF"))
        }
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
            applyAndShow()
        }

        binding.btnAccessibility.setOnClickListener {
            try {
                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Scroll down to 'Downloaded Apps' and enable 'Side Panel'", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open Accessibility Settings", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchTapOpen.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.tapToOpen = isChecked
            applyAndShow()
        }

        binding.switchShowPill.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showPill = isChecked
            applyAndShow()
        }

        binding.switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.hapticEnabled = isChecked
        }

        binding.switchBlur.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.blurEnabled = isChecked
            applyAndShow()
        }

        binding.sbOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    panelPrefs.panelOpacity = progress
                    binding.tvOpacityValue.text = "$progress%"
                    applyAndShow()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })

        binding.sbHandleHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    panelPrefs.handleHeight = progress
                    binding.tvHeightValue.text = "${progress}dp"
                    applyAndShow()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })

        binding.sbHandleWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    panelPrefs.handleWidth = progress
                    binding.tvWidthValue.text = "${progress}dp"
                    applyAndShow()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })

        binding.btnGoPremium.setOnClickListener {
            panelPrefs.isPremium = true
            updatePremiumUI()
            Toast.makeText(this, "Welcome to Premium!", Toast.LENGTH_SHORT).show()
        }

        binding.sbHandleOffset.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val offset = progress - 100
                    panelPrefs.handleVerticalOffset = offset
                    binding.tvOffsetValue.text = "${offset}dp"
                    applyAndShow()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })

        binding.sbPanelRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    panelPrefs.panelCornerRadius = progress
                    binding.tvRadiusValue.text = "${progress}dp"
                    applyAndShow()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })

        binding.rgThemes.setOnCheckedChangeListener { _, checkedId ->
            panelPrefs.uiTheme = when (checkedId) {
                R.id.rbThemeHyper -> PanelPreferences.THEME_HYPEROS
                R.id.rbThemeRealme -> PanelPreferences.THEME_REALME
                R.id.rbThemeRich -> PanelPreferences.THEME_RICH
                else -> PanelPreferences.THEME_ORIGIN
            }
            applyAndShow()
        }

        binding.rgIconShape.setOnCheckedChangeListener { _, checkedId ->
            panelPrefs.iconShape = when (checkedId) {
                R.id.rbShapeSquircle -> PanelPreferences.SHAPE_SQUIRCLE
                R.id.rbShapeSquare -> PanelPreferences.SHAPE_SQUARE
                else -> PanelPreferences.SHAPE_CIRCLE
            }
            applyAndShow()
        }

        binding.switchUseCustomAccent.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.useCustomAccent = isChecked
            applyAndShow()
        }

        binding.btnSelectIconPack.setOnClickListener {
            val intent = Intent(this, IconPackActivity::class.java)
            startActivity(intent)
        }

        binding.switchTools.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showTools = isChecked
            applyAndShow()
        }

        binding.switchHideBg.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.hideBackground = isChecked
            applyAndShow()
        }

        binding.btnResetDefaults.setOnClickListener {
            panelPrefs.resetToDefaults()
            loadCurrentSettings() 
            applyAndShow()
            Toast.makeText(this, "Settings Reset to Defaults", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetUIColors.setOnClickListener {
            panelPrefs.resetUIColors()
            loadCurrentSettings() 
            applyAndShow()
            Toast.makeText(this, "UI Colors Restored to Default", Toast.LENGTH_SHORT).show()
        }

        binding.btnPickAccent.setOnClickListener {
            openColorPicker(Color.parseColor(panelPrefs.accentColor)) { newColor ->
                val hex = String.format("#%06X", (0xFFFFFF and newColor))
                panelPrefs.accentColor = hex
                loadCurrentSettings()
                applyAndShow()
            }
        }

        binding.btnPickBg.setOnClickListener {
            openColorPicker(Color.parseColor(panelPrefs.panelBackgroundColor)) { newColor ->
                val hex = String.format("#E6%06X", (0xFFFFFF and newColor))
                panelPrefs.panelBackgroundColor = hex
                loadCurrentSettings()
                applyAndShow()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadCurrentSettings() 
    }

    private fun openColorPicker(initialColor: Int, onPick: (Int) -> Unit) {
        val picker = AmbilWarnaDialog(this, initialColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {}
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                onPick(color)
            }
        })
        picker.show()
    }

    private fun applyAndShow() {
        val stop = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_STOP
        }
        startService(stop)
        
        binding.root.postDelayed({
            val start = Intent(this, FloatingPanelService::class.java).apply {
                action = "com.originpanel.sidepanel.SHOW_TEMP"
            }
            startForegroundService(start)
        }, 300)
    }
}
