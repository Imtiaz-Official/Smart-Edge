package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsHandleBinding
import com.google.android.material.slider.Slider
import yuku.ambilwarna.AmbilWarnaDialog

class HandleSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsHandleBinding
    private lateinit var panelPrefs: PanelPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsHandleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color and icons
        val typedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window.statusBarColor = typedValue.data
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        panelPrefs = PanelPreferences(this)
        
        loadCurrentSettings()
        setupListeners()
    }

    private fun loadCurrentSettings() {
        binding.switchShowPill.isChecked = panelPrefs.showPill
        
        binding.sbHandleHeight.value = panelPrefs.handleHeight.toFloat()
        binding.tvHeightValue.text = "${panelPrefs.handleHeight}dp"
        
        binding.sbHandleWidth.value = panelPrefs.handleWidth.toFloat()
        binding.tvWidthValue.text = "${panelPrefs.handleWidth}dp"
        
        binding.sbHandleOffset.value = (panelPrefs.handleVerticalOffset + 100).toFloat()
        binding.tvOffsetValue.text = "${panelPrefs.handleVerticalOffset}dp"

        try {
            val color = Color.parseColor(panelPrefs.pillColor)
            binding.btnPickPillColor.backgroundTintList = ColorStateList.valueOf(color)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupListeners() {
        binding.btnPickPillColor.setOnClickListener {
            openColorPicker(Color.parseColor(panelPrefs.pillColor)) { newColor ->
                val hex = String.format("#%06X", (0xFFFFFF and newColor))
                panelPrefs.pillColor = hex
                try {
                    binding.btnPickPillColor.backgroundTintList = ColorStateList.valueOf(newColor)
                } catch (e: Exception) {}
                applyOnly()
            }
        }

        binding.switchShowPill.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showPill = isChecked
            applyOnly()
        }

        binding.sbHandleHeight.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                panelPrefs.handleHeight = progress
                binding.tvHeightValue.text = "${progress}dp"
            }
        }
        binding.sbHandleHeight.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
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
        binding.sbHandleWidth.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                applyOnly()
            }
        })

        binding.sbHandleOffset.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                val offset = progress - 100
                panelPrefs.handleVerticalOffset = offset
                binding.tvOffsetValue.text = "${offset}dp"
            }
        }
        binding.sbHandleOffset.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                applyAndShow() // Vertical offset needs a full WindowManager update
            }
        })
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
