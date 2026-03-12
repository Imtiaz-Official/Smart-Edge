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
    private var previewPanel: SidePanelView? = null
    private var previewHandle: EdgeHandleView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = getString(R.string.settings_title)
            setDisplayHomeAsUpEnabled(true)
        }

        panelPrefs = PanelPreferences(this)
        
        setupPreview()
        loadCurrentSettings()
        setupListeners()
    }

    private fun setupPreview() {
        binding.previewContainer.removeAllViews()

        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        val density = resources.displayMetrics.density

        // 1. Create Preview Handle
        previewHandle = EdgeHandleView(this).apply {
            this.isRightSide = isRight
            this.showPill = panelPrefs.showPill
            this.onTrigger = null 
            updatePill()
            visibility = if (panelPrefs.gesturesEnabled) View.VISIBLE else View.GONE
        }

        val handleWidthPx = (panelPrefs.handleWidth * density).toInt()
        val handleHeightPx = (panelPrefs.handleHeight * density).toInt()
        
        val handleParams = FrameLayout.LayoutParams(handleWidthPx, handleHeightPx).apply {
            gravity = if (isRight) Gravity.END or Gravity.CENTER_VERTICAL else Gravity.START or Gravity.CENTER_VERTICAL
        }
        previewHandle?.translationY = (panelPrefs.handleVerticalOffset * density)
        binding.previewContainer.addView(previewHandle, handleParams)

        // 2. Create Preview Panel
        previewPanel = SidePanelView(this).apply {
            onClose = null
            onAppsChanged = null
            onAddClick = null
            
            setApps(listOf(
                AppInfo("pkg1", "Browser", getDrawable(android.R.drawable.ic_menu_compass), true),
                AppInfo("pkg2", "Camera", getDrawable(android.R.drawable.ic_menu_camera), true),
                AppInfo("pkg3", "Gallery", getDrawable(android.R.drawable.ic_menu_gallery), true),
                AppInfo("pkg4", "Maps", getDrawable(android.R.drawable.ic_dialog_map), true),
                AppInfo("pkg5", "Settings", getDrawable(android.R.drawable.ic_menu_preferences), true)
            ))
            
            alpha = 1.0f
            visibility = View.VISIBLE
        }

        val panelParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.MATCH_PARENT 
        ).apply {
            gravity = if (isRight) Gravity.END else Gravity.START
            setMargins(if (isRight) 0 else 16, 0, if (isRight) 16 else 0, 0)
        }
        previewPanel?.translationY = (panelPrefs.handleVerticalOffset * density)
        binding.previewContainer.addView(previewPanel, panelParams)
    }

    private fun updatePreview() {
        setupPreview()
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
        binding.sbOpacity.progress = panelPrefs.panelOpacity
        binding.sbHandleHeight.progress = panelPrefs.handleHeight
        binding.sbHandleWidth.progress = panelPrefs.handleWidth
        binding.sbHandleOffset.progress = panelPrefs.handleVerticalOffset + 100

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

        val pack = panelPrefs.selectedIconPack
        binding.tvCurrentIconPack.text = if (pack == "none") "System Default" else pack

        // Sync Color Picker Previews (Make them accurate)
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
            updatePreview()
            restartServiceIfRunning()
        }

        binding.switchAutoStart.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.autoStart = isChecked
        }

        binding.switchGestures.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.gesturesEnabled = isChecked
            updatePreview()
            restartServiceIfRunning()
        }

        binding.switchTapOpen.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.tapToOpen = isChecked
            restartServiceIfRunning()
        }

        binding.switchShowPill.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showPill = isChecked
            updatePreview()
            restartServiceIfRunning()
        }

        binding.switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.hapticEnabled = isChecked
        }

        binding.sbOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    panelPrefs.panelOpacity = progress
                    updatePreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { restartServiceIfRunning() }
        })

        binding.sbHandleHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    panelPrefs.handleHeight = progress
                    updatePreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { restartServiceIfRunning() }
        })

        binding.sbHandleWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    panelPrefs.handleWidth = progress
                    updatePreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { restartServiceIfRunning() }
        })

        binding.btnGoPremium.setOnClickListener {
            panelPrefs.isPremium = true
            updatePremiumUI()
            Toast.makeText(this, "Welcome to Premium!", Toast.LENGTH_SHORT).show()
        }

        binding.sbHandleOffset.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    panelPrefs.handleVerticalOffset = progress - 100
                    updatePreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { restartServiceIfRunning() }
        })

        binding.rgThemes.setOnCheckedChangeListener { _, checkedId ->
            panelPrefs.uiTheme = when (checkedId) {
                R.id.rbThemeHyper -> PanelPreferences.THEME_HYPEROS
                R.id.rbThemeRealme -> PanelPreferences.THEME_REALME
                R.id.rbThemeRich -> PanelPreferences.THEME_RICH
                else -> PanelPreferences.THEME_ORIGIN
            }
            updatePreview()
            restartServiceIfRunning()
        }

        binding.rgIconShape.setOnCheckedChangeListener { _, checkedId ->
            panelPrefs.iconShape = when (checkedId) {
                R.id.rbShapeSquircle -> PanelPreferences.SHAPE_SQUIRCLE
                R.id.rbShapeSquare -> PanelPreferences.SHAPE_SQUARE
                else -> PanelPreferences.SHAPE_CIRCLE
            }
            updatePreview()
            restartServiceIfRunning()
        }

        binding.btnSelectIconPack.setOnClickListener {
            val intent = Intent(this, IconPackActivity::class.java)
            startActivity(intent)
        }

        binding.switchTools.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showTools = isChecked
            updatePreview()
            restartServiceIfRunning()
        }

        binding.switchHideBg.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.hideBackground = isChecked
            updatePreview()
            restartServiceIfRunning()
        }

        binding.btnResetDefaults.setOnClickListener {
            panelPrefs.resetToDefaults()
            loadCurrentSettings() 
            updatePreview()
            restartServiceIfRunning()
            Toast.makeText(this, "Settings Reset to Defaults", Toast.LENGTH_SHORT).show()
        }

        binding.btnPickAccent.setOnClickListener {
            openColorPicker(Color.parseColor(panelPrefs.accentColor)) { newColor ->
                val hex = String.format("#%06X", (0xFFFFFF and newColor))
                panelPrefs.accentColor = hex
                updatePreview()
                restartServiceIfRunning()
            }
        }

        binding.btnPickBg.setOnClickListener {
            openColorPicker(Color.parseColor(panelPrefs.panelBackgroundColor)) { newColor ->
                val hex = String.format("#E6%06X", (0xFFFFFF and newColor))
                panelPrefs.panelBackgroundColor = hex
                updatePreview()
                restartServiceIfRunning()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadCurrentSettings() 
        restartServiceIfRunning()
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

    private fun updateColor(hex: String) {
        if (!panelPrefs.isPremium) {
            Toast.makeText(this, "Premium Required", Toast.LENGTH_SHORT).show()
            return
        }
        panelPrefs.panelBackgroundColor = hex
        updatePreview()
        restartServiceIfRunning()
    }

    private fun restartServiceIfRunning() {
        val stop = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_STOP
        }
        startService(stop)
        binding.root.postDelayed({
            val start = Intent(this, FloatingPanelService::class.java)
            startForegroundService(start)
        }, 400)
    }
}
