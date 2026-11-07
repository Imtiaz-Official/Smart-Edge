package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsHandleBinding

class HandleSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsHandleBinding
    private lateinit var panelPrefs: PanelPreferences

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsHandleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        panelPrefs = PanelPreferences(this)
        
        loadCurrentSettings()
        setupListeners()
        handleDeepLink()
    }

    private fun handleDeepLink() {
        val targetId = intent.getStringExtra(SettingsMainActivity.EXTRA_SCROLL_TO) ?: return
        val viewId = resources.getIdentifier(targetId, "id", packageName)
        if (viewId != 0) {
            val targetView = findViewById<View>(viewId)
            targetView?.let { binding.handleScrollView.scrollToTargetView(it) }
        }
    }

    private fun loadCurrentSettings() {
        binding.featureShowPill.isChecked = panelPrefs.showPill
        
        updatePillColorUI()
        
        binding.sbPillThickness.value = panelPrefs.pillWidth.toFloat()
        binding.tvThicknessValue.text = "${panelPrefs.pillWidth}dp"
        
        binding.sbTriggerWidth.value = panelPrefs.handleWidth.toFloat()
        binding.tvWidthValue.text = "${panelPrefs.handleWidth}dp"
        
        binding.sbHandleHeight.value = panelPrefs.handleHeight.toFloat()
        binding.tvHeightValue.text = "${panelPrefs.handleHeight}dp"
        
        val offset = panelPrefs.handleVerticalOffset.toFloat().coerceIn(-500f, 500f)
        binding.sbHandlePos.value = offset
        binding.tvPosValue.text = "${offset.toInt()}dp"
    }

    private fun updatePillColorUI() {
        try {
            val color = android.graphics.Color.parseColor(panelPrefs.pillColor)
            binding.btnPickPillColor.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            // Adjust icon tint for contrast
            val l = androidx.core.graphics.ColorUtils.calculateLuminance(color)
            binding.btnPickPillColor.iconTint = android.content.res.ColorStateList.valueOf(if (l > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        } catch (e: Exception) {
            binding.btnPickPillColor.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        }
    }

    private fun setupListeners() {
        binding.featureShowPill.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showPill = isChecked
            applyOnly()
        }

        binding.btnPickPillColor.setOnClickListener {
            val currentColor = try {
                android.graphics.Color.parseColor(panelPrefs.pillColor)
            } catch (e: Exception) {
                android.graphics.Color.WHITE
            }

            openColorPicker(currentColor) { color ->
                val hex = String.format("#%08X", color)
                panelPrefs.pillColor = hex
                updatePillColorUI()
                applyOnly()
            }
        }

        binding.btnResetPillColor.setOnClickListener {
            panelPrefs.pillColor = PanelPreferences.DEFAULT_PILL_COLOR
            updatePillColorUI()
            applyOnly()
        }

        binding.sbPillThickness.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                panelPrefs.pillWidth = value.toInt()
                binding.tvThicknessValue.text = "${value.toInt()}dp"
            }
        }
        binding.sbPillThickness.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) { applyOnly() }
        })

        binding.btnResetThickness.setOnClickListener {
            val default = PanelPreferences.DEFAULT_PILL_WIDTH
            panelPrefs.pillWidth = default
            binding.sbPillThickness.value = default.toFloat()
            binding.tvThicknessValue.text = "${default}dp"
            applyOnly()
        }

        binding.sbTriggerWidth.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                panelPrefs.handleWidth = value.toInt()
                binding.tvWidthValue.text = "${value.toInt()}dp"
            }
        }
        binding.sbTriggerWidth.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) { applyOnly() }
        })

        binding.btnResetWidth.setOnClickListener {
            val default = PanelPreferences.DEFAULT_HANDLE_WIDTH
            panelPrefs.handleWidth = default
            binding.sbTriggerWidth.value = default.toFloat()
            binding.tvWidthValue.text = "${default}dp"
            applyOnly()
        }

        binding.sbHandleHeight.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                panelPrefs.handleHeight = value.toInt()
                binding.tvHeightValue.text = "${value.toInt()}dp"
            }
        }
        binding.sbHandleHeight.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) { applyOnly() }
        })

        binding.btnResetHeight.setOnClickListener {
            val default = PanelPreferences.DEFAULT_HANDLE_HEIGHT
            panelPrefs.handleHeight = default
            binding.sbHandleHeight.value = default.toFloat()
            binding.tvHeightValue.text = "${default}dp"
            applyOnly()
        }

        binding.sbHandlePos.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                panelPrefs.handleVerticalOffset = value.toInt()
                binding.tvPosValue.text = "${value.toInt()}dp"
            }
        }
        binding.sbHandlePos.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) { applyOnly() }
        })

        binding.btnResetPos.setOnClickListener {
            val default = 0
            panelPrefs.handleVerticalOffset = default
            binding.sbHandlePos.value = default.toFloat()
            binding.tvPosValue.text = "${default}dp"
            applyOnly()
        }
    }

    private fun applyOnly() {
        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_REFRESH
        }
        startService(intent)
    }
}
