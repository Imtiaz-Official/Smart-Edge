package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsHandleBinding

class HandleSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsHandleBinding
    private lateinit var panelPrefs: PanelPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsHandleBinding.inflate(layoutInflater)
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

    private fun handleDeepLink() {
        val targetId = intent.getStringExtra(SettingsMainActivity.EXTRA_SCROLL_TO) ?: return
        val viewId = resources.getIdentifier(targetId, "id", packageName)
        if (viewId != 0) {
            val targetView = findViewById<View>(viewId)
            targetView?.post {
                val rect = android.graphics.Rect()
                targetView.getDrawingRect(rect)
                binding.root.offsetDescendantRectToMyCoords(targetView, rect)
                binding.handleScrollView.smoothScrollTo(0, rect.top - 200)
                targetView.highlightView()
            }
        }
    }

    private fun loadCurrentSettings() {
        binding.featureShowPill.isChecked = panelPrefs.showPill
        updatePillColorUI()
        
        binding.sbPillWidth.value = panelPrefs.pillWidth.toFloat()
        binding.tvPillWidthValue.text = "${panelPrefs.pillWidth}dp"

        binding.sbHandleWidth.value = panelPrefs.handleWidth.toFloat()
        binding.tvHandleWidthValue.text = "${panelPrefs.handleWidth}dp"
        
        binding.sbHandleHeight.value = panelPrefs.handleHeight.toFloat()
        binding.tvHeightValue.text = "${panelPrefs.handleHeight}dp"
        
        binding.sbHandleOffset.value = panelPrefs.handleVerticalOffset.toFloat()
        binding.tvOffsetValue.text = "${panelPrefs.handleVerticalOffset}dp"
    }

    private fun updatePillColorUI() {
        try {
            val color = android.graphics.Color.parseColor(panelPrefs.pillColor)
            binding.btnPickPillColor.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupListeners() {
        binding.btnPickPillColor.setOnClickListener {
            val currentColor = try {
                android.graphics.Color.parseColor(panelPrefs.pillColor)
            } catch (e: Exception) {
                android.graphics.Color.WHITE
            }
            
            val picker = yuku.ambilwarna.AmbilWarnaDialog(this, currentColor, object : yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: yuku.ambilwarna.AmbilWarnaDialog?) {}
                override fun onOk(dialog: yuku.ambilwarna.AmbilWarnaDialog?, color: Int) {
                    val hex = String.format("#%08X", color)
                    panelPrefs.pillColor = hex
                    updatePillColorUI()
                    applyOnly()
                }
            })
            picker.show()
        }

        binding.btnResetPillColor.setOnClickListener {
            panelPrefs.pillColor = PanelPreferences.DEFAULT_PILL_COLOR
            updatePillColorUI()
            applyOnly()
        }

        binding.sbPillWidth.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                panelPrefs.pillWidth = progress
                binding.tvPillWidthValue.text = "${progress}dp"
            }
        }
        binding.sbPillWidth.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                applyOnly()
            }
        })

        binding.btnResetPillWidth.setOnClickListener {
            val default = 5
            panelPrefs.pillWidth = default
            binding.sbPillWidth.value = default.toFloat()
            binding.tvPillWidthValue.text = "${default}dp"
            applyOnly()
        }

        binding.sbHandleWidth.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                panelPrefs.handleWidth = progress
                binding.tvHandleWidthValue.text = "${progress}dp"
            }
        }
        binding.sbHandleWidth.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                applyAndShow() // Touch area change needs a full WindowManager update
            }
        })

        binding.btnResetHandleWidth.setOnClickListener {
            val default = 32
            panelPrefs.handleWidth = default
            binding.sbHandleWidth.value = default.toFloat()
            binding.tvHandleWidthValue.text = "${default}dp"
            applyAndShow()
        }

        binding.featureShowPill.setOnCheckedChangeListener { _, isChecked ->
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
