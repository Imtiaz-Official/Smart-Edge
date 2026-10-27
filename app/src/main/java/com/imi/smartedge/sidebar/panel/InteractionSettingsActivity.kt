package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
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

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

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
            }
        }
    }

    private fun loadCurrentSettings() {
        updateSecureSettingsUI()
        
        // 1. MAIN TRIGGER & GESTURES
        when (panelPrefs.panelSide) {
            PanelPreferences.SIDE_LEFT -> {
                binding.togglePanelSide.check(R.id.btnSideLeft)
                binding.togglePanelSide.uncheck(R.id.btnSideRight)
            }
            PanelPreferences.SIDE_BOTH -> {
                binding.togglePanelSide.check(R.id.btnSideLeft)
                binding.togglePanelSide.check(R.id.btnSideRight)
            }
            else -> {
                binding.togglePanelSide.check(R.id.btnSideRight)
                binding.togglePanelSide.uncheck(R.id.btnSideLeft)
            }
        }

        binding.featureGestures.isChecked = panelPrefs.gesturesEnabled
        binding.featureOnlyOnHome.isChecked = panelPrefs.onlyOnHome
        binding.featureAutomationGestures.isChecked = panelPrefs.useAutomationForGestures
        binding.sbSwipeSensitivity.value = panelPrefs.swipeSensitivity.toFloat()
        binding.tvSwipeSensitivityValue.text = "${panelPrefs.swipeSensitivity}%"
        binding.layoutSwipeSensitivity.visibility = if (panelPrefs.gesturesEnabled) View.VISIBLE else View.GONE
        
        binding.tvTapGesturesValue.text = getString(R.string.tap_gestures_summary, actionLabel(panelPrefs.tapAction), actionLabel(panelPrefs.doubleTapAction), actionLabel(panelPrefs.tripleTapAction), actionLabel(panelPrefs.longPressAction))
        binding.featureHaptic.isChecked = panelPrefs.hapticEnabled

        // 2. SPECIALIZED INTERACTION
        binding.featureSlideBrightness.isChecked = panelPrefs.slideBrightnessEnabled
        binding.featureSlideVolume.isChecked = panelPrefs.slideVolumeEnabled
        binding.sbSlideSensitivity.value = panelPrefs.slideSensitivity.toFloat()
        binding.tvSlideSensitivityValue.text = "${panelPrefs.slideSensitivity}%"
        updateSlideSeekUI()

        binding.featureNotchGestures.isChecked = panelPrefs.notchGesturesEnabled
        binding.tvNotchTapGesturesValue.text = getString(R.string.tap_gestures_summary, actionLabel(panelPrefs.notchTapAction), actionLabel(panelPrefs.notchDoubleTapAction), actionLabel(panelPrefs.notchTripleTapAction), actionLabel(panelPrefs.notchLongPressAction))
        binding.layoutNotchTapGestures.visibility = if (panelPrefs.notchGesturesEnabled) View.VISIBLE else View.GONE

        // 3. PANEL EXPERIENCE
        binding.featureShowLandscape.isChecked = panelPrefs.showInLandscape
        binding.featureNotificationApps.isChecked = panelPrefs.showNotificationApps
        binding.featureRememberScroll.isChecked = panelPrefs.rememberScroll
        binding.featureAutoShowKeyboard.isChecked = panelPrefs.autoShowKeyboard

        // 4. MULTITASKING & WINDOWING
        binding.featureDragSplit.isChecked = panelPrefs.dragToSplit
        binding.featureFreeform.isChecked = panelPrefs.freeformEnabled
        binding.layoutFreeformSize.visibility = if (panelPrefs.freeformEnabled) View.VISIBLE else View.GONE
        binding.layoutCustomWindowingMode.visibility = if (panelPrefs.freeformEnabled) View.VISIBLE else View.GONE
        binding.layoutFreeformUseShell.visibility = if (panelPrefs.freeformEnabled) View.VISIBLE else View.GONE
        
        binding.featureFreeformUseShell.isChecked = panelPrefs.useShellForFreeform
        
        val sizeModeStr = when(panelPrefs.freeformWindowMode) {
            PanelPreferences.FREEFORM_MODE_STANDARD -> "Standard (80%)"
            PanelPreferences.FREEFORM_MODE_PORTRAIT -> "Portrait (Narrow)"
            PanelPreferences.FREEFORM_MODE_MAXIMIZED -> "Maximized"
            PanelPreferences.FREEFORM_MODE_CUSTOM -> "Custom (${panelPrefs.freeformCustomWidth}x${panelPrefs.freeformCustomHeight})"
            else -> "Standard (80%)"
        }
        binding.tvFreeformSizeValue.text = sizeModeStr
        binding.tvCustomWindowingModeValue.text = "${panelPrefs.freeformWindowingModeInt} ${if (panelPrefs.freeformWindowingModeInt == 5) "(Standard)" else ""}"

        // 5. SHORTCUTS & AUTOMATION
        binding.featureAutomationGestures.isChecked = panelPrefs.useAutomationForGestures

        // 6. ADVANCED OPTIONS
        val whitelistCount = panelPrefs.getFullscreenWhitelist().size
        binding.tvFullscreenWhitelistValue.text = if (whitelistCount == 1) getString(R.string.apps_selected_singular) else getString(R.string.apps_selected_plural, whitelistCount)

        val gameAppsCount = panelPrefs.getGameApps().size
        binding.tvGameAppsValue.text = if (gameAppsCount == 1) getString(R.string.apps_selected_singular) else getString(R.string.apps_selected_plural, gameAppsCount)
        binding.featureGameMode.isChecked = panelPrefs.deliberateGestureInGames

        // 7. SYSTEM & BEHAVIOR
        binding.featureAutoStart.isChecked = panelPrefs.autoStart
        binding.featureShowLogs.isChecked = panelPrefs.showLogs

        binding.tvAnimFeelValue.text = when (panelPrefs.animSpeed) {
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

    private fun updateSlideSeekUI() {
        val brightnessOn = panelPrefs.slideBrightnessEnabled
        val volumeOn = panelPrefs.slideVolumeEnabled
        val anyOn = brightnessOn || volumeOn
        binding.layoutSlideSensitivity.visibility = if (anyOn) View.VISIBLE else View.GONE
    }

    private fun updateSecureSettingsUI() {
        // Simple placeholder for M3 UI as it doesn't have a specific status TextView currently visible in the provided layout
    }

    private fun setupListeners() {
        binding.togglePanelSide.addOnButtonCheckedListener { group, _, _ ->
            val checkedIds = group.checkedButtonIds
            val hasLeft = checkedIds.contains(R.id.btnSideLeft)
            val hasRight = checkedIds.contains(R.id.btnSideRight)
            if (!hasLeft && !hasRight) {
                group.check(if (panelPrefs.panelSide == PanelPreferences.SIDE_LEFT) R.id.btnSideLeft else R.id.btnSideRight)
                return@addOnButtonCheckedListener
            }
            val newSide = when {
                hasLeft && hasRight -> PanelPreferences.SIDE_BOTH
                hasLeft -> PanelPreferences.SIDE_LEFT
                else -> PanelPreferences.SIDE_RIGHT
            }
            if (panelPrefs.panelSide != newSide) {
                panelPrefs.panelSide = newSide
                applyOnly()
            }
        }

        binding.featureGestures.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.gesturesEnabled = isChecked
            binding.layoutSwipeSensitivity.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyOnly()
        }

        binding.featureOnlyOnHome.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.onlyOnHome = isChecked
            applyOnly()
        }

        binding.featureAutomationGestures.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked && !AutomationManager.isAutomationPossible()) {
                AutomationManager.checkRootAndRequestPermission(this) { success ->
                    runOnUiThread {
                        if (success) {
                            panelPrefs.useAutomationForGestures = true
                            buttonView.isChecked = true
                            applyOnly()
                        } else {
                            buttonView.isChecked = false
                            showAutomationSetupDialog(buttonView)
                        }
                    }
                }
                return@setOnCheckedChangeListener
            }
            panelPrefs.useAutomationForGestures = isChecked
            applyOnly()
        }

        findViewById<android.view.View>(R.id.btnAutomationSettings)?.setOnClickListener {
            SecureSettingsDialog.show(this) {
                updateSecureSettingsUI()
                if (AutomationManager.isAutomationPossible()) {
                    binding.featureAutomationGestures.isChecked = true
                }
            }
        }

        binding.sbSwipeSensitivity.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val sens = value.toInt()
                panelPrefs.swipeSensitivity = sens
                binding.tvSwipeSensitivityValue.text = "$sens%"
            }
        }
        binding.sbSwipeSensitivity.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                applyOnly()
            }
        })

        binding.layoutTapGestures.setOnClickListener {
            val mainOptions = arrayOf("Single Tap Action", "Double Tap Action", "Triple Tap Action", "Long Press Action")
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.tap_gestures)
                .setItems(mainOptions) { _, which ->
                    when (which) {
                        0 -> showActionPicker("Single Tap", panelPrefs.tapAction) { panelPrefs.tapAction = it }
                        1 -> showActionPicker("Double Tap", panelPrefs.doubleTapAction) { panelPrefs.doubleTapAction = it }
                        2 -> showActionPicker("Triple Tap", panelPrefs.tripleTapAction) { panelPrefs.tripleTapAction = it }
                        3 -> showActionPicker("Long Press", panelPrefs.longPressAction) { panelPrefs.longPressAction = it }
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.featureNotchGestures.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.notchGesturesEnabled = isChecked
            binding.layoutNotchTapGestures.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyOnly()
        }

        binding.layoutNotchTapGestures.setOnClickListener {
            val mainOptions = arrayOf("Single Tap Action", "Double Tap Action", "Triple Tap Action", "Long Press Action")
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.notch_tap_gestures)
                .setItems(mainOptions) { _, which ->
                    when (which) {
                        0 -> showActionPicker("Notch Single Tap", panelPrefs.notchTapAction) { panelPrefs.notchTapAction = it }
                        1 -> showActionPicker("Notch Double Tap", panelPrefs.notchDoubleTapAction) { panelPrefs.notchDoubleTapAction = it }
                        2 -> showActionPicker("Notch Triple Tap", panelPrefs.notchTripleTapAction) { panelPrefs.notchTripleTapAction = it }
                        3 -> showActionPicker("Notch Long Press", panelPrefs.notchLongPressAction) { panelPrefs.notchLongPressAction = it }
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.featureHaptic.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.hapticEnabled = isChecked
        }

        // Panel Experience Listeners
        binding.featureShowLandscape.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showInLandscape = isChecked
            applyOnly()
        }
        binding.featureNotificationApps.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showNotificationApps = isChecked
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

        // System & Behavior Listeners
        binding.featureAutoStart.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.autoStart = isChecked
        }
        binding.featureShowLogs.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.showLogs = isChecked
            applyOnly()
        }

        binding.featureAddShortcut.setOnClickListener {
            pinShortcut()
        }

        binding.featureAnimFeel.setOnClickListener {
            val options = arrayOf(getString(R.string.anim_calm), getString(R.string.anim_balanced), getString(R.string.anim_feel_snappy), getString(R.string.anim_instant), getString(R.string.anim_disabled))
            val values = intArrayOf(200, 400, 700, 1000, 0)
            var selectedIndex = values.indexOf(panelPrefs.animSpeed)
            if (selectedIndex == -1) selectedIndex = 1

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.feature_anim_feel_label)
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    panelPrefs.animSpeed = values[which]
                    binding.tvAnimFeelValue.text = options[which]
                    applyOnly()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
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

        binding.featureSlideBrightness.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.slideBrightnessEnabled = isChecked
            updateSlideSeekUI()
            applyOnly()
        }

        binding.featureSlideVolume.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.slideVolumeEnabled = isChecked
            updateSlideSeekUI()
            applyOnly()
        }

        binding.sbSlideSensitivity.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val sens = value.toInt()
                panelPrefs.slideSensitivity = sens
                binding.tvSlideSensitivityValue.text = "$sens%"
            }
        }
        binding.sbSlideSensitivity.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                applyOnly()
            }
        })

        binding.featureGameMode.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.deliberateGestureInGames = isChecked
            applyOnly()
        }

        val dragSplitSwitch = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.feature_drag_split)
        dragSplitSwitch?.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.dragToSplit = isChecked
            applyOnly()
        }

        val freeformSwitch = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.feature_freeform)
        val freeformSizeLayout = findViewById<android.view.View>(R.id.layout_freeform_size)
        val tvFreeformSizeValue = findViewById<android.widget.TextView>(R.id.tvFreeformSizeValue)
        
        freeformSwitch?.setOnCheckedChangeListener { _, isChecked ->
            panelPrefs.freeformEnabled = isChecked
            freeformSizeLayout?.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.layoutCustomWindowingMode.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.layoutFreeformUseShell.visibility = if (isChecked) View.VISIBLE else View.GONE
            applyOnly()
        }

        binding.layoutFreeformUseShell.setOnClickListener {
            if (!AutomationManager.isAutomationPossible()) {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_permission_required)
                    .setMessage(R.string.dialog_native_shell_desc)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return@setOnClickListener
            }
            val newState = !binding.featureFreeformUseShell.isChecked
            binding.featureFreeformUseShell.isChecked = newState
            panelPrefs.useShellForFreeform = newState
            applyOnly()
        }

        freeformSizeLayout?.setOnClickListener {
            val options = arrayOf(getString(R.string.freeform_size_standard), getString(R.string.freeform_size_portrait), getString(R.string.freeform_size_maximized))
            val values = arrayOf(PanelPreferences.FREEFORM_MODE_STANDARD, PanelPreferences.FREEFORM_MODE_PORTRAIT, PanelPreferences.FREEFORM_MODE_MAXIMIZED)
            val currentIdx = values.indexOf(panelPrefs.freeformWindowMode).coerceAtLeast(0)

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_freeform_window_size)
                .setSingleChoiceItems(options, currentIdx) { dialog, which ->
                    panelPrefs.freeformWindowMode = values[which]
                    tvFreeformSizeValue?.text = options[which]
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.layoutCustomWindowingMode.setOnClickListener {
            val input = com.google.android.material.textfield.TextInputEditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(panelPrefs.freeformWindowingModeInt.toString())
                setSelection(text?.length ?: 0)
            }
            val inputLayout = com.google.android.material.textfield.TextInputLayout(this).apply {
                boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                setBoxCornerRadii(12f, 12f, 12f, 12f)
                hint = "Enter Windowing Mode ID"
                addView(input)
            }
            
            val hintText = android.widget.TextView(this).apply {
                text = "Common OEM Windowing IDs:\n• Nothing OS: 102\n• Xiaomi / HyperOS: 12\n• iQOO / OriginOS / Vivo: 100\n• Realme / Oppo / OnePlus: 100\n• AOSP Default: 5"
                textSize = 12f
                alpha = 0.7f
                setPadding(0, (12 * resources.displayMetrics.density).toInt(), 0, 0)
            }

            val container = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                val pad = (24 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad/2, pad, pad/2)
                addView(inputLayout)
                addView(hintText)
            }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_custom_windowing_mode)
                .setView(container)
                .setPositiveButton(R.string.btn_save) { _, _ ->
                    val newVal = input.text.toString().toIntOrNull() ?: 5
                    panelPrefs.freeformWindowingModeInt = newVal
                    binding.tvCustomWindowingModeValue.text = "$newVal ${if (newVal == 5) "(Standard)" else ""}"
                    applyOnly()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.layoutFullscreenWhitelist.setOnClickListener {
            showAppMultiPicker(getString(R.string.picker_title_hide), panelPrefs.getFullscreenWhitelist()) { newWhitelist ->
                panelPrefs.setFullscreenWhitelist(newWhitelist)
                binding.tvFullscreenWhitelistValue.text = if (newWhitelist.size == 1) getString(R.string.apps_selected_singular) else getString(R.string.apps_selected_plural, newWhitelist.size)
                applyOnly()
            }
        }

        binding.layoutGameApps.setOnClickListener {
            showAppMultiPicker(getString(R.string.picker_title_game), panelPrefs.getGameApps()) { newGames ->
                panelPrefs.setGameApps(newGames)
                binding.tvGameAppsValue.text = if (newGames.size == 1) getString(R.string.apps_selected_singular) else getString(R.string.apps_selected_plural, newGames.size)
                applyOnly()
            }
        }
    }

    private fun showAutomationSetupDialog(buttonView: android.widget.CompoundButton) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_automation_title)
            .setMessage(R.string.dialog_automation_msg)
            .setPositiveButton(R.string.btn_setup) { _, _ ->
                SecureSettingsDialog.show(this) {
                    updateSecureSettingsUI()
                    if (AutomationManager.isAutomationPossible()) {
                        buttonView.isChecked = true
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAppMultiPicker(title: String, currentSelected: List<String>, onSave: (List<String>) -> Unit) {
        lifecycleScope.launch(Dispatchers.Main) {
            val loadingDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this@InteractionSettingsActivity)
                .setTitle(R.string.loading_apps)
                .setMessage(R.string.please_wait_apps)
                .setCancelable(false)
                .show()

            val allApps = withContext(Dispatchers.IO) { AppRepository(this@InteractionSettingsActivity).getAllApps() }
            loadingDialog.dismiss()

            val sortedApps = allApps.sortedBy { it.appName.lowercase() }
            val selectedPkgs = currentSelected.toMutableSet()
            
            val density = resources.displayMetrics.density
            val container = android.widget.LinearLayout(this@InteractionSettingsActivity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding((20 * density).toInt(), (10 * density).toInt(), (20 * density).toInt(), 0)
            }

            val searchBar = com.google.android.material.textfield.TextInputEditText(this@InteractionSettingsActivity).apply {
                hint = getString(R.string.hint_search_apps)
                setSingleLine()
            }
            container.addView(com.google.android.material.textfield.TextInputLayout(this@InteractionSettingsActivity).apply {
                boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                setBoxCornerRadii(12 * density, 12 * density, 12 * density, 12 * density)
                addView(searchBar)
            })

            val listView = android.widget.ListView(this@InteractionSettingsActivity).apply {
                choiceMode = android.widget.ListView.CHOICE_MODE_MULTIPLE
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    (400 * density).toInt()
                )
            }
            container.addView(listView)

            var displayApps = sortedApps
            fun updateList(query: String) {
                displayApps = if (query.isEmpty()) sortedApps
                else sortedApps.filter { it.appName.contains(query, ignoreCase = true) }
                
                listView.adapter = android.widget.ArrayAdapter(
                    this@InteractionSettingsActivity,
                    android.R.layout.simple_list_item_multiple_choice,
                    displayApps.map { it.appName }.toTypedArray()
                )
                
                displayApps.forEachIndexed { index, app ->
                    listView.setItemChecked(index, selectedPkgs.contains(app.packageName))
                }
            }

            updateList("")

            listView.setOnItemClickListener { _, _, position, _ ->
                val pkg = displayApps[position].packageName
                if (listView.isItemChecked(position)) selectedPkgs.add(pkg) else selectedPkgs.remove(pkg)
            }

            searchBar.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    updateList(s.toString())
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this@InteractionSettingsActivity)
                .setTitle(title)
                .setView(container)
                .setPositiveButton(R.string.btn_save) { _, _ -> onSave(selectedPkgs.toList()) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun showAppSinglePicker(title: String, onSelect: (String) -> Unit) {
        lifecycleScope.launch(Dispatchers.Main) {
            val loadingDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this@InteractionSettingsActivity)
                .setTitle(R.string.loading_apps)
                .setMessage(R.string.please_wait_apps)
                .setCancelable(false)
                .show()

            val allApps = withContext(Dispatchers.IO) { AppRepository(this@InteractionSettingsActivity).getAllApps() }
            loadingDialog.dismiss()

            val sortedApps = allApps.sortedBy { it.appName.lowercase() }
            val density = resources.displayMetrics.density
            val container = android.widget.LinearLayout(this@InteractionSettingsActivity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding((20 * density).toInt(), (10 * density).toInt(), (20 * density).toInt(), 0)
            }

            val searchBar = com.google.android.material.textfield.TextInputEditText(this@InteractionSettingsActivity).apply {
                hint = getString(R.string.hint_search_apps)
                setSingleLine()
            }
            container.addView(com.google.android.material.textfield.TextInputLayout(this@InteractionSettingsActivity).apply {
                boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                setBoxCornerRadii(12 * density, 12 * density, 12 * density, 12 * density)
                addView(searchBar)
            })

            val listView = android.widget.ListView(this@InteractionSettingsActivity).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    (400 * density).toInt()
                )
            }
            container.addView(listView)

            var displayApps = sortedApps
            fun updateList(query: String) {
                displayApps = if (query.isEmpty()) sortedApps
                else sortedApps.filter { it.appName.contains(query, ignoreCase = true) }
                listView.adapter = android.widget.ArrayAdapter(this@InteractionSettingsActivity, android.R.layout.simple_list_item_1, displayApps.map { it.appName }.toTypedArray())
            }

            updateList("")

            val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this@InteractionSettingsActivity)
                .setTitle(title)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .show()

            listView.setOnItemClickListener { _, _, position, _ ->
                onSelect(displayApps[position].packageName)
                dialog.dismiss()
            }

            searchBar.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateList(s.toString()) }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
    }

    private fun applyOnly() {
        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_REFRESH
        }
        startService(intent)
    }

    private fun pinShortcut() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java)
            if (shortcutManager.isRequestPinShortcutSupported) {
                val pinIntent = Intent(this, ToggleActivity::class.java).apply {
                    action = ToggleActivity.ACTION_TOGGLE
                }
                val pinShortcutInfo = android.content.pm.ShortcutInfo.Builder(this, "toggle_sidebar_pinned")
                    .setShortLabel(getString(R.string.label_toggle_sidebar))
                    .setIcon(android.graphics.drawable.Icon.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(pinIntent)
                    .build()

                shortcutManager.requestPinShortcut(pinShortcutInfo, null)
                Toast.makeText(this, R.string.toast_shortcut_sent, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.toast_launcher_no_shortcut, Toast.LENGTH_SHORT).show()
            }
        } else {
            // Legacy way for Android < 8.0
            val shortcutIntent = Intent(this, ToggleActivity::class.java).apply {
                action = ToggleActivity.ACTION_TOGGLE
            }
            val addIntent = Intent().apply {
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.label_toggle_sidebar))
                val iconResource = Intent.ShortcutIconResource.fromContext(this@InteractionSettingsActivity, R.mipmap.ic_launcher)
                putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource)
                action = "com.android.launcher.action.INSTALL_SHORTCUT"
            }
            sendBroadcast(addIntent)
            Toast.makeText(this, R.string.toast_shortcut_added, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showActionPicker(title: String, current: Int, onSelect: (Int) -> Unit) {
        val options = arrayOf(
            getString(R.string.action_none),
            getString(R.string.action_launcher),
            getString(R.string.action_screenshot),
            getString(R.string.action_last_app),
            getString(R.string.action_back),
            getString(R.string.action_home),
            getString(R.string.action_recents),
            getString(R.string.action_notifications),
            getString(R.string.action_quick_settings),
            getString(R.string.action_lock_screen),
            getString(R.string.action_power_menu),
            getString(R.string.action_flashlight),
            getString(R.string.action_camera),
            getString(R.string.action_rotation),
            getString(R.string.action_fav_app),
            getString(R.string.action_move_handle)
        )
        val values = intArrayOf(
            PanelPreferences.ACTION_NONE,
            PanelPreferences.ACTION_OPEN_LAUNCHER,
            PanelPreferences.ACTION_SCREENSHOT,
            PanelPreferences.ACTION_PREVIOUS_APP,
            PanelPreferences.ACTION_BACK,
            PanelPreferences.ACTION_HOME,
            PanelPreferences.ACTION_RECENTS,
            PanelPreferences.ACTION_NOTIFICATIONS,
            PanelPreferences.ACTION_QUICK_SETTINGS,
            PanelPreferences.ACTION_LOCK_SCREEN,
            PanelPreferences.ACTION_POWER_MENU,
            PanelPreferences.ACTION_FLASHLIGHT,
            PanelPreferences.ACTION_CAMERA,
            PanelPreferences.ACTION_AUTO_ROTATION,
            PanelPreferences.ACTION_OPEN_FAVORITE_APP,
            PanelPreferences.ACTION_MOVE_HANDLE
        )
        val selectedIndex = values.indexOf(current).coerceAtLeast(0)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                val selectedAction = values[which]
                if (selectedAction == PanelPreferences.ACTION_OPEN_FAVORITE_APP) {
                    dialog.dismiss()
                    showAppSinglePicker(getString(R.string.picker_title_favorite)) { pkg ->
                        panelPrefs.favoriteAppPackage = pkg
                        onSelect(selectedAction)
                        loadCurrentSettings()
                        applyOnly()
                    }
                } else {
                    onSelect(selectedAction)
                    loadCurrentSettings()
                    applyOnly()
                    dialog.dismiss()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun actionLabel(action: Int): String = when (action) {
        PanelPreferences.ACTION_OPEN_LAUNCHER -> getString(R.string.action_launcher)
        PanelPreferences.ACTION_SCREENSHOT -> getString(R.string.action_screenshot)
        PanelPreferences.ACTION_PREVIOUS_APP -> getString(R.string.action_last_app)
        PanelPreferences.ACTION_BACK -> getString(R.string.action_back)
        PanelPreferences.ACTION_HOME -> getString(R.string.action_home)
        PanelPreferences.ACTION_RECENTS -> getString(R.string.action_recents)
        PanelPreferences.ACTION_NOTIFICATIONS -> getString(R.string.action_notifications)
        PanelPreferences.ACTION_QUICK_SETTINGS -> getString(R.string.action_quick_settings)
        PanelPreferences.ACTION_LOCK_SCREEN -> getString(R.string.action_lock_screen)
        PanelPreferences.ACTION_POWER_MENU -> getString(R.string.action_power_menu)
        PanelPreferences.ACTION_FLASHLIGHT -> getString(R.string.action_flashlight)
        PanelPreferences.ACTION_CAMERA -> getString(R.string.action_camera)
        PanelPreferences.ACTION_AUTO_ROTATION -> getString(R.string.action_rotation)
        PanelPreferences.ACTION_OPEN_FAVORITE_APP -> "Fav: ${panelPrefs.favoriteAppPackage.substringAfterLast(".").take(10)}"
        PanelPreferences.ACTION_MOVE_HANDLE -> getString(R.string.action_move_handle)
        else -> getString(R.string.action_none)
    }
}
