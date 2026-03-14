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
import com.originpanel.sidepanel.databinding.ActivitySettingsM3Binding
import yuku.ambilwarna.AmbilWarnaDialog

/**
 * Settings screen for panel configuration.
 * Includes real-time preview and premium dashboard.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsM3Binding
    private lateinit var panelPrefs: PanelPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsM3Binding.inflate(layoutInflater)
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
        binding.switchShowLogs.isChecked = panelPrefs.showLogs
        binding.switchBlur.isChecked = panelPrefs.blurEnabled
        binding.switchColumns.isChecked = panelPrefs.panelColumns == 2
        binding.sbOpacity.value = panelPrefs.panelOpacity.toFloat()
        binding.tvOpacityValue.text = "${panelPrefs.panelOpacity}%"
        
        binding.sbPanelRadius.value = panelPrefs.panelCornerRadius.toFloat()
        binding.tvRadiusValue.text = "${panelPrefs.panelCornerRadius}dp"
        
        binding.sbHandleHeight.value = panelPrefs.handleHeight.toFloat()
        binding.tvHeightValue.text = "${panelPrefs.handleHeight}dp"
        
        binding.sbHandleWidth.value = panelPrefs.handleWidth.toFloat()
        binding.tvWidthValue.text = "${panelPrefs.handleWidth}dp"
        
        binding.sbHandleOffset.value = (panelPrefs.handleVerticalOffset + 100).toFloat()
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
        binding.switchColumns.isEnabled = isPremium
        binding.switchUseCustomAccent.isEnabled = isPremium
        binding.sbPanelRadius.isEnabled = isPremium
        binding.btnResetUIColors.isEnabled = isPremium
        
        binding.btnPickAccent.isEnabled = isPremium
        binding.btnPickBg.isEnabled = isPremium
        binding.btnSelectIconPack.isEnabled = isPremium

        // Resolve theme colors for Material 3 styling
        val typedValue = android.util.TypedValue()
        
        if (isPremium) {
            binding.tvPremiumStatus.text = "Premium Active"
            binding.btnGoPremium.visibility = View.GONE
            
            theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHigh, typedValue, true)
            binding.cardPremium.setCardBackgroundColor(typedValue.data)
            binding.cardPremium.strokeWidth = 2
            
            theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
            binding.cardPremium.strokeColor = typedValue.data
            
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
            val onSurface = typedValue.data
            
            binding.tvPremiumStatus.setTextColor(onSurface)
            binding.switchUseCustomAccent.setTextColor(onSurface)
            binding.switchTools.setTextColor(onSurface)
            binding.switchHideBg.setTextColor(onSurface)
            binding.tvAccentColor.setTextColor(onSurface)
            binding.tvPanelBgColor.setTextColor(onSurface)
        } else {
            binding.tvPremiumStatus.text = "Unlock Full Customization"
            binding.btnGoPremium.visibility = View.VISIBLE
            
            theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainer, typedValue, true)
            binding.cardPremium.setCardBackgroundColor(typedValue.data)
            binding.cardPremium.strokeWidth = 0
            
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)
            val onSurfaceVariant = typedValue.data
            
            binding.tvPremiumStatus.setTextColor(onSurfaceVariant)
            binding.switchUseCustomAccent.setTextColor(onSurfaceVariant)
            binding.switchTools.setTextColor(onSurfaceVariant)
            binding.switchHideBg.setTextColor(onSurfaceVariant)
            binding.tvAccentColor.setTextColor(onSurfaceVariant)
            binding.tvPanelBgColor.setTextColor(onSurfaceVariant)
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

        binding.switchShowLogs.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showLogs = isChecked
        }

        binding.switchBlur.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.blurEnabled = isChecked
            applyAndShow()
        }

        binding.switchColumns.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.panelColumns = if (isChecked) 2 else 1
            applyAndShow()
        }

        binding.sbOpacity.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                panelPrefs.panelOpacity = progress
                binding.tvOpacityValue.text = "$progress%"
                applyAndShow()
            }
        }

        binding.sbHandleHeight.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                panelPrefs.handleHeight = progress
                binding.tvHeightValue.text = "${progress}dp"
                applyAndShow()
            }
        }

        binding.sbHandleWidth.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                panelPrefs.handleWidth = progress
                binding.tvWidthValue.text = "${progress}dp"
                applyAndShow()
            }
        }

        binding.btnGoPremium.setOnClickListener {
            panelPrefs.isPremium = true
            updatePremiumUI()
            Toast.makeText(this, "Welcome to Premium!", Toast.LENGTH_SHORT).show()
        }

        binding.sbHandleOffset.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                val offset = progress - 100
                panelPrefs.handleVerticalOffset = offset
                binding.tvOffsetValue.text = "${offset}dp"
                applyAndShow()
            }
        }

        binding.sbPanelRadius.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                panelPrefs.panelCornerRadius = progress
                binding.tvRadiusValue.text = "${progress}dp"
                applyAndShow()
            }
        }

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
                action = FloatingPanelService.ACTION_SHOW_TEMP
            }
            startForegroundService(start)
        }, 300)
    }
}
