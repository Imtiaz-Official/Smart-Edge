package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsInteractionBinding
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InteractionSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsInteractionBinding
    private lateinit var panelPrefs: PanelPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsInteractionBinding.inflate(layoutInflater)
        setContentView(binding.root)



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
        updateSecureSettingsUI()
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
                binding.interactionScrollView.smoothScrollTo(0, rect.top - 200)
                targetView.highlightView()
                
                // If fixing freeform from MainActivity, try direct toggle first
                if (targetId == "feature_freeform" && !isFreeformEnabled()) {
                    if (putGlobalSetting("freeform_window_management", 1)) {
                        binding.root.showModernToast("System Freeform Mode Enabled")
                        return@post
                    }
                    
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        "System freeform mode is disabled in Developer Options",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).setAction("FIX") {
                        openFreeformDeveloperSettings()
                    }.show()
                }
            }
        }
    }

    private fun loadCurrentSettings() {
        updateSecureSettingsUI()
        if (panelPrefs.panelSide == PanelPreferences.SIDE_LEFT) {
            binding.featurePanelSide.check(R.id.rbLeft)
        } else {
            binding.featurePanelSide.check(R.id.rbRight)
        }

        binding.featureAutoStart.isChecked = panelPrefs.autoStart
        binding.featureGestures.isChecked = panelPrefs.gesturesEnabled
        binding.tvTapGesturesValue.text = when {
            panelPrefs.tripleTapToOpen -> "Triple Tap"
            panelPrefs.doubleTapToOpen -> "Double Tap"
            panelPrefs.tapToOpen -> "Single Tap"
            else -> "Disabled"
        }
        binding.featureHaptic.isChecked = panelPrefs.hapticEnabled
        binding.featureShowLandscape.isChecked = panelPrefs.showInLandscape
        binding.featureFreeform.isChecked = panelPrefs.freeformEnabled
        binding.featureNotificationApps.isChecked = panelPrefs.showNotificationApps
        binding.featureDragSplit.isChecked = panelPrefs.dragToSplit
        binding.featureRememberScroll.isChecked = panelPrefs.rememberScroll
        binding.featureAutoShowKeyboard.isChecked = panelPrefs.autoShowKeyboard
        binding.featureShowLogs.isChecked = panelPrefs.showLogs

        val gameAppsCount = panelPrefs.getGameApps().size
        binding.tvGameAppsValue.text = if (gameAppsCount == 1) "1 app selected" else "$gameAppsCount apps selected"

        // Window size picker — visible only when freeform is on
        val freeformOn = panelPrefs.freeformEnabled
        binding.layoutFreeformSize.visibility = if (freeformOn) View.VISIBLE else View.GONE
        binding.tvFreeformSizeValue.text = freeformModeLabel(panelPrefs.freeformWindowMode)

        // Custom size sliders — only visible when Custom mode is active
        val customVisible = freeformOn && panelPrefs.freeformWindowMode == PanelPreferences.FREEFORM_MODE_CUSTOM
        binding.layoutFreeformCustom.visibility = if (customVisible) View.VISIBLE else View.GONE
        binding.sbFreeformCustomW.value = panelPrefs.freeformCustomWidth.toFloat()
        binding.sbFreeformCustomH.value = panelPrefs.freeformCustomHeight.toFloat()
        binding.tvFreeformCustomW.text = "${panelPrefs.freeformCustomWidth}%"
        binding.tvFreeformCustomH.text = "${panelPrefs.freeformCustomHeight}%"

        val animSpeed = panelPrefs.animSpeed
        binding.tvAnimFeelValue.text = when (animSpeed) {
            200 -> "Calm (Slow)"
            400 -> "Balanced (Default)"
            700 -> "Snappy"
            1000 -> "Instant"
            0 -> "Disabled"
            else -> "Balanced (Default)"
        }

        binding.sbPickerGap.value = panelPrefs.pickerGap.toFloat()
        binding.tvPickerGapValue.text = "${panelPrefs.pickerGap}dp"
    }

    private fun updateSecureSettingsUI() {
        val hasPermission = checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            binding.tvSecureSettingsStatus.text = "Granted"
            binding.tvSecureSettingsStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            binding.tvSecureSettingsStatus.text = "Not Granted"
            binding.tvSecureSettingsStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    private fun setupListeners() {
        binding.btnSecureSettings.setOnClickListener {
            SecureSettingsDialog.show(this) {
                updateSecureSettingsUI()
            }
        }

        binding.featurePanelSide.setOnCheckedChangeListener { _, checkedId ->
            panelPrefs.panelSide = if (checkedId == R.id.rbLeft)
                PanelPreferences.SIDE_LEFT else PanelPreferences.SIDE_RIGHT
            applyAndShow()
        }

        binding.featureAutoStart.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.autoStart = isChecked
        }

        binding.featureGestures.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.gesturesEnabled = isChecked
            applyOnly()
        }

        binding.layoutTapGestures.setOnClickListener {
            val options = arrayOf("Disabled", "Single Tap", "Double Tap", "Triple Tap")
            var selectedIndex = 0
            if (panelPrefs.tapToOpen) selectedIndex = 1
            if (panelPrefs.doubleTapToOpen) selectedIndex = 2
            if (panelPrefs.tripleTapToOpen) selectedIndex = 3

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Tap to Open")
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.tapToOpen = (which == 1)
                    panelPrefs.doubleTapToOpen = (which == 2)
                    panelPrefs.tripleTapToOpen = (which == 3)
                    binding.tvTapGesturesValue.text = options[which]
                    applyOnly()
                    dialog.dismiss()
                }
                .show()
        }

        binding.featureHaptic.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.hapticEnabled = isChecked
        }

        binding.featureShowLandscape.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showInLandscape = isChecked
            applyOnly()
        }

        binding.featureFreeform.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.freeformEnabled = isChecked
            binding.layoutFreeformSize.visibility = if (isChecked) View.VISIBLE else View.GONE
            // Hide custom sliders when freeform is turned off
            if (!isChecked) binding.layoutFreeformCustom.visibility = View.GONE
            if (isChecked && !isFreeformEnabled()) {
                // 1. Try direct toggle (requires WRITE_SECURE_SETTINGS)
                val success1 = putGlobalSetting("freeform_window_management", 1)
                val success2 = putGlobalSetting("force_resizable_activities", 1)

                if (success1 || success2) {
                    binding.root.showModernToast("System Freeform Mode Enabled")
                    return@setOnCheckedChangeListener
                }

                // 2. Fallback to Deep-link
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    "System freeform mode is disabled in Developer Options",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).setAction("FIX") {
                    openFreeformDeveloperSettings()
                }.show()
            }
        }

        binding.featureNotificationApps.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                val enabledListeners = android.provider.Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
                if (enabledListeners?.contains(packageName) != true) {
                    buttonView.isChecked = false
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Permission Required")
                        .setMessage("Smart Edge needs Notification Access to see which apps have active notifications so they can be shown in the panel.")
                        .setPositiveButton("Grant") { _, _ ->
                            try {
                                val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                                        putExtra(
                                            android.provider.Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                                            android.content.ComponentName(this@InteractionSettingsActivity, NotificationTrackingService::class.java).flattenToString()
                                        )
                                    }
                                } else {
                                    android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                }
                                startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback just in case OEM broke the detail intent
                                startActivity(android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    return@setOnCheckedChangeListener
                }
            }
            panelPrefs.showNotificationApps = isChecked
            applyOnly()
        }

        binding.featureDragSplit.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.dragToSplit = isChecked
            applyOnly()
        }

        binding.featureRememberScroll.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.rememberScroll = isChecked
            applyOnly()
        }

        binding.featureAutoShowKeyboard.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.autoShowKeyboard = isChecked
            applyOnly()
        }

        binding.layoutFreeformSize.setOnClickListener {
            val options = arrayOf("Standard (80%)", "Portrait (Narrow)", "Maximized", "Custom…")
            val values = arrayOf(
                PanelPreferences.FREEFORM_MODE_STANDARD,
                PanelPreferences.FREEFORM_MODE_PORTRAIT,
                PanelPreferences.FREEFORM_MODE_MAXIMIZED,
                PanelPreferences.FREEFORM_MODE_CUSTOM
            )
            val selectedIndex = values.indexOf(panelPrefs.freeformWindowMode).coerceAtLeast(0)

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Window Size")
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    val mode = values[which]
                    panelPrefs.freeformWindowMode = mode
                    binding.tvFreeformSizeValue.text = freeformModeLabel(mode)
                    // Show/hide custom sliders immediately
                    val isCustom = mode == PanelPreferences.FREEFORM_MODE_CUSTOM
                    binding.layoutFreeformCustom.visibility = if (isCustom) View.VISIBLE else View.GONE
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Custom Width slider
        binding.sbFreeformCustomW.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val pct = value.toInt()
                panelPrefs.freeformCustomWidth = pct
                binding.tvFreeformCustomW.text = "$pct%"
                // Refresh the subtitle so it reflects the new size
                binding.tvFreeformSizeValue.text = freeformModeLabel(PanelPreferences.FREEFORM_MODE_CUSTOM)
            }
        }
        binding.btnResetFreeformW.setOnClickListener {
            val default = 80
            panelPrefs.freeformCustomWidth = default
            binding.sbFreeformCustomW.value = default.toFloat()
            binding.tvFreeformCustomW.text = "$default%"
            binding.tvFreeformSizeValue.text = freeformModeLabel(PanelPreferences.FREEFORM_MODE_CUSTOM)
        }

        // Custom Height slider
        binding.sbFreeformCustomH.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val pct = value.toInt()
                panelPrefs.freeformCustomHeight = pct
                binding.tvFreeformCustomH.text = "$pct%"
                binding.tvFreeformSizeValue.text = freeformModeLabel(PanelPreferences.FREEFORM_MODE_CUSTOM)
            }
        }
        binding.btnResetFreeformH.setOnClickListener {
            val default = 80
            panelPrefs.freeformCustomHeight = default
            binding.sbFreeformCustomH.value = default.toFloat()
            binding.tvFreeformCustomH.text = "$default%"
            binding.tvFreeformSizeValue.text = freeformModeLabel(PanelPreferences.FREEFORM_MODE_CUSTOM)
        }

        binding.featureShowLogs.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showLogs = isChecked
        }

        binding.featureGameApps.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                // Show a loading dialog while fetching apps on the IO thread
                val loadingDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this@InteractionSettingsActivity)
                    .setTitle("Loading Apps...")
                    .setMessage("Please wait while apps are being categorized.")
                    .setCancelable(false)
                    .show()

                val allApps = withContext(Dispatchers.IO) { AppRepository(this@InteractionSettingsActivity).getAllApps() }
                loadingDialog.dismiss()

                val sortedApps = allApps.sortedBy { it.appName.lowercase() }
                val appNames = sortedApps.map { it.appName }.toTypedArray()
                val pkgNames = sortedApps.map { it.packageName }.toTypedArray()

                val currentSelected = panelPrefs.getGameApps()
                val checkedItems = BooleanArray(sortedApps.size) { i ->
                    currentSelected.contains(pkgNames[i])
                }

                com.google.android.material.dialog.MaterialAlertDialogBuilder(this@InteractionSettingsActivity)
                    .setTitle("Select Game Mode Apps")
                    .setMultiChoiceItems(appNames, checkedItems) { _, which, isChecked ->
                        checkedItems[which] = isChecked
                    }
                    .setPositiveButton("Save") { _, _ ->
                        val newSelected = pkgNames.filterIndexed { index, _ -> checkedItems[index] }
                        panelPrefs.setGameApps(newSelected)
                        binding.tvGameAppsValue.text = if (newSelected.size == 1) "1 app selected" else "${newSelected.size} apps selected"
                        applyOnly() // Restart service to apply changes
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        binding.featureAnimFeel.setOnClickListener {
            val options = arrayOf("Calm (Slow)", "Balanced (Default)", "Snappy", "Instant", "Disabled")
            val values = intArrayOf(200, 400, 700, 1000, 0)
            
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
                applyOnly()
            }
        })

        binding.btnResetPickerGap.setOnClickListener {
            val default = 20
            panelPrefs.pickerGap = default
            binding.sbPickerGap.value = default.toFloat()
            binding.tvPickerGapValue.text = "${default}dp"
            applyOnly()
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

    private fun freeformModeLabel(mode: String): String = when (mode) {
        PanelPreferences.FREEFORM_MODE_PORTRAIT  -> "Portrait (Narrow)"
        PanelPreferences.FREEFORM_MODE_MAXIMIZED -> "Maximized"
        PanelPreferences.FREEFORM_MODE_CUSTOM    -> "Custom (${panelPrefs.freeformCustomWidth}% × ${panelPrefs.freeformCustomHeight}%)"
        else                                      -> "Standard (80%)"
    }
}
