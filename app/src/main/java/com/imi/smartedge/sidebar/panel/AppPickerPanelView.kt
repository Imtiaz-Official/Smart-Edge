package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.graphics.Color
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppPickerPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    companion object {
        const val VIEW_TYPE_APP = 0
        const val VIEW_TYPE_HEADER = 1
        const val VIEW_TYPE_DIVIDER = 2
    }

    var onClose: (() -> Unit)? = null
    var onAppLaunched: (() -> Unit)? = null
    var onToggleApp: ((AppInfo, Boolean) -> Unit)? = null

    private val pickerPanelCard: View
    private val rvPickerGrid: RecyclerView
    private val etSearch: EditText
    private val btnSearchClear: ImageView
    private val btnSettings: ImageButton
    private val btnEdit: TextView
    private val tvHeader: TextView
    private val adapter = PickerAdapter()
    private val notificationAdapter = NotificationAdapter()
    
    private var recommendedApps = listOf<AppInfo>()
    
    private val repository = AppRepository(context)
    private val panelPrefs = PanelPreferences(context)
    private var allApps = listOf<AppInfo>()
    var isEditMode = false
        private set
    
    private var currentType = AppInfo.Type.APP
    private lateinit var btnTypeApps: TextView
    private lateinit var btnTypeActivities: TextView
    
    private var _scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!_scope.coroutineContext[Job]!!.isActive) {
            _scope = CoroutineScope(Dispatchers.Main + Job())
        }
        
        // Setup notification listener
        NotificationTrackingService.onNotificationsChanged = {
            _scope.launch { updateNotifications() }
        }
        updateNotifications()
    }

    private fun updateNotifications() {
        if (!panelPrefs.showNotificationApps) {
            findViewById<View>(R.id.layoutPickerNotifications).visibility = View.GONE
            return
        }

        val pkgs = NotificationTrackingService.getActiveNotificationPackages()
        
        if (pkgs.isEmpty()) {
            findViewById<View>(R.id.layoutPickerNotifications).visibility = View.GONE
        } else {
            val panelApps = panelPrefs.getPanelApps().toSet()
            val filteredPkgs = pkgs.filter { !panelApps.contains(it) }
            
            val appInfos = filteredPkgs.mapNotNull { pkg ->
                try {
                    val pm = context.packageManager
                    val ai = pm.getApplicationInfo(pkg, 0)
                    AppInfo(pkg, ai.loadLabel(pm).toString(), isInPanel = false, type = AppInfo.Type.APP)
                } catch (e: Exception) { null }
            }
            
            if (appInfos.isEmpty()) {
                findViewById<View>(R.id.layoutPickerNotifications).visibility = View.GONE
            } else {
                findViewById<View>(R.id.layoutPickerNotifications).visibility = View.VISIBLE
                notificationAdapter.setForceFreeform(true)
                notificationAdapter.setIsNotificationType(true)
                notificationAdapter.submitList(appInfos)
            }
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
        btnSearchClear = view.findViewById(R.id.btnSearchClear)
        btnSearchClear.setOnClickListener {
            etSearch.setText("")
        }

        btnTypeApps = view.findViewById(R.id.btnTypeApps)
        btnTypeActivities = view.findViewById(R.id.btnTypeActivities)
        
        // Hide type toggle for now
        view.findViewById<View>(R.id.layoutTypeToggle).visibility = View.GONE

        btnTypeApps.setOnClickListener {
            if (currentType != AppInfo.Type.APP) {
                currentType = AppInfo.Type.APP
                updateTypeToggleUI()
                loadApps(forceRefresh = true)
            }
        }

        btnTypeActivities.setOnClickListener {
            // Disabled for now
        }

        // Attempt to force floating keyboard for overlay panels
        etSearch.privateImeOptions = "nm" // This sometimes hints keyboards to stay compact
        etSearch.imeOptions = etSearch.imeOptions or android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN or android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI
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
            val cols = panelPrefs.pickerColumns
            val lm = GridLayoutManager(context, cols)
            lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter.getItemViewType(position)) {
                        VIEW_TYPE_HEADER, VIEW_TYPE_DIVIDER -> cols
                        else -> 1
                    }
                }
            }
            rvPickerGrid.layoutManager = lm
        }
        
        // --- PERFORMANCE OPTIMIZATIONS ---
        rvPickerGrid.setHasFixedSize(false)
        rvPickerGrid.setItemViewCacheSize(0)
        rvPickerGrid.setDrawingCacheEnabled(false)
        rvPickerGrid.recycledViewPool.setMaxRecycledViews(0, 0)
        
        rvPickerGrid.adapter = adapter

        rvPickerGrid.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                if (panelPrefs.rememberScroll) {
                    panelPrefs.lastPickerScroll = recyclerView.computeVerticalScrollOffset()
                }
            }
        })

        val rvNotifications = view.findViewById<RecyclerView>(R.id.rvPickerNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvNotifications.adapter = notificationAdapter

        val btnToggleNotifs = view.findViewById<View>(R.id.btnToggleNotifications)
        val ivChevron = view.findViewById<ImageView>(R.id.ivNotificationsChevron)
        btnToggleNotifs.setOnClickListener {
            val isHidden = rvNotifications.visibility == View.GONE
            rvNotifications.visibility = if (isHidden) View.VISIBLE else View.GONE
            findViewById<View>(R.id.divNotifications).visibility = rvNotifications.visibility
            ivChevron.rotation = if (isHidden) 90f else 0f
        }

        // Hide keyboard when scrolling the app list
        rvPickerGrid.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING) {
                    if (etSearch.hasFocus()) {
                        etSearch.clearFocus()
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
                    }
                }
            }
        })

        btnSettings.setOnClickListener {
            val intent = android.content.Intent(context, SettingsMainActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onAppLaunched?.invoke() 
        }

        btnEdit.setOnClickListener {
            setEditMode(!isEditMode)
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnSearchClear.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Pressing Enter or Search action launches the first result
        etSearch.setOnEditorActionListener { _, actionId, event ->
            val isEnter = (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN)
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
                isEnter) {
                if (adapter.launchFirstItem()) {
                    etSearch.clearFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
                    true
                } else false
            } else false
        }

        // Hide keyboard when tapping outside the search bar
        pickerPanelCard.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                if (etSearch.hasFocus()) {
                    etSearch.clearFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
                }
            }
            false // don't consume the event completely
        }

        applyTheme()
        loadApps()
    }

    private var lastMaxPx: Int = -1

    fun setMaxRecyclerViewHeight(maxPx: Int) {
        lastMaxPx = maxPx
        updatePickerHeight()
    }

    private fun updatePickerHeight() {
        if (lastMaxPx == -1) return
        
        val lp = rvPickerGrid.layoutParams
        
        // Use the total number of items to determine the "normal" height
        val itemsCount = if (etSearch.text.isEmpty()) adapter.itemCount else allApps.size
        
        if (itemsCount == 0) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            rvPickerGrid.layoutParams = lp
            return
        }

        val density = context.resources.displayMetrics.density
        val theme = panelPrefs.uiTheme
        val isRich = theme == PanelPreferences.THEME_RICH
        val cols = if (isRich) 1 else panelPrefs.pickerColumns
        
        val itemHeightDp = if (isRich) 72 else 88
        val itemHeightPx = (itemHeightDp * density).toInt()
        val rows = Math.ceil(itemsCount.toDouble() / cols).toInt()
        val estimatedContentHeightPx = rows * itemHeightPx

        if (estimatedContentHeightPx < lastMaxPx) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        } else {
            lp.height = lastMaxPx
        }
        rvPickerGrid.layoutParams = lp
    }

    fun applyTheme() {
        updateColumns()
        val theme = panelPrefs.uiTheme
        val density = context.resources.displayMetrics.density
        
        val drawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        }
        
        val customBg = try { Color.parseColor(panelPrefs.panelBackgroundColor) } catch (e: Exception) { Color.parseColor("#E61A1C1E") }
        val customAccent = try { Color.parseColor(panelPrefs.accentColor) } catch (e: Exception) { Color.parseColor("#4A9EFF") }
        val hasCustomAccent = panelPrefs.useCustomAccent || theme == PanelPreferences.THEME_RICH

        val themeBgColor = if (panelPrefs.useCustomPanelBg) {
            customBg
        } else {
            when (theme) {
                PanelPreferences.THEME_ORIGIN -> Color.parseColor("#1F1F1F")
                PanelPreferences.THEME_HYPEROS -> Color.parseColor("#E6252525")
                PanelPreferences.THEME_REALME -> Color.parseColor("#333333")
                else -> Color.parseColor(PanelPreferences.DEFAULT_PANEL_BG)
            }
        }
        drawable.setColor(themeBgColor)
        
        val finalRadius = if (theme == PanelPreferences.THEME_HYPEROS) 16f else panelPrefs.panelCornerRadius.toFloat()
        drawable.cornerRadius = finalRadius * density

        if (hasCustomAccent) {
            val strokeWidth = (if (theme == PanelPreferences.THEME_RICH) 2 * density else 1.5 * density).toInt()
            drawable.setStroke(strokeWidth, customAccent)
        } else if (theme == PanelPreferences.THEME_HYPEROS) {
            drawable.setStroke((1.5 * density).toInt(), Color.parseColor("#4DFFFFFF"))
        } else if (theme == PanelPreferences.THEME_REALME) {
            if (!panelPrefs.useCustomPanelBg) {
                val color1 = Color.parseColor("#333333")
                val color2 = Color.parseColor("#1A1A1A")
                drawable.colors = intArrayOf(color1, color2)
                drawable.orientation = android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM
            }
            drawable.setStroke((1 * density).toInt(), Color.parseColor("#33FFFFFF"))
        }
        
        pickerPanelCard.background = drawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) pickerPanelCard.clipToOutline = true

        val textColor = Color.WHITE
        val subTextColor = Color.parseColor("#B3FFFFFF")

        tvHeader.setTextColor(textColor)
        btnEdit.setTextColor(if (isEditMode) Color.parseColor("#4A9EFF") else subTextColor)
        etSearch.setTextColor(textColor)
        etSearch.setHintTextColor(subTextColor)

        val searchBg = findViewById<View>(R.id.layoutSearchCapsule)
        val pillBgProducer = {
            android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 21 * density
                val baseColor = themeBgColor
                val alpha = (Color.alpha(baseColor) * 0.65f).toInt().coerceIn(30, 200)
                val r = (Color.red(baseColor) * 0.85f).toInt()
                val g = (Color.green(baseColor) * 0.85f).toInt()
                val b = (Color.blue(baseColor) * 0.85f).toInt()
                setColor(Color.argb(alpha, r, g, b))
                setStroke((1 * density).toInt(), Color.parseColor("#26FFFFFF"))
            }
        }
        searchBg?.background = pillBgProducer()
        btnSettings.background = pillBgProducer()
        
        adapter.setIsLightMode(false)
        notificationAdapter.setIsLightMode(false)
    }

    fun setEditMode(enabled: Boolean) {
        if (isEditMode == enabled) return
        isEditMode = enabled
        tvHeader.text = if (isEditMode) context.getString(R.string.picker_manage_panel) else context.getString(R.string.picker_all_apps)
        btnEdit.text = if (isEditMode) context.getString(R.string.picker_btn_done) else context.getString(R.string.picker_btn_edit)
        
        val accentColor = try {
            if (panelPrefs.useCustomAccent) Color.parseColor(panelPrefs.accentColor)
            else Color.parseColor("#4A9EFF")
        } catch (e: Exception) { Color.parseColor("#4A9EFF") }

        btnEdit.setTextColor(if (isEditMode) accentColor else Color.parseColor("#4A9EFF"))
        adapter.notifyItemRangeChanged(0, adapter.itemCount, "EDIT_MODE_CHANGE")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        _scope.coroutineContext[Job]!!.cancel()
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_UP && event.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            onClose?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun updateTypeToggleUI() {
        val accentColor = try {
            if (panelPrefs.useCustomAccent) Color.parseColor(panelPrefs.accentColor)
            else Color.parseColor("#4A9EFF")
        } catch (e: Exception) { Color.parseColor("#4A9EFF") }

        if (currentType == AppInfo.Type.APP) {
            btnTypeApps.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1AFFFFFF"))
            btnTypeApps.setTextColor(Color.WHITE)
            btnTypeActivities.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            btnTypeActivities.setTextColor(Color.parseColor("#80FFFFFF"))
        } else {
            btnTypeActivities.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1AFFFFFF"))
            btnTypeActivities.setTextColor(Color.WHITE)
            btnTypeApps.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            btnTypeApps.setTextColor(Color.parseColor("#80FFFFFF"))
        }
    }

    private fun buildDisplayList(query: String = etSearch.text.toString()): List<PickerItem> {
        val list = mutableListOf<PickerItem>()
        if (query.isEmpty()) {
            if (panelPrefs.showRecommendedApps && recommendedApps.isNotEmpty()) {
                list.add(PickerItem.Header(
                    title = context.getString(R.string.picker_recommended_apps),
                    actionText = "Hide",
                    onAction = {
                        panelPrefs.showRecommendedApps = false
                        submitCurrentList()
                    }
                ))
                recommendedApps.forEach { list.add(PickerItem.App(it, isRecommended = true)) }
                list.add(PickerItem.Divider("rec_div"))
            }
            allApps.forEach { list.add(PickerItem.App(it, isRecommended = false)) }
        } else {
            val filtered = allApps.filter { it.appName.contains(query, ignoreCase = true) }
            filtered.forEach { list.add(PickerItem.App(it, isRecommended = false)) }
        }
        return list
    }

    private fun submitCurrentList(onComplete: (() -> Unit)? = null) {
        val displayList = buildDisplayList()
        adapter.submitList(displayList) {
            updatePickerHeight()
            onComplete?.invoke()
        }
    }

    fun loadApps(forceRefresh: Boolean = false) {
        updateColumns()
        _scope.launch {
            val recommended = withContext(Dispatchers.IO) {
                repository.getRecommendedApps(6)
            }
            recommendedApps = recommended

            if (!forceRefresh && allApps.isNotEmpty()) {
                val panelIdentifiers = panelPrefs.getPanelApps().toSet()
                allApps.forEach { 
                    it.isInPanel = panelIdentifiers.contains(it.identifier)
                }
                recommendedApps.forEach {
                    it.isInPanel = panelIdentifiers.contains(it.identifier)
                }
                withContext(Dispatchers.Main) {
                    submitCurrentList()
                }
                return@launch
            }

            // --- SHOW LOADING STATE ---
            withContext(Dispatchers.Main) {
                tvHeader.text = if (currentType == AppInfo.Type.ACTIVITY) "Scanning Activities..." else "Loading Apps..."
            }
            
            val apps = withContext(Dispatchers.IO) { 
                if (currentType == AppInfo.Type.ACTIVITY) repository.getAllActivities()
                else repository.getAllApps()
            }
            allApps = apps
            
            // --- RESTORE HEADER STATE ---
            withContext(Dispatchers.Main) {
                tvHeader.text = if (isEditMode) context.getString(R.string.picker_manage_panel) else (if (currentType == AppInfo.Type.ACTIVITY) context.getString(R.string.picker_all_activities) else context.getString(R.string.picker_all_apps))

                submitCurrentList {
                    val query = etSearch.text.toString()
                    if (panelPrefs.rememberScroll && query.isEmpty()) {
                        rvPickerGrid.post {
                            rvPickerGrid.scrollBy(0, panelPrefs.lastPickerScroll)
                        }
                    } else {
                        rvPickerGrid.post {
                            rvPickerGrid.scrollToPosition(0)
                        }
                    }

                    // --- STAGGERED ENTRY ANIMATION ---
                    rvPickerGrid.post {
                        val layoutManager = rvPickerGrid.layoutManager ?: return@post
                        val childCount = layoutManager.childCount
                        if (childCount == 0) return@post

                        for (i in 0 until childCount) {
                            val v = layoutManager.getChildAt(i) ?: continue
                            v.alpha = 0f
                            v.translationY = 50f
                            v.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(400)
                                .setStartDelay(i * 30L)
                                .setInterpolator(android.view.animation.DecelerateInterpolator())
                                .start()
                        }
                    }
                }
            }
        }
    }

    fun updateColumns() {
        if (panelPrefs.uiTheme != PanelPreferences.THEME_RICH) {
            val cols = panelPrefs.pickerColumns
            val lm = rvPickerGrid.layoutManager as? GridLayoutManager
            if (lm == null || lm.spanCount != cols) {
                val newLm = GridLayoutManager(context, cols)
                newLm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (adapter.getItemViewType(position)) {
                            VIEW_TYPE_HEADER, VIEW_TYPE_DIVIDER -> cols
                            else -> 1
                        }
                    }
                }
                rvPickerGrid.layoutManager = newLm
            }
            adapter.notifyDataSetChanged()
        }
    }

    fun resetSearch() {
        btnSearchClear.visibility = View.GONE
        if (etSearch.text.isNotEmpty()) {
            etSearch.setText("")
            submitCurrentList {
                // --- STAGGERED ENTRY ANIMATION ---
                rvPickerGrid.post {
                    val layoutManager = rvPickerGrid.layoutManager ?: return@post
                    for (i in 0 until layoutManager.childCount) {
                        val v = layoutManager.getChildAt(i) ?: continue
                        v.alpha = 0f
                        v.translationY = 50f
                        v.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(400)
                            .setStartDelay(i * 30L)
                            .setInterpolator(android.view.animation.DecelerateInterpolator())
                            .start()
                    }
                }
            }
        }
    }

    fun handleKeyboard() {
        if (!panelPrefs.autoShowKeyboard) {
            etSearch.clearFocus()
            pickerPanelCard.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
        } else {
            etSearch.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun invalidateAppList() {
        allApps = listOf()
        recommendedApps = listOf()
    }

    fun clearIcons() {
        adapter.refreshIcons()
        notificationAdapter.refreshIcons()
    }

    private fun filter(query: String) {
        submitCurrentList {
            // --- STAGGERED ENTRY ANIMATION FOR SEARCH RESULTS ---
            rvPickerGrid.post {
                val layoutManager = rvPickerGrid.layoutManager ?: return@post
                for (i in 0 until layoutManager.childCount) {
                    val v = layoutManager.getChildAt(i) ?: continue
                    v.alpha = 0f
                    v.translationY = 30f
                    v.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .setStartDelay(i * 20L)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                }
            }
        }
    }

    fun getPickerCardRect(outRect: android.graphics.Rect) {
        pickerPanelCard.getGlobalVisibleRect(outRect)
    }

    inner class NotificationAdapter : androidx.recyclerview.widget.ListAdapter<AppInfo, PickerViewHolder>(AppDiffCallback()) {
        private var isLightMode: Boolean = false
        private var forceFreeform: Boolean = false
        private var isNotificationType: Boolean = true

        fun setIsLightMode(isLight: Boolean) {
            isLightMode = isLight
            notifyDataSetChanged()
        }

        fun setForceFreeform(force: Boolean) {
            forceFreeform = force
        }

        fun setIsNotificationType(value: Boolean) {
            isNotificationType = value
        }

        fun refreshIcons() {
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_picker_notification, parent, false)
            return PickerViewHolder(view)
        }

        override fun onBindViewHolder(holder: PickerViewHolder, position: Int) {
            val app = getItem(position)
            val scale = context.getAutoScalingFactor() * panelPrefs.scaleFactor
            val baseIconSize = 44
            val baseTextSize = 10f

            holder.ivIcon.layoutParams.let { lp ->
                lp.width = (context.dpToPx(baseIconSize) * scale).toInt()
                lp.height = (context.dpToPx(baseIconSize) * scale).toInt()
                holder.ivIcon.layoutParams = lp
            }
            holder.tvName.textSize = baseTextSize * scale
            holder.tvName.visibility = if (panelPrefs.showAppNamesDrawer) View.VISIBLE else View.GONE
            holder.tvName.text = app.appName
            holder.tvPackage?.visibility = View.GONE

            val textColor = if (isLightMode) Color.parseColor("#1E293B") else Color.WHITE
            holder.tvName.setTextColor(textColor)

            Glide.with(holder.itemView.context).clear(holder.ivIcon)
            Glide.with(holder.itemView.context)
                .load(AppIconRequest(app.packageName, panelPrefs.appearanceKey))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .error(android.R.drawable.sym_def_app_icon)
                .override((120 * scale).toInt(), (120 * scale).toInt())
                .into(holder.ivIcon)

            IconShapeHelper.applyShape(holder.ivIcon, panelPrefs.iconShape)
            holder.ivCheck.visibility = View.GONE

            holder.itemView.setOnClickListener {
                if (panelPrefs.hapticEnabled) {
                    it.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                }
                adapter.launchApp(app)
            }
        }
    }

    inner class PickerAdapter : androidx.recyclerview.widget.ListAdapter<PickerItem, RecyclerView.ViewHolder>(PickerItemDiffCallback()) {

        private var accentColor: Int = Color.parseColor("#4DFFFFFF")
        private var accentColorStateList: android.content.res.ColorStateList = android.content.res.ColorStateList.valueOf(accentColor)
        private var forceFreeform: Boolean = false
        private var isLightMode: Boolean = false

        init {
            updateAccentColor()
        }

        fun setIsLightMode(isLight: Boolean) {
            isLightMode = isLight
            updateAccentColor()
            notifyDataSetChanged()
        }

        fun setForceFreeform(force: Boolean) {
            forceFreeform = force
        }

        fun updateAccentColor() {
            accentColor = try {
                if (panelPrefs.useCustomAccent) {
                    Color.parseColor(panelPrefs.accentColor)
                } else {
                    if (isLightMode) Color.parseColor("#4F46E5") else Color.parseColor("#4DFFFFFF")
                }
            } catch (e: Exception) {
                if (isLightMode) Color.parseColor("#4F46E5") else Color.parseColor("#4DFFFFFF")
            }
            accentColorStateList = android.content.res.ColorStateList.valueOf(accentColor)
        }

        fun refreshIcons() {
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return when (getItem(position)) {
                is PickerItem.App -> VIEW_TYPE_APP
                is PickerItem.Header -> VIEW_TYPE_HEADER
                is PickerItem.Divider -> VIEW_TYPE_DIVIDER
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                VIEW_TYPE_HEADER -> {
                    val view = inflater.inflate(R.layout.item_picker_section_header, parent, false)
                    HeaderViewHolder(view)
                }
                VIEW_TYPE_DIVIDER -> {
                    val view = inflater.inflate(R.layout.item_picker_divider, parent, false)
                    DividerViewHolder(view)
                }
                else -> {
                    val layoutId = if (panelPrefs.uiTheme == PanelPreferences.THEME_RICH) {
                        R.layout.item_picker_app_rich
                    } else {
                        R.layout.item_picker_app_modern
                    }
                    val view = inflater.inflate(layoutId, parent, false)
                    PickerViewHolder(view)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is HeaderViewHolder -> {
                    val headerItem = getItem(position) as PickerItem.Header
                    holder.tvTitle.text = headerItem.title
                    if (headerItem.actionText != null && headerItem.onAction != null) {
                        holder.btnAction.visibility = View.VISIBLE
                        holder.btnAction.text = headerItem.actionText
                        holder.btnAction.setOnClickListener { headerItem.onAction.invoke() }
                    } else {
                        holder.btnAction.visibility = View.GONE
                    }
                }
                is DividerViewHolder -> {
                    // Divider line layout
                }
                is PickerViewHolder -> {
                    val appItem = getItem(position) as PickerItem.App
                    val app = appItem.app
                    val scale = context.getAutoScalingFactor() * panelPrefs.scaleFactor
                    val isRich = panelPrefs.uiTheme == PanelPreferences.THEME_RICH
                    val cols = panelPrefs.pickerColumns

                    val baseIconSize = if (isRich) 48 else (if (cols == 3) 38 else 44)
                    val baseTextSize = if (isRich) 11f else (if (cols == 3) 10f else 11f)
                    val basePkgTextSize = if (isRich) 10f else 9f

                    holder.ivIcon.layoutParams.let { lp ->
                        lp.width = (context.dpToPx(baseIconSize) * scale).toInt()
                        lp.height = (context.dpToPx(baseIconSize) * scale).toInt()
                        holder.ivIcon.layoutParams = lp
                    }
                    holder.tvName.textSize = baseTextSize * scale
                    holder.tvPackage?.textSize = basePkgTextSize * scale

                    val showNames = panelPrefs.showAppNamesDrawer
                    if (showNames) {
                        holder.tvName.visibility = View.VISIBLE
                        holder.tvName.text = app.appName
                        if (isRich) {
                            holder.tvPackage?.visibility = View.VISIBLE
                            holder.tvPackage?.text = app.packageName
                        } else {
                            holder.tvPackage?.visibility = View.GONE
                        }
                    } else {
                        holder.tvName.visibility = View.GONE
                        holder.tvPackage?.visibility = View.GONE
                    }

                    val textColor = if (isLightMode) Color.parseColor("#1E293B") else Color.WHITE
                    val subTextColor = if (isLightMode) Color.parseColor("#64748B") else Color.parseColor("#B3FFFFFF")
                    holder.tvName.setTextColor(textColor)
                    holder.tvPackage?.setTextColor(subTextColor)

                    Glide.with(holder.itemView.context).clear(holder.ivIcon)
                    Glide.with(holder.itemView.context)
                        .load(AppIconRequest(app.packageName, panelPrefs.appearanceKey))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(android.R.drawable.sym_def_app_icon)
                        .error(android.R.drawable.sym_def_app_icon)
                        .override((120 * scale).toInt(), (120 * scale).toInt())
                        .into(holder.ivIcon)

                    IconShapeHelper.applyShape(holder.ivIcon, panelPrefs.iconShape)

                    holder.itemView.setOnClickListener {
                        val currentPos = holder.bindingAdapterPosition
                        if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener
                        val currentItem = getItem(currentPos) as? PickerItem.App ?: return@setOnClickListener
                        val currentApp = currentItem.app

                        if (isEditMode) {
                            if (panelPrefs.hapticEnabled) {
                                it.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            }
                            toggleAppSelection(currentApp, currentPos, holder.ivCheck)
                        } else {
                            if (panelPrefs.hapticEnabled) {
                                it.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                            }
                            launchApp(currentApp)
                        }
                    }

                    holder.ivCheck.setOnClickListener {
                        val currentPos = holder.bindingAdapterPosition
                        if (currentPos != RecyclerView.NO_POSITION && isEditMode) {
                            val currentItem = getItem(currentPos) as? PickerItem.App ?: return@setOnClickListener
                            if (panelPrefs.hapticEnabled) {
                                it.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            }
                            toggleAppSelection(currentItem.app, currentPos, holder.ivCheck)
                        }
                    }

                    val isSelected = panelPrefs.isInPanel(app.identifier)
                    if (isEditMode) {
                        holder.ivCheck.visibility = View.VISIBLE
                        val iconTint = if (isSelected) accentColorStateList else android.content.res.ColorStateList.valueOf(Color.parseColor("#B3FFFFFF"))
                        if (holder.ivCheck is ImageView) {
                            holder.ivCheck.imageTintList = iconTint
                        }
                        holder.ivCheck.rotation = if (isSelected) 45f else 0f
                    } else {
                        holder.ivCheck.visibility = View.GONE
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else if (holder is PickerViewHolder) {
                val item = getItem(position) as? PickerItem.App ?: return
                val app = item.app
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

        private fun toggleAppSelection(app: AppInfo, position: Int, plusView: View) {
            val newState = !app.isInPanel
            app.isInPanel = newState
            onToggleApp?.invoke(app, newState)

            plusView.animate()
                .rotation(if (newState) 45f else 0f)
                .setDuration(320)
                .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                .start()

            if (plusView is ImageView) {
                val tint = if (newState) accentColorStateList
                           else android.content.res.ColorStateList.valueOf(Color.parseColor("#B3FFFFFF"))
                plusView.imageTintList = tint
            }

            notifyItemChanged(position, "TOGGLE_STATE")
        }

        fun launchFirstItem(): Boolean {
            val firstAppItem = currentList.filterIsInstance<PickerItem.App>().firstOrNull() ?: return false
            launchApp(firstAppItem.app)
            return true
        }

        fun launchApp(app: AppInfo) {
            panelPrefs.recordAppLaunch(app.packageName)
            val index = currentList.indexOfFirst { it is PickerItem.App && it.app.identifier == app.identifier }
            if (index != -1) {
                rvPickerGrid.findViewHolderForAdapterPosition(index)?.itemView?.let {
                    SpringAnimator.scalePulse(it)
                }
            }
            
            val intent = if (app.intentUri != null) {
                try {
                    android.content.Intent.parseUri(app.intentUri, android.content.Intent.URI_INTENT_SCHEME)
                } catch (e: Exception) {
                    context.packageManager.getLaunchIntentForPackage(app.packageName)
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(app.packageName)
            }

            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                
                val shouldFreeform = forceFreeform || (panelPrefs.freeformEnabled && context.isFreeformEnabled())
                
                if (shouldFreeform && context.isFreeformEnabled() && app.type != AppInfo.Type.SHORTCUT) {
                    launchFreeform(intent)
                } else {
                    context.startActivity(intent)
                }
            }
            onAppLaunched?.invoke()
        }

        @android.annotation.SuppressLint("BlockedPrivateApi")
        private fun launchFreeform(intent: android.content.Intent) {
            val pkg = intent.`package` ?: intent.component?.packageName ?: return
            
            if (panelPrefs.useShellForFreeform && AutomationManager.isAutomationPossible()) {
                val launched = AutomationManager.launchApp(context, pkg, panelPrefs.freeformWindowingModeInt)
                if (launched) return
            }

            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            
            if (panelPrefs.freeformWindowingModeInt == 5) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }

            intent.putExtra("android.intent.extra.WINDOWING_MODE", panelPrefs.freeformWindowingModeInt)
            intent.putExtra("android.intent.extra.LAUNCH_WINDOWING_MODE", panelPrefs.freeformWindowingModeInt)
            
            if (panelPrefs.freeformWindowingModeInt == 106) {
                intent.putExtra("vivo_small_window", true)
                intent.putExtra("is_vivo_freeform", true)
            }
            
            try {
                val options = android.app.ActivityOptions.makeBasic()

                val displayMetrics = context.resources.displayMetrics
                val w = displayMetrics.widthPixels
                val h = displayMetrics.heightPixels

                val prefersLandscape = detectLandscapeOrientation(intent.`package`, intent)

                val bounds: android.graphics.Rect? = when (panelPrefs.freeformWindowMode) {
                    PanelPreferences.FREEFORM_MODE_PORTRAIT -> {
                        val left = w / 3
                        val top = h / 15
                        android.graphics.Rect(left, top, w - left, h - top)
                    }
                    PanelPreferences.FREEFORM_MODE_MAXIMIZED -> {
                        android.graphics.Rect(0, 0, w, h)
                    }
                    PanelPreferences.FREEFORM_MODE_CUSTOM -> {
                        val winW = (w * panelPrefs.freeformCustomWidth / 100.0).toInt()
                        val winH = (h * panelPrefs.freeformCustomHeight / 100.0).toInt()
                        val left = (w - winW) / 2
                        val top = (h - winH) / 2
                        android.graphics.Rect(left, top, left + winW, top + winH)
                    }
                    else -> {
                        val factor = if (panelPrefs.freeformWindowingModeInt == 5) 0.8 else 0.7
                        
                        if (prefersLandscape) {
                            val targetW = if (w > h) (w * factor).toInt() else (w * (factor + 0.1)).toInt()
                            val targetH = (targetW / 1.77).toInt().coerceAtMost((h * (factor + 0.05)).toInt())
                            val left = (w - targetW) / 2
                            val top = (h - targetH) / 2
                            android.graphics.Rect(left, top, left + targetW, top + targetH)
                        } else {
                            val targetH = (h * (factor + 0.05)).toInt()
                            val targetW = (targetH * 9 / 16).toInt().coerceAtMost((w * factor).toInt())
                            val left = (w - targetW) / 2
                            val top = (h - targetH) / 2
                            android.graphics.Rect(left, top, left + targetW, top + targetH)
                        }
                    }
                }
                if (bounds != null) {
                    options.launchBounds = bounds
                }

                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        org.lsposed.hiddenapibypass.HiddenApiBypass.invoke(
                            android.app.ActivityOptions::class.java,
                            options,
                            "setLaunchWindowingMode",
                            panelPrefs.freeformWindowingModeInt
                        )
                    }
                } catch (e: Exception) {
                    // ignore
                }

                context.startActivity(intent, options.toBundle())
            } catch (e: Exception) {
                context.startActivity(intent)
            }
        }

        private fun detectLandscapeOrientation(packageName: String?, intent: android.content.Intent? = null): Boolean {
            val pkg = packageName ?: intent?.`package` ?: intent?.component?.packageName ?: return false
            return try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                val component = launchIntent?.component ?: return false
                val activityInfo = context.packageManager
                    .getActivityInfo(component, android.content.pm.PackageManager.GET_META_DATA)

                when (activityInfo.screenOrientation) {
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                    -> true
                    else -> false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    sealed class PickerItem(open val id: String) {
        data class Header(
            val title: String,
            val actionText: String? = null,
            val onAction: (() -> Unit)? = null
        ) : PickerItem("header_$title")

        data class Divider(
            val dividerId: String
        ) : PickerItem("divider_$dividerId")

        data class App(
            val app: AppInfo,
            val isRecommended: Boolean = false
        ) : PickerItem(if (isRecommended) "rec_${app.identifier}" else app.identifier)
    }

    private class PickerItemDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<PickerItem>() {
        override fun areItemsTheSame(oldItem: PickerItem, newItem: PickerItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PickerItem, newItem: PickerItem): Boolean {
            return when {
                oldItem is PickerItem.App && newItem is PickerItem.App -> {
                    oldItem.app.appName == newItem.app.appName &&
                    oldItem.app.isInPanel == newItem.app.isInPanel &&
                    oldItem.isRecommended == newItem.isRecommended
                }
                oldItem is PickerItem.Header && newItem is PickerItem.Header -> {
                    oldItem.title == newItem.title && oldItem.actionText == newItem.actionText
                }
                oldItem is PickerItem.Divider && newItem is PickerItem.Divider -> true
                else -> false
            }
        }
    }

    private class AppDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.identifier == newItem.identifier
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
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
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvSectionTitle)
        val btnAction: TextView = view.findViewById(R.id.btnSectionAction)
    }

    inner class DividerViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
