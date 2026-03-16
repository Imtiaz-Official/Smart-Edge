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
import rikka.shizuku.Shizuku
import android.content.pm.PackageManager
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsM3Binding
import yuku.ambilwarna.AmbilWarnaDialog

/**
 * Settings screen for panel configuration.
 * Includes real-time preview and premium dashboard.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsM3Binding
    private lateinit var panelPrefs: PanelPreferences
    private val SHIZUKU_CODE = 1001

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsM3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Register Shizuku listener
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)

        // Set status bar color and icons
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window.statusBarColor = typedValue.data
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

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

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
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
        binding.switchSysInfo.isChecked = panelPrefs.showSysInfo
        binding.switchPowerMenu.isChecked = panelPrefs.showPowerMenu

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
        
        binding.sbHandleOffset.value = (panelPrefs.handleVerticalOffset + 100).toFloat()
        binding.tvOffsetValue.text = "${panelPrefs.handleVerticalOffset}dp"

        binding.sbPickerGap.value = panelPrefs.pickerGap.toFloat()
        binding.tvPickerGapValue.text = "${panelPrefs.pickerGap}dp"

        binding.tvUIStyleValue.text = when (panelPrefs.uiTheme) {
            PanelPreferences.THEME_HYPEROS -> "HyperOS (Glass)"
            PanelPreferences.THEME_REALME -> "Realme UI"
            PanelPreferences.THEME_RICH -> "Rich UI (Glow)"
            PanelPreferences.THEME_M3 -> "Material 3 (Modern)"
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
        updateShizukuStatus()
    }

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
            binding.tvShizukuStatus.setTextColor(Color.parseColor("#FFA500")) // Orange
            binding.btnLinkShizuku.text = "Authorize Shizuku"
            binding.btnLinkShizuku.isEnabled = true
        } else {
            binding.tvShizukuStatus.text = "Service Running & Authorized"
            binding.tvShizukuStatus.setTextColor(Color.parseColor("#4CAF50")) // Green
            binding.btnLinkShizuku.text = "Shizuku Ready"
            binding.btnLinkShizuku.isEnabled = false
        }
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

        binding.btnLinkShizuku.setOnClickListener {
            if (!ShizukuHelper.isShizukuAvailable()) {
                // Open Shizuku app or guide
                try {
                    val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        // Open Play Store or website
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

        binding.switchGestures.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.gesturesEnabled = isChecked
            applyOnly()
        }

        binding.btnAccessibility.setOnClickListener {
            try {
                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                binding.root.showModernToast("Scroll down to 'Downloaded Apps' and enable 'Smart Edge'", Snackbar.LENGTH_LONG)
            } catch (e: Exception) {
                binding.root.showModernToast("Could not open Accessibility Settings")
            }
        }

        binding.switchTapOpen.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.tapToOpen = isChecked
            applyOnly()
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

        binding.switchSysInfo.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showSysInfo = isChecked
            applyOnly()
        }

        binding.switchPowerMenu.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showPowerMenu = isChecked
            applyOnly()
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

        binding.btnGoPremium.setOnClickListener {
            val intent = Intent(this, SupportActivity::class.java)
            startActivity(intent)
        }

        binding.sbHandleOffset.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                val offset = progress - 100
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

        binding.layoutUIStyle.setOnClickListener {
            val options = arrayOf("OriginOS (Rounded)", "HyperOS (Glass)", "Realme UI", "Rich UI (Glow)", "Material 3 (Modern)")
            val values = arrayOf(
                PanelPreferences.THEME_ORIGIN,
                PanelPreferences.THEME_HYPEROS,
                PanelPreferences.THEME_REALME,
                PanelPreferences.THEME_RICH,
                PanelPreferences.THEME_M3
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
