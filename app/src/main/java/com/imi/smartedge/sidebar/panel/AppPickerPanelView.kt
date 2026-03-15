package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppPickerPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onClose: (() -> Unit)? = null
    var onAppLaunched: (() -> Unit)? = null
    var onToggleApp: ((AppInfo, Boolean) -> Unit)? = null

    private val pickerPanelCard: View
    private val rvPickerGrid: RecyclerView
    private val etSearch: EditText
    private val btnSettings: ImageButton
    private val btnEdit: TextView
    private val tvHeader: TextView
    private val adapter = PickerAdapter()
    
    private val repository = AppRepository(context)
    private val panelPrefs = PanelPreferences(context)
    private var allApps = listOf<AppInfo>()
    var isEditMode = false
        private set
    
    private val scope: CoroutineScope
        get() = _scope
    private var _scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Recreate scope if it was cancelled by a previous detach
        if (!_scope.coroutineContext[Job]!!.isActive) {
            _scope = CoroutineScope(Dispatchers.Main + Job())
        }
    }

    private lateinit var gestureDetector: android.view.GestureDetector

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (::gestureDetector.isInitialized) {
            gestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.picker_panel_layout, this, true)
        pickerPanelCard = view.findViewById(R.id.pickerPanelCard)
        rvPickerGrid = view.findViewById(R.id.rvPickerGrid)
        etSearch = view.findViewById(R.id.etPickerSearch)
        btnSettings = view.findViewById(R.id.btnPickerClose) 
        btnEdit = view.findViewById(R.id.btnPickerEdit)
        tvHeader = view.findViewById(R.id.tvPickerHeader)

        gestureDetector = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val canScrollUp = rvPickerGrid.canScrollVertically(-1)
                if (!canScrollUp && velocityY > 800f) {
                    onClose?.invoke()
                    return true
                }
                return false
            }
        })

        if (panelPrefs.uiTheme == PanelPreferences.THEME_RICH) {
            rvPickerGrid.layoutManager = LinearLayoutManager(context)
        } else {
            rvPickerGrid.layoutManager = GridLayoutManager(context, 2)
        }
        
        rvPickerGrid.adapter = adapter

        btnSettings.setOnClickListener {
            val intent = android.content.Intent(context, SettingsActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onAppLaunched?.invoke() // This will call closePanel() in the service
        }

        btnEdit.setOnClickListener {
            setEditMode(!isEditMode)
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        applyTheme()
        loadApps()
    }

    fun applyTheme() {
        val theme = panelPrefs.uiTheme
        val density = context.resources.displayMetrics.density
        
        val drawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        }
        
        val themeBgColor = when (theme) {
            PanelPreferences.THEME_ORIGIN -> Color.parseColor("#1F1F1F") 
            PanelPreferences.THEME_HYPEROS -> Color.parseColor("#E6252525")
            else -> Color.parseColor(panelPrefs.panelBackgroundColor)
        }
        drawable.setColor(themeBgColor)
        
        // Match SidePanelView logic for HyperOS
        val finalRadius = if (theme == PanelPreferences.THEME_HYPEROS) 16f else panelPrefs.panelCornerRadius.toFloat()
        drawable.cornerRadius = finalRadius * density

        if (theme == PanelPreferences.THEME_HYPEROS) {
            drawable.setStroke((1.5 * density).toInt(), Color.parseColor("#4DFFFFFF"))
        }
        
        pickerPanelCard.background = drawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) pickerPanelCard.clipToOutline = true

        // Style the search bar background
        val searchBg = findViewById<View>(R.id.etPickerSearch).parent as? View
        searchBg?.let {
            val sd = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 20 * density
                setColor(Color.parseColor("#4D000000")) // Darker pill like image
            }
            it.background = sd
        }
    }

    fun setEditMode(enabled: Boolean) {
        if (isEditMode == enabled) return
        isEditMode = enabled
        tvHeader.text = if (isEditMode) "Manage Smart Edge" else "All Apps"
        btnEdit.text = if (isEditMode) "DONE" else "EDIT"
        
        // Visual feedback for the edit button
        val accentColor = try {
            if (panelPrefs.useCustomAccent) Color.parseColor(panelPrefs.accentColor)
            else Color.parseColor("#4A9EFF")
        } catch (e: Exception) { Color.parseColor("#4A9EFF") }

        btnEdit.setTextColor(if (isEditMode) accentColor else Color.parseColor("#4A9EFF"))
        
        // Surgical update instead of full notifyDataSetChanged to prevent flickering
        adapter.notifyItemRangeChanged(0, adapter.itemCount, "EDIT_MODE_CHANGE")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Cancel all running coroutines (icon loads) to prevent memory leaks
        scope.cancel()
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_UP && event.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            onClose?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    fun loadApps(forceRefresh: Boolean = false) {
        if (!forceRefresh && allApps.isNotEmpty()) {
            // Just refresh the "isInPanel" state from preferences without recreating objects
            val panelPackages = panelPrefs.getPanelApps().toSet()
            var changed = false
            allApps.forEach { 
                val inPanel = panelPackages.contains(it.packageName)
                if (it.isInPanel != inPanel) {
                    it.isInPanel = inPanel
                    changed = true
                }
            }
            if (changed) {
                // Submit a copy to trigger DiffUtil if needed, or just notify
                adapter.notifyItemRangeChanged(0, adapter.itemCount, "PANEL_STATE_CHANGE")
            }
            return
        }

        scope.launch {
            val apps = withContext(Dispatchers.IO) { repository.getAllApps() }
            allApps = apps
            adapter.submitList(allApps.toList())
        }
    }

    /** Clears the search box and restores the full app list. Call when picker opens. */
    fun resetSearch() {
        if (etSearch.text.isNotEmpty()) {
            etSearch.setText("")
            adapter.submitList(allApps.toList())
        }
    }

    /** Clears cached app list so next loadApps() fetches fresh data. Call when picker closes. */
    fun invalidateAppList() {
        allApps = listOf()
    }

    /** Specifically clears icons from cached apps to force a reload (e.g. icon pack change) */
    fun clearIcons() {
        allApps.forEach { it.icon = null }
        adapter.notifyDataSetChanged()
    }

    private fun filter(query: String) {
        val filtered = allApps.filter { it.appName.contains(query, ignoreCase = true) }
        adapter.submitList(filtered)
    }

    fun getPickerCardRect(outRect: android.graphics.Rect) {
        pickerPanelCard.getGlobalVisibleRect(outRect)
    }

    inner class PickerAdapter : androidx.recyclerview.widget.ListAdapter<AppInfo, PickerViewHolder>(AppDiffCallback()) {

        private var accentColor: Int = Color.parseColor("#4DFFFFFF")
        private var accentColorStateList: android.content.res.ColorStateList = android.content.res.ColorStateList.valueOf(accentColor)

        init {
            updateAccentColor()
        }

        fun updateAccentColor() {
            accentColor = try {
                if (panelPrefs.useCustomAccent) {
                    Color.parseColor(panelPrefs.accentColor)
                } else {
                    Color.parseColor("#4DFFFFFF")
                }
            } catch (e: Exception) {
                Color.parseColor("#4DFFFFFF")
            }
            accentColorStateList = android.content.res.ColorStateList.valueOf(accentColor)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickerViewHolder {
            val layoutId = if (panelPrefs.uiTheme == PanelPreferences.THEME_RICH) 
                R.layout.item_picker_app_rich else R.layout.item_picker_app_modern
            
            val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
            return PickerViewHolder(view)
        }

        override fun onBindViewHolder(holder: PickerViewHolder, position: Int) {
            val app = getItem(position)
            holder.tvName.text = app.appName
            holder.tvPackage?.text = app.packageName

            // --- Lazy icon loading ---
            // Cancel any previous icon load for this view (it was recycled)
            holder.iconJob?.cancel()

            if (app.icon != null) {
                // Icon already cached on the AppInfo object — show immediately
                holder.ivIcon.setImageDrawable(app.icon)
            } else {
                // Show placeholder immediately so there's no blank gap
                holder.ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                // Load icon in background, then update only this view
                holder.iconJob = scope.launch {
                    val icon: Drawable? = withContext(Dispatchers.IO) {
                        repository.loadIconForApp(app.packageName)
                    }
                    // Cache result on the AppInfo so DiffCallback / rebinds skip re-loading
                    if (icon != null) {
                        app.icon = icon
                        // Verify the holder still shows the same app (not recycled to another)
                        val currentPos = holder.bindingAdapterPosition
                        if (currentPos != RecyclerView.NO_POSITION && currentPos < currentList.size &&
                            currentList[currentPos].packageName == app.packageName) {
                            holder.ivIcon.setImageDrawable(icon)
                        }
                    }
                }
            }

            // Apply icon shape immediately (even to placeholder)
            IconShapeHelper.applyShape(holder.ivIcon, panelPrefs.iconShape)

            if (isEditMode) {
                holder.ivCheck.visibility = View.VISIBLE
                val isSelected = app.isInPanel
                
                // Color the plus icon itself
                val iconTint = if (isSelected) accentColorStateList else android.content.res.ColorStateList.valueOf(Color.parseColor("#B3FFFFFF"))
                if (holder.ivCheck is ImageView) {
                    holder.ivCheck.imageTintList = iconTint
                }
                
                // Rotate to 'x' if selected, 'plus' if not
                holder.ivCheck.rotation = if (isSelected) 45f else 0f
            } else {
                holder.ivCheck.visibility = View.GONE
            }

            // vHighlight overlay removed — no shader/dim on icons
            holder.vHighlight.visibility = View.GONE

            // In normal mode: clicking the item launches the app.
            // In edit mode: clicking the item does nothing — only the plus icon works.
            holder.itemView.setOnClickListener {
                if (!isEditMode) {
                    if (panelPrefs.hapticEnabled) {
                        holder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    }
                    launchApp(app)
                }
            }

            // Only the plus icon triggers add/remove in edit mode.
            // Use bindingAdapterPosition (not the captured `position`) to avoid stale index.
            holder.ivCheck.setOnClickListener {
                if (isEditMode) {
                    val currentPos = holder.bindingAdapterPosition
                    if (currentPos != RecyclerView.NO_POSITION) {
                        if (panelPrefs.hapticEnabled) {
                            it.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                        }
                        toggleAppSelection(getItem(currentPos), currentPos, holder.ivCheck)
                    }
                }
            }
        }

        override fun onViewRecycled(holder: PickerViewHolder) {
            super.onViewRecycled(holder)
            // Cancel the pending icon load so a recycled view doesn't get the wrong icon
            holder.iconJob?.cancel()
            holder.iconJob = null
        }

        private fun toggleAppSelection(app: AppInfo, position: Int, plusView: View) {
            val newState = !app.isInPanel
            app.isInPanel = newState
            onToggleApp?.invoke(app, newState)

            // Animate plus icon: rotate to 'x' when selected, back to '+' when not
            plusView.animate()
                .rotation(if (newState) 45f else 0f)
                .setDuration(200)
                .start()

            // Change icon tint only (no background — the icon has no circular shadow)
            if (plusView is ImageView) {
                val tint = if (newState) accentColorStateList
                           else android.content.res.ColorStateList.valueOf(Color.parseColor("#B3FFFFFF"))
                plusView.imageTintList = tint
            }

            notifyItemChanged(position, "TOGGLE_STATE")
        }

        private fun launchApp(app: AppInfo) {
            rvPickerGrid.findViewHolderForAdapterPosition(currentList.indexOf(app))?.itemView?.let {
                SpringAnimator.scalePulse(it)
            }
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            onAppLaunched?.invoke()
        }

        override fun onBindViewHolder(holder: PickerViewHolder, position: Int, payloads: List<Any>) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                val app = getItem(position)
                if (isEditMode) {
                    holder.ivCheck.visibility = View.VISIBLE
                    val isSelected = app.isInPanel
                    
                    val iconTint = if (isSelected) accentColorStateList else android.content.res.ColorStateList.valueOf(Color.parseColor("#B3FFFFFF"))
                    if (holder.ivCheck is ImageView) {
                        holder.ivCheck.imageTintList = iconTint
                    }
                    holder.ivCheck.rotation = if (isSelected) 45f else 0f
                } else {
                    holder.ivCheck.visibility = View.GONE
                }
                holder.vHighlight.visibility = View.GONE
            }
        }
    }

    private class AppDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            // Icons are loaded lazily and stored directly on AppInfo; we don't compare them
            // here to avoid blocking updates while icons are being fetched.
            return oldItem.appName == newItem.appName &&
                   oldItem.isInPanel == newItem.isInPanel
        }
    }

    inner class PickerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivPickerAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvPickerAppName)
        val ivCheck: View = view.findViewById(R.id.ivPickerCheck)
        val vHighlight: View = view.findViewById(R.id.vPickerBgHighlight)
        val tvPackage: TextView? = view.findViewById(R.id.tvPickerPackageName)
        /** Tracks the active icon-loading coroutine for this holder. */
        var iconJob: Job? = null
    }
}
