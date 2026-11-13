package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
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

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsAppearanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        panelPrefs = PanelPreferences(this)

        setupToolbar()
        loadCurrentSettings()
        setupListeners()
        handleDeepLink()
    }

    private fun handleDeepLink() {
        val targetId = intent.getStringExtra(SettingsMainActivity.EXTRA_SCROLL_TO) ?: return
        if (targetId == "layout_show_app_names_sidebar" || targetId == "layout_show_app_names_drawer") {
            binding.layoutShowAppNamesSuboptions.visibility = View.VISIBLE
        }
        val viewId = resources.getIdentifier(targetId, "id", packageName)
        if (viewId != 0) {
            val targetView = findViewById<View>(viewId)
            targetView?.let { binding.appearanceScrollView.scrollToTargetView(it) }
        }
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
        binding.featureShowBottomArrow.isChecked = panelPrefs.showBottomArrow
        val masterNames = panelPrefs.showAppNamesMaster
        binding.featureShowAppNamesMaster.isChecked = masterNames
        binding.layoutShowAppNamesSuboptions.visibility = if (masterNames) View.VISIBLE else View.GONE
        binding.featureShowAppNamesSidebar.isChecked = panelPrefs.showAppNamesSidebar
        binding.featureShowAppNamesDrawer.isChecked = panelPrefs.showAppNamesDrawerRaw
        
        binding.tvColumnsValue.text = "${panelPrefs.panelColumns} Column${if (panelPrefs.panelColumns > 1) "s" else ""}"
        val autoCloseSeconds = panelPrefs.autoCloseTimeout
        binding.tvAutoCloseValue.text = if (autoCloseSeconds <= 0) "Never" else "${autoCloseSeconds} Seconds"
        
        binding.featureCustomAccent.isChecked = panelPrefs.useCustomAccent
        binding.featureCustomBg.isChecked = panelPrefs.useCustomPanelBg

        val accentRowAlpha = if (panelPrefs.useCustomAccent) 1.0f else 0.5f
        binding.featureAccentColor.alpha = accentRowAlpha
        binding.btnResetAccent.isEnabled = panelPrefs.useCustomAccent

        val bgRowAlpha = if (panelPrefs.useCustomPanelBg) 1.0f else 0.5f
        binding.featureBgColor.alpha = bgRowAlpha
        binding.btnResetBg.isEnabled = panelPrefs.useCustomPanelBg
        
        binding.tvCurrentIconPack.text = panelPrefs.iconPackLabel

        val effectiveBgColor = if (!panelPrefs.useCustomPanelBg && panelPrefs.uiTheme == PanelPreferences.THEME_ORIGIN) {
            Color.parseColor("#1F1F1F")
        } else {
            try { Color.parseColor(panelPrefs.panelBackgroundColor) } catch (e: Exception) { Color.parseColor(PanelPreferences.DEFAULT_PANEL_BG) }
        }
        val accentColorInt = try { Color.parseColor(panelPrefs.accentColor) } catch (e: Exception) { Color.parseColor(PanelPreferences.DEFAULT_ACCENT_COLOR) }

        binding.btnPickAccent.backgroundTintList = android.content.res.ColorStateList.valueOf(accentColorInt)
        binding.btnPickBg.backgroundTintList = android.content.res.ColorStateList.valueOf(effectiveBgColor)

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

        binding.btnResetBlur.setOnClickListener {
            val default = PanelPreferences.DEFAULT_BLUR_AMOUNT
            panelPrefs.blurAmount = default
            binding.sbBlurAmount.value = default.toFloat()
            binding.tvBlurAmountValue.text = "$default"
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
                .setTitle(R.string.app_theme)
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.themeMode = values[which]
                    binding.tvThemeModeValue.text = options[which]
                    applyAppTheme(this)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.layoutUIStyle.setOnClickListener {
            val options = arrayOf(getString(R.string.theme_origin), getString(R.string.theme_hyperos), getString(R.string.theme_realme), getString(R.string.theme_rich))
            val values = arrayOf(
                PanelPreferences.THEME_ORIGIN,
                PanelPreferences.THEME_HYPEROS,
                PanelPreferences.THEME_REALME,
                PanelPreferences.THEME_RICH
            )
            
            val selectedIndex = values.indexOf(panelPrefs.uiTheme).let { if (it == -1) 0 else it }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ui_style_theme)
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.uiTheme = values[which]
                    binding.tvUIStyleValue.text = options[which]
                    applyOnly()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.featureIconShape.setOnClickListener {
            val options = arrayOf(getString(R.string.icon_pack_default), getString(R.string.shape_circle), getString(R.string.shape_square), getString(R.string.shape_rounded), getString(R.string.shape_squircle))
            val values = arrayOf(
                PanelPreferences.SHAPE_SYSTEM,
                PanelPreferences.SHAPE_CIRCLE,
                PanelPreferences.SHAPE_SQUARE,
                PanelPreferences.SHAPE_ROUNDED,
                PanelPreferences.SHAPE_SQUIRCLE
            )

            val selectedIndex = values.indexOf(panelPrefs.iconShape).let { if (it == -1) 0 else it }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.icon_shape)
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.iconShape = values[which]
                    binding.tvIconShapeValue.text = options[which]
                    applyOnly()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
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

        binding.featureShowBottomArrow.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showBottomArrow = isChecked
            applyOnly()
        }

        binding.featureShowAppNamesMaster.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showAppNamesMaster = isChecked
            if (isChecked && !panelPrefs.showAppNamesSidebar && !panelPrefs.showAppNamesDrawerRaw) {
                panelPrefs.showAppNamesSidebar = true
                binding.featureShowAppNamesSidebar.isChecked = true
            }
            val transition = android.transition.TransitionSet().apply {
                ordering = android.transition.TransitionSet.ORDERING_TOGETHER
                addTransition(android.transition.ChangeBounds())
                addTransition(android.transition.Fade())
                duration = 320L
                interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
            }
            android.transition.TransitionManager.beginDelayedTransition(
                binding.appearanceScrollView as? android.view.ViewGroup ?: binding.layoutShowAppNames,
                transition
            )
            binding.layoutShowAppNamesSuboptions.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyOnly()
        }

        binding.layoutShowAppNamesMaster.setOnClickListener {
            binding.featureShowAppNamesMaster.toggle()
        }

        binding.featureShowAppNamesSidebar.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showAppNamesSidebar = isChecked
            applyOnly()
        }

        binding.featureShowAppNamesDrawer.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showAppNamesDrawerRaw = isChecked
            applyOnly()
        }

        binding.layoutShowAppNamesSidebar.setOnClickListener {
            binding.featureShowAppNamesSidebar.toggle()
        }

        binding.layoutShowAppNamesDrawer.setOnClickListener {
            binding.featureShowAppNamesDrawer.toggle()
        }

        binding.featureAutoClose.setOnClickListener {
            val options = arrayOf(getString(R.string.auto_close_never), getString(R.string.auto_close_3s), getString(R.string.auto_close_5s), getString(R.string.auto_close_10s))
            val values = arrayOf(0, 3, 5, 10)
            val currentIdx = values.indexOf(panelPrefs.autoCloseTimeout).coerceAtLeast(0)

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.appearance_auto_close)
                .setSingleChoiceItems(options, currentIdx) { dialog, which ->
                    panelPrefs.autoCloseTimeout = values[which]
                    binding.tvAutoCloseValue.text = options[which]
                    applyOnly()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.featureColumns.setOnClickListener {
            val options = arrayOf(getString(R.string.columns_1), getString(R.string.columns_2))
            val currentSelectedIndex = (panelPrefs.panelColumns - 1).coerceIn(0, 1)
            var newlySelectedIndex = currentSelectedIndex

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.panel_columns)
                .setSingleChoiceItems(options, currentSelectedIndex) { _, which ->
                    newlySelectedIndex = which
                }
                .setPositiveButton(R.string.btn_apply) { _, _ ->
                    val columns = newlySelectedIndex + 1
                    panelPrefs.panelColumns = columns
                    binding.tvColumnsValue.text = options[newlySelectedIndex]
                    applyOnly()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.featureCustomAccent.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.useCustomAccent = isChecked
            val alpha = if (isChecked) 1.0f else 0.5f
            binding.featureAccentColor.animate().alpha(alpha).setDuration(200).start()
            binding.btnResetAccent.isEnabled = isChecked
            applyOnly()
        }

        binding.featureCustomBg.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.useCustomPanelBg = isChecked
            val alpha = if (isChecked) 1.0f else 0.5f
            binding.featureBgColor.animate().alpha(alpha).setDuration(200).start()
            binding.btnResetBg.isEnabled = isChecked
            loadCurrentSettings()
            applyOnly()
        }

        val openIconPackPicker = {
            IconPackPickerDialog.show(this) {
                binding.tvCurrentIconPack.text = panelPrefs.iconPackLabel
            }
        }
        binding.btnSelectIconPack.setOnClickListener { openIconPackPicker() }
        binding.featureIconPack.setOnClickListener { openIconPackPicker() }

        binding.btnResetUIColors.setOnClickListener {
            panelPrefs.resetUIColors()
            loadCurrentSettings()
            applyOnly()
            binding.root.showModernToast(getString(R.string.toast_ui_colors_reset))
        }

        binding.btnPickAccent.setOnClickListener {
            val currentAccent = try { Color.parseColor(panelPrefs.accentColor) } catch (e: Exception) { Color.parseColor(PanelPreferences.DEFAULT_ACCENT_COLOR) }
            openColorPicker(currentAccent) { newColor ->
                val hex = String.format("#%06X", (0xFFFFFF and newColor))
                panelPrefs.accentColor = hex
                panelPrefs.useCustomAccent = true
                loadCurrentSettings()
                applyOnly()
            }
        }

        binding.btnResetAccent.setOnClickListener {
            panelPrefs.accentColor = PanelPreferences.DEFAULT_ACCENT_COLOR
            panelPrefs.useCustomAccent = PanelPreferences.DEFAULT_USE_CUSTOM_ACCENT
            loadCurrentSettings()
            applyOnly()
        }

        binding.featureAccentColor.setOnClickListener {
            binding.btnPickAccent.performClick()
        }

        binding.btnPickBg.setOnClickListener {
            val currentBg = try { Color.parseColor(panelPrefs.panelBackgroundColor) } catch (e: Exception) { Color.parseColor(PanelPreferences.DEFAULT_PANEL_BG) }
            openColorPicker(currentBg) { newColor ->
                val hex = String.format("#E6%06X", (0xFFFFFF and newColor))
                panelPrefs.panelBackgroundColor = hex
                panelPrefs.useCustomPanelBg = true
                loadCurrentSettings()
                applyOnly()
            }
        }

        binding.btnResetBg.setOnClickListener {
            panelPrefs.panelBackgroundColor = PanelPreferences.DEFAULT_PANEL_BG
            panelPrefs.useCustomPanelBg = PanelPreferences.DEFAULT_USE_CUSTOM_PANEL_BG
            loadCurrentSettings()
            applyOnly()
        }

        binding.featureBgColor.setOnClickListener {
            binding.btnPickBg.performClick()
        }

        binding.featureHomeButton.setOnClickListener {
            val options = arrayOf(getString(R.string.button_style_power), getString(R.string.button_style_logo))
            val values = arrayOf(PanelPreferences.STYLE_POWER, PanelPreferences.STYLE_CLASSIC)
            val selectedIndex = values.indexOf(panelPrefs.homeButtonStyle).let { if (it == -1) 0 else it }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.service_button_style)
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.homeButtonStyle = values[which]
                    binding.tvHomeButtonStyleValue.text = options[which]
                    applyOnly()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
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
