package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsM3Binding
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
        binding.tvTapGesturesValue.text = when {
            panelPrefs.tripleTapToOpen -> "Triple Tap"
            panelPrefs.doubleTapToOpen -> "Double Tap"
            panelPrefs.tapToOpen -> "Single Tap"
            else -> "Disabled"
        }
        binding.switchShowPill.isChecked = panelPrefs.showPill
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

        binding.switchBlur.isChecked = panelPrefs.blurEnabled
        binding.sbBlurAmount.value = panelPrefs.blurAmount.toFloat()
        binding.tvBlurAmountValue.text = panelPrefs.blurAmount.toString()
        binding.layoutBlurAmount.visibility = if (panelPrefs.blurEnabled) View.VISIBLE else View.GONE
        
        binding.switchColumns.isChecked = panelPrefs.panelColumns == 2
        binding.sbOpacity.value = panelPrefs.panelOpacity.toFloat()
        binding.tvOpacityValue.text = "${panelPrefs.panelOpacity}%"
        
        binding.sbPanelRadius.value = panelPrefs.panelCornerRadius.toFloat()
        binding.tvRadiusValue.text = "${panelPrefs.panelCornerRadius}dp"
        
        binding.sbHandleHeight.value = panelPrefs.handleHeight.toFloat()
        binding.tvHeightValue.text = "${panelPrefs.handleHeight}dp"
        
        binding.sbHandleWidth.value = panelPrefs.handleWidth.toFloat()
        binding.tvWidthValue.text = "${panelPrefs.handleWidth}dp"
        
        binding.sbHandleOffset.value = panelPrefs.handleVerticalOffset.toFloat()
        binding.tvOffsetValue.text = "${panelPrefs.handleVerticalOffset}dp"

        binding.sbPickerGap.value = panelPrefs.pickerGap.toFloat()
        binding.tvPickerGapValue.text = "${panelPrefs.pickerGap}dp"

        binding.tvUIStyleValue.text = when (panelPrefs.uiTheme) {
            PanelPreferences.THEME_HYPEROS -> "HyperOS (Glass)"
            PanelPreferences.THEME_REALME -> "Realme UI"
            PanelPreferences.THEME_RICH -> "Rich UI (Glow)"
            else -> "OriginOS (Rounded)"
        }

        binding.tvIconShapeValue.text = when (panelPrefs.iconShape) {
            PanelPreferences.SHAPE_SQUIRCLE -> "Squircle"
            PanelPreferences.SHAPE_SQUARE -> "Square"
            PanelPreferences.SHAPE_CIRCLE -> "Circle"
            else -> "System Default"
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

        updateSupportUI()
    }

    private fun updateSupportUI() {
        // All features unlocked by default for F-Droid version
        binding.sbHandleOffset.isEnabled = true
        binding.sbBlurAmount.isEnabled = true
        
        binding.layoutUIStyle.isEnabled = true
        binding.layoutUIStyle.alpha = 1.0f
        
        binding.layoutIconShape.isEnabled = true
        binding.layoutIconShape.alpha = 1.0f

        binding.switchTools.isEnabled = true
        binding.switchHideBg.isEnabled = true
        binding.switchColumns.isEnabled = true
        
        binding.switchUseCustomAccent.isEnabled = true
        binding.sbPanelRadius.isEnabled = true
        binding.btnResetUIColors.isEnabled = true
        
        binding.btnPickAccent.isEnabled = true
        binding.btnPickBg.isEnabled = true
        binding.btnSelectIconPack.isEnabled = true

        binding.tvSupportStatus.text = "Support Development"
        binding.btnGoPremium.text = "Donate"
        binding.btnGoPremium.visibility = View.VISIBLE
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

        binding.btnAccessibility.setOnClickListener {
            AccessibilityGuideDialog.newInstance()
                .show(supportFragmentManager, AccessibilityGuideDialog.TAG)
        }

        binding.layoutTapGestures.setOnClickListener {
            val options = arrayOf("Disabled", "Single Tap", "Double Tap", "Triple Tap")
            var selectedIndex = 0
            if (panelPrefs.tapToOpen) selectedIndex = 1
            if (panelPrefs.doubleTapToOpen) selectedIndex = 2
            if (panelPrefs.tripleTapToOpen) selectedIndex = 3

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Tap to Open")
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.tapToOpen = (which == 1)
                    panelPrefs.doubleTapToOpen = (which == 2)
                    panelPrefs.tripleTapToOpen = (which == 3)
                    binding.tvTapGesturesValue.text = options[which]
                    applyOnly()
                    dialog.dismiss()
                }
                .show()
        }

        binding.switchShowPill.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showPill = isChecked
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

        binding.switchBlur.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.blurEnabled = isChecked
            binding.layoutBlurAmount.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyOnly()
        }

        binding.sbBlurAmount.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val amount = value.toInt()
                panelPrefs.blurAmount = amount
                binding.tvBlurAmountValue.text = amount.toString()
            }
        }
        binding.sbBlurAmount.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                applyOnly()
            }
        })

        binding.btnResetBlur.setOnClickListener {
            val default = 15
            panelPrefs.blurAmount = default
            binding.sbBlurAmount.value = default.toFloat()
            binding.tvBlurAmountValue.text = default.toString()
            applyOnly()
        }

        binding.switchColumns.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.panelColumns = if (isChecked) 2 else 1
            applyOnly()
        }

        binding.sbOpacity.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                panelPrefs.panelOpacity = progress
                binding.tvOpacityValue.text = "$progress%"
            }
        }
        binding.sbOpacity.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                applyOnly()
            }
        })

        binding.btnResetOpacity.setOnClickListener {
            val default = 100
            panelPrefs.panelOpacity = default
            binding.sbOpacity.value = default.toFloat()
            binding.tvOpacityValue.text = "$default%"
            applyOnly()
        }

        binding.sbHandleHeight.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                panelPrefs.handleHeight = progress
                binding.tvHeightValue.text = "${progress}dp"
            }
        }
        binding.sbHandleHeight.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                applyOnly()
            }
        })

        binding.btnResetHeight.setOnClickListener {
            val default = 80
            panelPrefs.handleHeight = default
            binding.sbHandleHeight.value = default.toFloat()
            binding.tvHeightValue.text = "${default}dp"
            applyOnly()
        }

        binding.sbHandleWidth.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                panelPrefs.handleWidth = progress
                binding.tvWidthValue.text = "${progress}dp"
            }
        }
        binding.sbHandleWidth.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                applyOnly()
            }
        })

        binding.btnResetWidth.setOnClickListener {
            val default = 24
            panelPrefs.handleWidth = default
            binding.sbHandleWidth.value = default.toFloat()
            binding.tvWidthValue.text = "${default}dp"
            applyOnly()
        }

        binding.btnGoPremium.setOnClickListener {
            val intent = Intent(this, SupportActivity::class.java)
            startActivity(intent)
        }

        binding.sbHandleOffset.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val offset = value.toInt()
                panelPrefs.handleVerticalOffset = offset
                binding.tvOffsetValue.text = "${offset}dp"
            }
        }
        binding.sbHandleOffset.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                applyAndShow() // Vertical offset needs a full WindowManager update
            }
        })

        binding.btnResetOffset.setOnClickListener {
            val default = 0
            panelPrefs.handleVerticalOffset = default
            binding.sbHandleOffset.value = default.toFloat()
            binding.tvOffsetValue.text = "${default}dp"
            applyAndShow()
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
                applyOnly() // Just save, don't trigger a full preview refresh
            }
        })

        binding.btnResetPickerGap.setOnClickListener {
            val default = 20
            panelPrefs.pickerGap = default
            binding.sbPickerGap.value = default.toFloat()
            binding.tvPickerGapValue.text = "${default}dp"
            applyOnly()
        }

        binding.sbPanelRadius.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                panelPrefs.panelCornerRadius = progress
                binding.tvRadiusValue.text = "${progress}dp"
            }
        }
        binding.sbPanelRadius.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                applyOnly()
            }
        })

        binding.btnResetRadius.setOnClickListener {
            val default = 20
            panelPrefs.panelCornerRadius = default
            binding.sbPanelRadius.value = default.toFloat()
            binding.tvRadiusValue.text = "${default}dp"
            applyOnly()
        }

        binding.layoutUIStyle.setOnClickListener {
            val options = arrayOf("OriginOS (Rounded)", "HyperOS (Glass)", "Realme UI", "Rich UI (Glow)")
            val values = arrayOf(
                PanelPreferences.THEME_ORIGIN,
                PanelPreferences.THEME_HYPEROS,
                PanelPreferences.THEME_REALME,
                PanelPreferences.THEME_RICH
            )
            
            val selectedIndex = values.indexOf(panelPrefs.uiTheme).let { if (it == -1) 0 else it }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("UI Style Theme")
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.uiTheme = values[which]
                    binding.tvUIStyleValue.text = options[which]
                    
                    // Auto-disable custom accent for Origin theme to match standard look
                    if (panelPrefs.uiTheme == PanelPreferences.THEME_ORIGIN) {
                        panelPrefs.useCustomAccent = false
                        binding.switchUseCustomAccent.isChecked = false
                    }
                    
                    updateSupportUI()
                    applyAndShow()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.layoutIconShape.setOnClickListener {
            val options = arrayOf("System Default", "Circle", "Squircle", "Square")
            val values = arrayOf(
                PanelPreferences.SHAPE_SYSTEM,
                PanelPreferences.SHAPE_CIRCLE,
                PanelPreferences.SHAPE_SQUIRCLE,
                PanelPreferences.SHAPE_SQUARE
            )
            
            val selectedIndex = values.indexOf(panelPrefs.iconShape).let { if (it == -1) 0 else it }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Icon Shape")
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.iconShape = values[which]
                    binding.tvIconShapeValue.text = options[which]
                    applyAndShow()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.switchUseCustomAccent.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.useCustomAccent = isChecked
            applyOnly()
        }

        binding.btnSelectIconPack.setOnClickListener {
            val intent = Intent(this, IconPackActivity::class.java)
            startActivity(intent)
        }

        binding.switchTools.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showTools = isChecked
            applyOnly()
        }

        binding.switchHideBg.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.hideBackground = isChecked
            applyOnly()
        }

        binding.btnResetDefaults.setOnClickListener {
            panelPrefs.resetToDefaults()
            loadCurrentSettings() 
            applyAndShow()
            binding.root.showModernToast("Settings Reset to Defaults")
        }

        binding.btnResetUIColors.setOnClickListener {
            panelPrefs.resetUIColors()
            loadCurrentSettings() 
            applyOnly()
            binding.root.showModernToast("UI Colors Restored to Default")
        }

        binding.btnPickAccent.setOnClickListener {
            if (panelPrefs.uiTheme == PanelPreferences.THEME_ORIGIN) {
                binding.root.showModernToast("Accent color is locked for OriginOS theme")
                return@setOnClickListener
            }
            openColorPicker(Color.parseColor(panelPrefs.accentColor)) { newColor ->
                val hex = String.format("#%06X", (0xFFFFFF and newColor))
                panelPrefs.accentColor = hex
                loadCurrentSettings()
                applyOnly()
            }
        }

        binding.btnPickBg.setOnClickListener {
            if (panelPrefs.uiTheme == PanelPreferences.THEME_ORIGIN) {
                binding.root.showModernToast("Background color is locked for OriginOS theme")
                return@setOnClickListener
            }
            openColorPicker(Color.parseColor(panelPrefs.panelBackgroundColor)) { newColor ->
                val hex = String.format("#E6%06X", (0xFFFFFF and newColor))
                panelPrefs.panelBackgroundColor = hex
                loadCurrentSettings()
                applyOnly()
            }
        }

        binding.switchUseCustomAccent.setOnTouchListener { _, _ ->
            if (panelPrefs.uiTheme == PanelPreferences.THEME_ORIGIN) {
                binding.root.showModernToast("Custom accent is disabled for OriginOS theme")
                true 
            } else {
                false
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
