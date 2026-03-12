package com.originpanel.sidepanel

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
        // Create a mini version of our SidePanelView for the preview area
        previewPanel = SidePanelView(this).apply {
            // Disable actual logic for preview
            onClose = null
            onAppsChanged = null
            onAddClick = null
            
            // Set some dummy apps for visual
            setApps(listOf(
                AppInfo("pkg1", "App 1", getDrawable(android.R.drawable.ic_menu_camera), true),
                AppInfo("pkg2", "App 2", getDrawable(android.R.drawable.ic_menu_gallery), true)
            ))
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.CENTER
        }
        
        binding.previewContainer.removeAllViews()
        binding.previewContainer.addView(previewPanel, params)
    }

    private fun updatePreview() {
        setupPreview()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun loadCurrentSettings() {
        // Panel side
        if (panelPrefs.panelSide == PanelPreferences.SIDE_LEFT) {
            binding.rgPanelSide.check(R.id.rbLeft)
        } else {
            binding.rgPanelSide.check(R.id.rbRight)
        }

        // Auto-start
        binding.switchAutoStart.isChecked = panelPrefs.autoStart

        // Show Pill
        binding.switchShowPill.isChecked = panelPrefs.showPill

        // Haptic Feedback
        binding.switchHaptic.isChecked = panelPrefs.hapticEnabled

        // Opacity
        binding.sbOpacity.progress = panelPrefs.panelOpacity

        // Handle Height
        binding.sbHandleHeight.progress = panelPrefs.handleHeight

        // Handle Width
        binding.sbHandleWidth.progress = panelPrefs.handleWidth

        // Premium Vertical Offset
        binding.sbHandleOffset.progress = panelPrefs.handleVerticalOffset + 100

        // Premium Columns
        if (panelPrefs.panelColumns == 2) {
            binding.rgColumns.check(R.id.rbCol2)
        } else {
            binding.rgColumns.check(R.id.rbCol1)
        }

        // Premium Themes
        when (panelPrefs.uiTheme) {
            PanelPreferences.THEME_HYPEROS -> binding.rgThemes.check(R.id.rbThemeHyper)
            PanelPreferences.THEME_REALME -> binding.rgThemes.check(R.id.rbThemeRealme)
            PanelPreferences.THEME_RICH -> binding.rgThemes.check(R.id.rbThemeRich)
            else -> binding.rgThemes.check(R.id.rbThemeOrigin)
        }

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
            restartServiceIfRunning()
        }

        binding.switchAutoStart.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.autoStart = isChecked
        }

        binding.switchShowPill.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showPill = isChecked
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
                if (fromUser) panelPrefs.handleHeight = progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { restartServiceIfRunning() }
        })

        binding.sbHandleWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) panelPrefs.handleWidth = progress
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
                if (fromUser) panelPrefs.handleVerticalOffset = progress - 100
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
