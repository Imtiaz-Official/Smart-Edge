package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsAppearanceBinding
import yuku.ambilwarna.AmbilWarnaDialog

class AppearanceSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsAppearanceBinding
    private lateinit var panelPrefs: PanelPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsAppearanceBinding.inflate(layoutInflater)
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

    override fun onResume() {
        super.onResume()
        loadCurrentSettings()
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
                binding.appearanceScrollView.smoothScrollTo(0, rect.top - 200)
                targetView.highlightView()
            }
        }
    }

    private fun loadCurrentSettings() {
        binding.featureBlur.isChecked = panelPrefs.blurEnabled
        binding.sbBlurAmount.value = panelPrefs.blurAmount.toFloat()
        binding.tvBlurAmountValue.text = panelPrefs.blurAmount.toString()
        binding.featureBlurIntensity.visibility = if (panelPrefs.blurEnabled) View.VISIBLE else View.GONE
        
        binding.featureColumns.isChecked = panelPrefs.panelColumns == 2
        binding.sbOpacity.value = panelPrefs.panelOpacity.toFloat()
        binding.tvOpacityValue.text = "${panelPrefs.panelOpacity}%"
        
        binding.sbPanelRadius.value = panelPrefs.panelCornerRadius.toFloat()
        binding.tvRadiusValue.text = "${panelPrefs.panelCornerRadius}dp"

        binding.sbMaxHeight.value = panelPrefs.panelMaxHeight.toFloat()
        binding.tvMaxHeightValue.text = "${panelPrefs.panelMaxHeight}dp"

        binding.sbPickerMaxHeight.value = panelPrefs.pickerMaxHeight.toFloat()
        binding.tvPickerMaxHeightValue.text = "${panelPrefs.pickerMaxHeight}dp"

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

        binding.tvHomeButtonStyleValue.text = when (panelPrefs.homeButtonStyle) {
            PanelPreferences.STYLE_CLASSIC -> "Classic (Text Button)"
            else -> "Modern (Power Icon)"
        }

        binding.featureHideBg.isChecked = panelPrefs.hideBackground
        binding.featureCustomAccent.isChecked = panelPrefs.useCustomAccent

        binding.tvCurrentIconPack.text = panelPrefs.iconPackLabel

        try {
            val accentColor = Color.parseColor(panelPrefs.accentColor)
            binding.btnPickAccent.backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
            
            val bgColor = Color.parseColor(panelPrefs.panelBackgroundColor)
            binding.btnPickBg.backgroundTintList = android.content.res.ColorStateList.valueOf(bgColor)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupListeners() {
        binding.featureBlur.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.blurEnabled = isChecked
            binding.featureBlurIntensity.visibility = if (isChecked) View.VISIBLE else View.GONE
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

        binding.featureColumns.setOnCheckedChangeListener { _, isChecked ->
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

        binding.sbMaxHeight.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                panelPrefs.panelMaxHeight = progress
                binding.tvMaxHeightValue.text = "${progress}dp"
            }
        }
        binding.sbMaxHeight.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                applyOnly()
            }
        })

        binding.btnResetMaxHeight.setOnClickListener {
            val default = 350
            panelPrefs.panelMaxHeight = default
            binding.sbMaxHeight.value = default.toFloat()
            binding.tvMaxHeightValue.text = "${default}dp"
            applyOnly()
        }

        binding.sbPickerMaxHeight.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                panelPrefs.pickerMaxHeight = progress
                binding.tvPickerMaxHeightValue.text = "${progress}dp"
            }
        }
        binding.sbPickerMaxHeight.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                applyOnly()
            }
        })

        binding.btnResetPickerMaxHeight.setOnClickListener {
            val default = 450
            panelPrefs.pickerMaxHeight = default
            binding.sbPickerMaxHeight.value = default.toFloat()
            binding.tvPickerMaxHeightValue.text = "${default}dp"
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
                    
                    if (panelPrefs.uiTheme == PanelPreferences.THEME_ORIGIN) {
                        panelPrefs.useCustomAccent = false
                        binding.featureCustomAccent.isChecked = false
                    }
                    
                    applyAndShow()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.featureIconShape.setOnClickListener {
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

        binding.featureHomeButton.setOnClickListener {
            val options = arrayOf("Modern (Power Icon)", "Classic (Text Button)")
            val values = arrayOf(
                PanelPreferences.STYLE_POWER,
                PanelPreferences.STYLE_CLASSIC
            )
            
            val selectedIndex = values.indexOf(panelPrefs.homeButtonStyle).let { if (it == -1) 0 else it }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Home Button Style")
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.homeButtonStyle = values[which]
                    binding.tvHomeButtonStyleValue.text = options[which]
                    // Refresh current activity if needed (though it will refresh on resume if we go back)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.featureCustomAccent.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.useCustomAccent = isChecked
            applyOnly()
        }

        binding.btnSelectIconPack.setOnClickListener {
            val intent = Intent(this, IconPackActivity::class.java)
            startActivity(intent)
        }

        binding.featureHideBg.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.hideBackground = isChecked
            applyOnly()
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
