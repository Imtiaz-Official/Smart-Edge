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

/**
 * Settings screen for panel configuration.
 * Includes real-time preview and premium theme selections.
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
        
        val tv = android.widget.TextView(this).apply {
            text = "Live Display Simulation"
            setTextColor(Color.parseColor("#B3FFFFFF"))
            textSize = 10f
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM
                bottomMargin = 8
            }
        }
        binding.previewContainer.addView(tv)
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
        binding.switchShowPill.isChecked = panelPrefs.showPill
        binding.switchHaptic.isChecked = panelPrefs.hapticEnabled
        binding.sbOpacity.progress = panelPrefs.panelOpacity
        binding.sbHandleHeight.progress = panelPrefs.handleHeight
        binding.sbHandleWidth.progress = panelPrefs.handleWidth
        binding.sbHandleOffset.progress = panelPrefs.handleVerticalOffset + 100

        if (panelPrefs.panelColumns == 2) {
            binding.rgColumns.check(R.id.rbCol2)
        } else {
            binding.rgColumns.check(R.id.rbCol1)
        }

        when (panelPrefs.uiTheme) {
            PanelPreferences.THEME_HYPEROS -> binding.rgThemes.check(R.id.rbThemeHyper)
            PanelPreferences.THEME_REALME -> binding.rgThemes.check(R.id.rbThemeRealme)
            PanelPreferences.THEME_RICH -> binding.rgThemes.check(R.id.rbThemeRich)
            else -> binding.rgThemes.check(R.id.rbThemeOrigin)
        }

        // Icon Shape
        when (panelPrefs.iconShape) {
            PanelPreferences.SHAPE_SQUIRCLE -> binding.rgIconShape.check(R.id.rbShapeSquircle)
            PanelPreferences.SHAPE_SQUARE -> binding.rgIconShape.check(R.id.rbShapeSquare)
            else -> binding.rgIconShape.check(R.id.rbShapeCircle)
        }

        binding.sbPanelRadius.progress = panelPrefs.panelCornerRadius
        binding.switchTools.isChecked = panelPrefs.showTools
        binding.switchHideBg.isChecked = panelPrefs.hideBackground

        updatePremiumUI()
    }

    private fun updatePremiumUI() {
        val isPremium = panelPrefs.isPremium
        binding.sbHandleOffset.isEnabled = isPremium
        binding.rgColumns.isEnabled = isPremium
        binding.rbCol1.isEnabled = isPremium
        binding.rbCol2.isEnabled = isPremium
        binding.rgThemes.isEnabled = isPremium
        binding.rbThemeOrigin.isEnabled = isPremium
        binding.rbThemeHyper.isEnabled = isPremium
        binding.rbThemeRealme.isEnabled = isPremium
        binding.rbThemeRich.isEnabled = isPremium
        
        binding.rgIconShape.isEnabled = isPremium
        binding.rbShapeCircle.isEnabled = isPremium
        binding.rbShapeSquircle.isEnabled = isPremium
        binding.rbShapeSquare.isEnabled = isPremium

        binding.sbPanelRadius.isEnabled = isPremium
        binding.switchTools.isEnabled = isPremium
        binding.switchHideBg.isEnabled = isPremium

        if (isPremium) {
            binding.tvPremiumStatus.text = "Premium Active"
            binding.btnGoPremium.visibility = View.GONE
            binding.cardPremium.setCardBackgroundColor(Color.parseColor("#1A00C853"))
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

        binding.rgColumns.setOnCheckedChangeListener { _, checkedId ->
            panelPrefs.panelColumns = if (checkedId == R.id.rbCol2) 2 else 1
            updatePreview()
            restartServiceIfRunning()
        }

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

        binding.sbPanelRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    panelPrefs.panelCornerRadius = progress
                    updatePreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { restartServiceIfRunning() }
        })

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

        binding.btnColorDark.setOnClickListener { updateColor("#E61A1C1E") }
        binding.btnColorBlue.setOnClickListener { updateColor("#E60D47A1") }
        binding.btnColorRed.setOnClickListener { updateColor("#E6B71C1C") }
        binding.btnColorGreen.setOnClickListener { updateColor("#E61B5E20") }
        binding.btnColorPurple.setOnClickListener { updateColor("#E64A148C") }
    }

    private fun updateColor(hex: String) {
        if (!panelPrefs.isPremium) {
            Toast.makeText(this, "Custom colors require Premium", Toast.LENGTH_SHORT).show()
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
