package com.imi.smartedge.sidebar.panel

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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



        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        panelPrefs = PanelPreferences(this)
        
        initializeSettingsList()
        setupSearch()
        setupListeners()
        
        binding.btnGithubTop.setOnClickListener {
            openGithub()
        }

        // Handle System Back to close search
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.rvSettingsResults.visibility == View.VISIBLE || binding.etSettingsSearch.visibility == View.VISIBLE) {
                    collapseSearch()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun initializeSettingsList() {
        allSettings.clear()
        
        // Map of Layouts to their corresponding Activities
        val layoutMap = mapOf(
            R.layout.activity_settings_appearance to AppearanceSettingsActivity::class.java,
            R.layout.activity_settings_interaction to InteractionSettingsActivity::class.java,
            R.layout.activity_settings_handle to HandleSettingsActivity::class.java,
            R.layout.activity_settings_tools to ToolsSettingsActivity::class.java
        )

        val inflater = LayoutInflater.from(this)
        
        layoutMap.forEach { (layoutId, activityClass) ->
            val root = inflater.inflate(layoutId, null) as ViewGroup
            discoverSettingsInView(root, activityClass, layoutId)
        }

        // Add static actions that aren't in standard layouts
        allSettings.add(SettingItem("Manage Apps", "Choose which apps appear in sidebar", "General", "apps choose select picker manage add remove", AppPickerActivity::class.java))
        allSettings.add(SettingItem("View Repository", "Check source code on GitHub", "Project", "github source code open repo smartedge", SettingsMainActivity::class.java, "btnGithubTop"))
        allSettings.add(SettingItem("Reset to Defaults", "Reset all settings to factory state", "General", "reset all factory wipe restore settings", SettingsMainActivity::class.java, "btnReset"))
    }

    private fun discoverSettingsInView(view: View, activityClass: Class<*>, layoutId: Int) {
        val viewId = view.id
        if (viewId != View.NO_ID) {
            val idName = resources.getResourceEntryName(viewId)
            
            // "Intelligent" Discovery: Any view starting with 'feature_' or 'layout_' is indexed
            if (idName.startsWith("feature_") || idName.startsWith("layout_")) {
                val category = getCategoryFromLayout(layoutId)
                val (title, description) = extractMetadata(view)
                
                if (title.isNotEmpty()) {
                    allSettings.add(SettingItem(title, description, category, "$title $description $idName".lowercase(), activityClass, idName))
                }
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                discoverSettingsInView(view.getChildAt(i), activityClass, layoutId)
            }
        }
    }

    private fun extractMetadata(view: View): Pair<String, String> {
        var title = ""
        var description = ""

        if (view is ViewGroup) {
            val textViews = mutableListOf<TextView>()
            findAllTextViews(view, textViews)
            
            if (textViews.isNotEmpty()) {
                // The first TextView is almost always the Title in our layouts
                title = textViews[0].text.toString()
                
                // The second TextView is usually the Description/Value
                if (textViews.size > 1) {
                    description = textViews[1].text.toString()
                }
            }
        } else if (view is com.google.android.material.materialswitch.MaterialSwitch) {
            title = view.text.toString()
        }

        return title to description
    }

    private fun findAllTextViews(view: View, list: MutableList<TextView>) {
        if (view is TextView && view !is com.google.android.material.materialswitch.MaterialSwitch) {
            list.add(view)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findAllTextViews(view.getChildAt(i), list)
            }
        }
    }

    private fun getCategoryFromLayout(layoutId: Int): String = when (layoutId) {
        R.layout.activity_settings_appearance -> "Appearance"
        R.layout.activity_settings_interaction -> "Interaction"
        R.layout.activity_settings_handle -> "Handle"
        R.layout.activity_settings_tools -> "Tools"
        else -> "General"
    }

    private fun setupSearch() {
        searchAdapter = SearchResultsAdapter { item ->
            collapseSearch()
            
            if (item.targetActivity == SettingsMainActivity::class.java) {
                if (item.targetId == "btnReset") {
                    binding.settingsMainContent.post {
                        binding.settingsMainContent.fullScroll(View.FOCUS_DOWN)
                        binding.btnReset.highlightView()
                    }
                } else if (item.targetId == "btnGithubTop") {
                    openGithub()
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

        // Setup expansion animation
        binding.searchCard.setOnClickListener {
            if (binding.etSettingsSearch.visibility == View.GONE) {
                expandSearch()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (binding.rvSettingsResults.visibility == View.VISIBLE || binding.etSettingsSearch.visibility == View.VISIBLE) {
            collapseSearch()
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

    private fun openGithub() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Imtiaz-Official/Smart-Edge"))
            startActivity(intent)
        } catch (e: Exception) {
            binding.root.showModernToast("Could not open browser")
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun expandSearch() {
        val transition = android.transition.TransitionSet().apply {
            addTransition(android.transition.ChangeBounds())
            addTransition(android.transition.Fade())
            duration = 300
            interpolator = android.view.animation.DecelerateInterpolator()
        }
        
        android.transition.TransitionManager.beginDelayedTransition(binding.searchCard.parent as ViewGroup, transition)
        
        val lp = binding.searchCard.layoutParams as FrameLayout.LayoutParams
        lp.width = FrameLayout.LayoutParams.MATCH_PARENT
        lp.setMargins(dpToPx(20), 0, dpToPx(20), 0)
        binding.searchCard.layoutParams = lp

        val containerLp = binding.searchContainer.layoutParams
        containerLp.width = ViewGroup.LayoutParams.MATCH_PARENT
        binding.searchContainer.layoutParams = containerLp
        
        binding.tvSearchHint.visibility = View.GONE
        binding.etSettingsSearch.visibility = View.VISIBLE
        binding.etSettingsSearch.requestFocus()
        
        // Show keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.etSettingsSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun collapseSearch() {
        val transition = android.transition.TransitionSet().apply {
            addTransition(android.transition.ChangeBounds())
            addTransition(android.transition.Fade())
            duration = 250
            interpolator = android.view.animation.AccelerateInterpolator()
        }
        
        android.transition.TransitionManager.beginDelayedTransition(binding.searchCard.parent as ViewGroup, transition)
        
        binding.etSettingsSearch.setText("")
        binding.etSettingsSearch.visibility = View.GONE
        binding.tvSearchHint.visibility = View.VISIBLE
        binding.etSettingsSearch.clearFocus()
        
        val lp = binding.searchCard.layoutParams as FrameLayout.LayoutParams
        lp.width = FrameLayout.LayoutParams.WRAP_CONTENT
        lp.setMargins(0, 0, 0, 0)
        binding.searchCard.layoutParams = lp

        val containerLp = binding.searchContainer.layoutParams
        containerLp.width = ViewGroup.LayoutParams.WRAP_CONTENT
        binding.searchContainer.layoutParams = containerLp

        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSettingsSearch.windowToken, 0)
    }

    private fun filterSettings(query: String) {
        val lowerQuery = query.lowercase().trim()
        if (lowerQuery.isEmpty()) {
            binding.rvSettingsResults.visibility = View.GONE
            binding.settingsCategoriesContainer.visibility = View.VISIBLE
            return
        }

        val filtered = allSettings.filter { item ->
            item.title.lowercase().contains(lowerQuery) ||
            item.description.lowercase().contains(lowerQuery) ||
            item.keywords.lowercase().contains(lowerQuery)
        }

        binding.settingsCategoriesContainer.visibility = View.GONE
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
                title.setTextColor(com.google.android.material.color.MaterialColors.getColor(title, com.google.android.material.R.attr.colorOnSurface))
                
                desc.text = "${item.category} • ${item.description}"
                desc.setTextColor(com.google.android.material.color.MaterialColors.getColor(desc, com.google.android.material.R.attr.colorOnSurfaceVariant))
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
