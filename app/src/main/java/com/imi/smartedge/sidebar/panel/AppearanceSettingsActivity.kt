package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsAppearanceBinding

/**
 * Handles all UI styling settings:
 * - Theme selection (Origin, HyperOS, etc)
 * - Accent color
 * - Background color & opacity
 * - Corner radius
 * - Scale & Size
 */
class AppearanceSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsAppearanceBinding
    private lateinit var panelPrefs: PanelPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsAppearanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        panelPrefs = PanelPreferences(this)

        setupToolbar()
        loadCurrentSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadCurrentSettings() {
        binding.sbOpacity.value = panelPrefs.panelOpacity.toFloat()
        binding.tvOpacityValue.text = "${panelPrefs.panelOpacity}%"

        binding.sbPanelRadius.value = panelPrefs.panelCornerRadius.toFloat()
        binding.tvRadiusValue.text = "${panelPrefs.panelCornerRadius}dp"

        binding.sbIconScale.value = panelPrefs.scaleFactor
        binding.tvIconScaleValue.text = String.format("%.1fx", panelPrefs.scaleFactor)

        binding.sbMaxHeight.value = panelPrefs.panelMaxHeight.toFloat()
        binding.tvMaxHeightValue.text = "${panelPrefs.panelMaxHeight}dp"

        binding.sbPickerMaxHeight.value = panelPrefs.pickerMaxHeight.toFloat()
        binding.tvPickerMaxHeightValue.text = "${panelPrefs.pickerMaxHeight}dp"

        binding.tvThemeModeValue.text = when (panelPrefs.themeMode) {
            PanelPreferences.MODE_LIGHT -> "Light"
            PanelPreferences.MODE_DARK -> "Dark"
            else -> "Follow System"
        }

        binding.tvUIStyleValue.text = when (panelPrefs.uiTheme) {
            PanelPreferences.THEME_HYPEROS -> "HyperOS (Glass)"
            PanelPreferences.THEME_REALME -> "Realme UI"
            PanelPreferences.THEME_RICH -> "Rich UI (Glow)"
            else -> "OriginOS (Rounded)"
        }

        binding.tvIconShapeValue.text = when (panelPrefs.iconShape) {
            PanelPreferences.SHAPE_CIRCLE -> "Circle"
            PanelPreferences.SHAPE_SQUARE -> "Square"
            PanelPreferences.SHAPE_ROUNDED -> "Rounded"
            PanelPreferences.SHAPE_SQUIRCLE -> "Squircle"
            else -> "System Default"
        }

        binding.featureBlur.isChecked = panelPrefs.blurEnabled
        binding.sbBlurAmount.value = panelPrefs.blurAmount.toFloat()
        binding.tvBlurAmountValue.text = "${panelPrefs.blurAmount}"
        
        binding.featureHideBg.isChecked = panelPrefs.hideBackground
        
        binding.tvColumnsValue.text = "${panelPrefs.panelColumns} Column${if (panelPrefs.panelColumns > 1) "s" else ""}"
        
        binding.featureCustomAccent.isChecked = panelPrefs.useCustomAccent
        
        binding.tvCurrentIconPack.text = panelPrefs.iconPackLabel

        binding.btnPickAccent.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(panelPrefs.accentColor))
        binding.btnPickBg.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(panelPrefs.panelBackgroundColor))

        binding.tvHomeButtonStyleValue.text = when (panelPrefs.homeButtonStyle) {
            PanelPreferences.STYLE_POWER -> "Modern Power Icon"
            else -> "Classic Logo"
        }
    }

    private fun setupListeners() {
        binding.sbOpacity.addOnChangeListener { _, value, _ ->
            panelPrefs.panelOpacity = value.toInt()
            binding.tvOpacityValue.text = "${value.toInt()}%"
            applyOnly()
        }

        binding.sbPanelRadius.addOnChangeListener { _, value, _ ->
            panelPrefs.panelCornerRadius = value.toInt()
            binding.tvRadiusValue.text = "${value.toInt()}dp"
            applyOnly()
        }

        binding.sbIconScale.addOnChangeListener { _, value, _ ->
            panelPrefs.scaleFactor = value
            binding.tvIconScaleValue.text = String.format("%.1fx", value)
            applyOnly()
        }

        binding.sbMaxHeight.addOnChangeListener { _, value, _ ->
            panelPrefs.panelMaxHeight = value.toInt()
            binding.tvMaxHeightValue.text = "${value.toInt()}dp"
            applyOnly()
        }

        binding.sbPickerMaxHeight.addOnChangeListener { _, value, _ ->
            panelPrefs.pickerMaxHeight = value.toInt()
            binding.tvPickerMaxHeightValue.text = "${value.toInt()}dp"
            applyOnly()
        }

        binding.btnResetIconScale.setOnClickListener {
            panelPrefs.scaleFactor = 1.0f
            binding.sbIconScale.value = 1.0f
            binding.tvIconScaleValue.text = "1.0x"
            applyOnly()
        }

        binding.btnResetMaxHeight.setOnClickListener {
            val default = 350
            panelPrefs.panelMaxHeight = default
            binding.sbMaxHeight.value = default.toFloat()
            binding.tvMaxHeightValue.text = "${default}dp"
            applyOnly()
        }

        binding.btnResetPickerMaxHeight.setOnClickListener {
            val default = 450
            panelPrefs.pickerMaxHeight = default
            binding.sbPickerMaxHeight.value = default.toFloat()
            binding.tvPickerMaxHeightValue.text = "${default}dp"
            applyOnly()
        }

        binding.btnResetOpacity.setOnClickListener {
            val default = 100
            panelPrefs.panelOpacity = default
            binding.sbOpacity.value = default.toFloat()
            binding.tvOpacityValue.text = "${default}%"
            applyOnly()
        }

        binding.btnResetRadius.setOnClickListener {
            val default = 20
            panelPrefs.panelCornerRadius = default
            binding.sbPanelRadius.value = default.toFloat()
            binding.tvRadiusValue.text = "${default}dp"
            applyOnly()
        }

        binding.featureThemeMode.setOnClickListener {
            val options = arrayOf("Follow System", "Light", "Dark")
            val values = arrayOf(
                PanelPreferences.MODE_SYSTEM,
                PanelPreferences.MODE_LIGHT,
                PanelPreferences.MODE_DARK
            )
            
            val selectedIndex = values.indexOf(panelPrefs.themeMode).let { if (it == -1) 0 else it }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("App Theme")
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.themeMode = values[which]
                    binding.tvThemeModeValue.text = options[which]
                    applyAppTheme(this)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
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
                .setTitle("Panel UI Style")
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.uiTheme = values[which]
                    binding.tvUIStyleValue.text = options[which]
                    applyOnly()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.featureIconShape.setOnClickListener {
            val options = arrayOf("System Default", "Circle", "Square", "Rounded", "Squircle")
            val values = arrayOf(
                PanelPreferences.SHAPE_SYSTEM,
                PanelPreferences.SHAPE_CIRCLE,
                PanelPreferences.SHAPE_SQUARE,
                PanelPreferences.SHAPE_ROUNDED,
                PanelPreferences.SHAPE_SQUIRCLE
            )

            val selectedIndex = values.indexOf(panelPrefs.iconShape).let { if (it == -1) 0 else it }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Icon Shape")
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.iconShape = values[which]
                    binding.tvIconShapeValue.text = options[which]
                    applyOnly()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.featureBlur.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.blurEnabled = isChecked
            applyOnly()
        }

        binding.sbBlurAmount.addOnChangeListener { _, value, _ ->
            panelPrefs.blurAmount = value.toInt()
            binding.tvBlurAmountValue.text = "${value.toInt()}"
            applyOnly()
        }

        binding.featureHideBg.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.hideBackground = isChecked
            applyOnly()
        }

        binding.featureColumns.setOnClickListener {
            val options = arrayOf("1 Column", "2 Columns", "3 Columns")
            val currentSelectedIndex = panelPrefs.panelColumns - 1
            var newlySelectedIndex = currentSelectedIndex

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Panel Columns")
                .setSingleChoiceItems(options, currentSelectedIndex) { _, which ->
                    newlySelectedIndex = which
                }
                .setPositiveButton("Apply") { _, _ ->
                    val columns = newlySelectedIndex + 1
                    panelPrefs.panelColumns = columns
                    binding.tvColumnsValue.text = options[newlySelectedIndex]
                    applyOnly()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.featureCustomAccent.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.useCustomAccent = isChecked
            applyOnly()
        }

        binding.btnSelectIconPack.setOnClickListener {
            startActivity(Intent(this, IconPackActivity::class.java))
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

        binding.featureHomeButton.setOnClickListener {
            val options = arrayOf("Modern Power Icon", "Classic Logo")
            val values = arrayOf(PanelPreferences.STYLE_POWER, PanelPreferences.STYLE_CLASSIC)
            val selectedIndex = values.indexOf(panelPrefs.homeButtonStyle).let { if (it == -1) 0 else it }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Service Button Style")
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.homeButtonStyle = values[which]
                    binding.tvHomeButtonStyleValue.text = options[which]
                    applyOnly()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun applyOnly() {
        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_REFRESH
        }
        startService(intent)
    }
}
