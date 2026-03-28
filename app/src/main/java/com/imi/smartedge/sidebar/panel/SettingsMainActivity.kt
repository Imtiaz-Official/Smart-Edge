package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.imi.smartedge.sidebar.panel.databinding.ActivitySettingsMainBinding

class SettingsMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsMainBinding
    private lateinit var panelPrefs: PanelPreferences

    private data class SettingItem(
        val title: String,
        val description: String,
        val category: String,
        val keywords: String,
        val targetActivity: Class<*>,
        val targetId: String? = null
    )

    private val allSettings = mutableListOf<SettingItem>()
    private lateinit var searchAdapter: SearchResultsAdapter

    companion object {
        const val EXTRA_SCROLL_TO = "extra_scroll_to"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color and icons
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window.statusBarColor = typedValue.data
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        panelPrefs = PanelPreferences(this)
        
        initializeSettingsList()
        setupSearch()
        setupListeners()
    }

    private fun initializeSettingsList() {
        // --- Appearance ---
        allSettings.add(SettingItem("UI Style Theme", "Change panel theme (OriginOS, HyperOS, etc.)", "Appearance", "theme style skin origin hyperos realme rich m3 material design", AppearanceSettingsActivity::class.java, "layoutUIStyle"))
        allSettings.add(SettingItem("Glassmorphic Blur", "Enable background blur effect", "Appearance", "blur glass transparency frost acrylic background effect", AppearanceSettingsActivity::class.java, "feature_blur"))
        allSettings.add(SettingItem("Blur Intensity", "Adjust the background blur strength", "Appearance", "blur level transparency strength depth amount", AppearanceSettingsActivity::class.java, "feature_blur_intensity"))
        allSettings.add(SettingItem("Invisible Background", "Make the sidebar background fully transparent", "Appearance", "hide background invisible transparency clear alpha", AppearanceSettingsActivity::class.java, "feature_hide_bg"))
        allSettings.add(SettingItem("Double-Column Layout", "Show two columns of apps", "Appearance", "column grid layout width dual rows columns", AppearanceSettingsActivity::class.java, "feature_columns"))
        allSettings.add(SettingItem("Icon Shape", "Change app icon masks", "Appearance", "icon shape mask circle square squircle round adaptive", AppearanceSettingsActivity::class.java, "feature_icon_shape"))
        allSettings.add(SettingItem("Icon Pack", "Apply third-party icon packs", "Appearance", "icon pack theme customize iconpack icons", AppearanceSettingsActivity::class.java, "feature_icon_pack"))
        allSettings.add(SettingItem("Use Custom Accent", "Toggle custom highlight color", "Appearance", "custom accent enable primary toggle color", AppearanceSettingsActivity::class.java, "feature_custom_accent"))
        allSettings.add(SettingItem("Accent Color", "Change UI highlight color", "Appearance", "color accent highlight tint primary custom", AppearanceSettingsActivity::class.java, "feature_accent_color"))
        allSettings.add(SettingItem("Panel Background", "Choose custom background color", "Appearance", "background color customize primary bg", AppearanceSettingsActivity::class.java, "feature_bg_color"))
        allSettings.add(SettingItem("Reset UI Colors", "Restore default colors and theme", "Appearance", "reset color restore defaults factory", AppearanceSettingsActivity::class.java, "btnResetUIColors"))
        allSettings.add(SettingItem("Service Button Style", "Toggle between Power and Classic home button", "Appearance", "home button power classic style start stop toggle", AppearanceSettingsActivity::class.java, "feature_home_button"))
        allSettings.add(SettingItem("Panel Opacity", "Adjust panel transparency", "Appearance", "opacity transparent alpha background visibility", AppearanceSettingsActivity::class.java, "feature_opacity"))
        allSettings.add(SettingItem("Corner Radius", "Change panel roundness", "Appearance", "corner radius roundness shape curve smooth", AppearanceSettingsActivity::class.java, "feature_radius"))

        // --- Interaction ---
        allSettings.add(SettingItem("Screen Side", "Choose left or right screen edge", "Interaction", "side edge left right position hand layout", InteractionSettingsActivity::class.java, "feature_panel_side"))
        allSettings.add(SettingItem("Edge Swipe Gestures", "Enable swipe and hold gestures", "Interaction", "gesture swipe hold interaction touch triggers", InteractionSettingsActivity::class.java, "feature_gestures"))
        allSettings.add(SettingItem("Tap Pill to Open", "Open panel with a simple tap", "Interaction", "tap click open interaction touch trigger", InteractionSettingsActivity::class.java, "feature_tap_open"))
        allSettings.add(SettingItem("Haptic Feedback", "Tactile vibration on touch", "Interaction", "haptic vibration tactile feel feedback shake touch", InteractionSettingsActivity::class.java, "feature_haptic"))
        allSettings.add(SettingItem("Auto Start", "Start service on device boot", "Interaction", "boot autostart service background startup run", InteractionSettingsActivity::class.java, "feature_auto_start"))
        allSettings.add(SettingItem("Activity Logs", "Show debug logs on home screen", "Interaction", "logs activity debug show developer info", InteractionSettingsActivity::class.java, "feature_show_logs"))
        allSettings.add(SettingItem("Animation Feel", "Adjust UI animation speed", "Interaction", "animation speed feel calm snappy instant smooth performance", InteractionSettingsActivity::class.java, "feature_anim_feel"))
        allSettings.add(SettingItem("Picker Gap", "Space between app categories", "Interaction", "gap margin picker space categories spacing", InteractionSettingsActivity::class.java, "feature_picker_gap"))

        // --- Handle & Pill ---
        allSettings.add(SettingItem("Visible Pill Handle", "Toggle visual handle visibility", "Handle", "pill handle indicator visibility show hide look", HandleSettingsActivity::class.java, "feature_show_pill"))
        allSettings.add(SettingItem("Handle Height", "Adjust trigger area height", "Handle", "height size length handle vertical long short", HandleSettingsActivity::class.java, "feature_handle_height"))
        allSettings.add(SettingItem("Handle Width", "Adjust trigger area width", "Handle", "width thickness handle horizontal fat thin", HandleSettingsActivity::class.java, "feature_handle_width"))
        allSettings.add(SettingItem("Vertical Position", "Adjust handle offset from center", "Handle", "position offset center vertical move up down height", HandleSettingsActivity::class.java, "feature_handle_position"))

        // --- Tools ---
        allSettings.add(SettingItem("Dashboard Tools", "Toggle bottom tools section", "Tools", "tools visibility dashboard utilities bottom", ToolsSettingsActivity::class.java, "feature_tools_master"))
        allSettings.add(SettingItem("System Info Overlay", "Show RAM and Battery info", "Tools", "ram memory battery usage system info monitor stats hardware", ToolsSettingsActivity::class.java, "feature_sys_info"))
        allSettings.add(SettingItem("Power Menu", "Add reboot/screenshot options", "Tools", "power reboot screenshot restart shutdown menu tools", ToolsSettingsActivity::class.java, "feature_power_menu"))

        // --- Extra Actions ---
        allSettings.add(SettingItem("Manage Apps", "Choose which apps appear in sidebar", "General", "apps choose select picker manage add remove", AppPickerActivity::class.java))
        allSettings.add(SettingItem("Support Development", "Donate or support the project", "Support", "donate support heart love money contribute", SupportActivity::class.java))
        allSettings.add(SettingItem("Reset to Defaults", "Reset all settings to factory state", "General", "reset all factory wipe restore settings", SettingsMainActivity::class.java, "btnReset"))
    }

    private fun setupSearch() {
        searchAdapter = SearchResultsAdapter { item ->
            if (item.targetActivity == SettingsMainActivity::class.java && item.targetId == "btnReset") {
                // If it's reset, just close search and scroll to reset button
                binding.etSettingsSearch.setText("")
                binding.settingsMainContent.post {
                    binding.settingsMainContent.fullScroll(View.FOCUS_DOWN)
                    binding.btnReset.highlightView()
                }
                return@SearchResultsAdapter
            }

            val intent = Intent(this, item.targetActivity)
            if (item.targetId != null) {
                intent.putExtra(EXTRA_SCROLL_TO, item.targetId)
            }
            startActivity(intent)
        }
        binding.rvSettingsResults.layoutManager = LinearLayoutManager(this)
        binding.rvSettingsResults.adapter = searchAdapter

        binding.etSettingsSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSettings(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        if (binding.rvSettingsResults.visibility == View.VISIBLE) {
            binding.etSettingsSearch.setText("")
            return true
        }
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupListeners() {
        binding.btnAppearance.setOnClickListener {
            startActivity(Intent(this, AppearanceSettingsActivity::class.java))
        }

        binding.btnInteraction.setOnClickListener {
            startActivity(Intent(this, InteractionSettingsActivity::class.java))
        }

        binding.btnHandle.setOnClickListener {
            startActivity(Intent(this, HandleSettingsActivity::class.java))
        }

        binding.btnTools.setOnClickListener {
            startActivity(Intent(this, ToolsSettingsActivity::class.java))
        }

        binding.btnDonation.setOnClickListener {
            startActivity(Intent(this, SupportActivity::class.java))
        }

        binding.btnReset.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Reset All Settings?")
                .setMessage("This will restore all configurations to their original defaults. Your added apps will NOT be removed.")
                .setPositiveButton("Reset") { _, _ ->
                    panelPrefs.resetToDefaults()
                    applyGlobalRefresh()
                    binding.root.showModernToast("Settings Reset Successfully")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun filterSettings(query: String) {
        val lowerQuery = query.lowercase().trim()
        if (lowerQuery.isEmpty()) {
            binding.rvSettingsResults.visibility = View.GONE
            binding.settingsMainContent.visibility = View.VISIBLE
            return
        }

        val filtered = allSettings.filter { item ->
            item.title.lowercase().contains(lowerQuery) ||
            item.description.lowercase().contains(lowerQuery) ||
            item.keywords.lowercase().contains(lowerQuery)
        }

        binding.settingsMainContent.visibility = View.GONE
        binding.rvSettingsResults.visibility = View.VISIBLE
        searchAdapter.submitList(filtered)
    }

    private fun applyGlobalRefresh() {
        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_REFRESH
        }
        startService(intent)
    }

    // --- Search Adapter ---
    private inner class SearchResultsAdapter(private val onClick: (SettingItem) -> Unit) : 
        ListAdapter<SettingItem, SearchResultsAdapter.ViewHolder>(DiffCallback) {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val title = view.findViewById<TextView>(android.R.id.text1)
            private val desc = view.findViewById<TextView>(android.R.id.text2)

            fun bind(item: SettingItem) {
                title.text = item.title
                title.setTextColor(Color.WHITE)
                desc.text = "${item.category} • ${item.description}"
                desc.setTextColor(Color.parseColor("#B3FFFFFF"))
                itemView.setOnClickListener { onClick(item) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            view.setPadding(64, 32, 64, 32)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SettingItem>() {
        override fun areItemsTheSame(oldItem: SettingItem, newItem: SettingItem) = oldItem.title == newItem.title
        override fun areContentsTheSame(oldItem: SettingItem, newItem: SettingItem) = oldItem == newItem
    }
}
