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
    private val notificationAdapter = PickerAdapter()
    
    private val repository = AppRepository(context)
    private val panelPrefs = PanelPreferences(context)
    private var allApps = listOf<AppInfo>()
    var isEditMode = false
        private set
    
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
        android.util.Log.d("AppPickerPanelView", "Active notification pkgs: ${pkgs.size}")
        
        if (pkgs.isEmpty()) {
            findViewById<View>(R.id.layoutPickerNotifications).visibility = View.GONE
        } else {
            val panelApps = panelPrefs.getPanelApps().toSet()
            val filteredPkgs = pkgs.filter { !panelApps.contains(it) }
            
            val appInfos = filteredPkgs.mapNotNull { pkg ->
                try {
                    val pm = context.packageManager
                    val ai = pm.getApplicationInfo(pkg, 0)
                    AppInfo(pkg, ai.loadLabel(pm).toString())
                } catch (e: Exception) { null }
            }
            
            android.util.Log.d("AppPickerPanelView", "Filtered appInfos: ${appInfos.size}")
            
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
            rvPickerGrid.layoutManager = GridLayoutManager(context, 2)
        }
        
        // --- PERFORMANCE OPTIMIZATIONS ---
        rvPickerGrid.setHasFixedSize(true)
        rvPickerGrid.setItemViewCacheSize(30)
        rvPickerGrid.setDrawingCacheEnabled(true)
        rvPickerGrid.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH)
        rvPickerGrid.recycledViewPool.setMaxRecycledViews(0, 50)
        
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
        rvNotifications.adapter = notificationAdapter.apply { setIsNotificationType(true) }

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
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

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
        val itemsCount = allApps.size
        if (itemsCount == 0) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            rvPickerGrid.layoutParams = lp
            return
        }

        val density = context.resources.displayMetrics.density
        val theme = panelPrefs.uiTheme
        val isRich = theme == PanelPreferences.THEME_RICH
        
        // Accurate estimation for Picker height
        // Modern: 100dp approx, Rich: 72dp approx
        val itemHeightDp = if (isRich) 72 else 100
        val itemHeightPx = (itemHeightDp * density).toInt()
        val cols = if (isRich) 1 else 2
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
        val theme = panelPrefs.uiTheme
        val density = context.resources.displayMetrics.density
        
        val drawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        }
        
        val themeBgColor = when (theme) {
            PanelPreferences.THEME_ORIGIN -> Color.parseColor("#1F1F1F") 
            PanelPreferences.THEME_HYPEROS -> Color.parseColor("#E6252525")
            else -> try { Color.parseColor(panelPrefs.panelBackgroundColor) } catch (e: Exception) { Color.parseColor("#E61A1C1E") }
        }
        drawable.setColor(themeBgColor)
        
        val finalRadius = if (theme == PanelPreferences.THEME_HYPEROS) 16f else panelPrefs.panelCornerRadius.toFloat()
        drawable.cornerRadius = finalRadius * density

        if (theme == PanelPreferences.THEME_HYPEROS) {
            drawable.setStroke((1.5 * density).toInt(), Color.parseColor("#4DFFFFFF"))
        } else if (theme == PanelPreferences.THEME_RICH) {
            val accent = try { Color.parseColor(panelPrefs.accentColor) } catch (e: Exception) { Color.parseColor("#4A9EFF") }
            drawable.setStroke((2 * density).toInt(), accent)
        } else if (theme == PanelPreferences.THEME_REALME) {
            val color1 = Color.parseColor("#333333")
            val color2 = Color.parseColor("#1A1A1A")
            drawable.colors = intArrayOf(color1, color2)
            drawable.orientation = android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM
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

        val searchBg = findViewById<View>(R.id.etPickerSearch).parent as? View
        searchBg?.let {
            val sd = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 22 * density
                
                // Slightly lighter than panel background to make it "pop" or darker to recessed
                val baseColor = themeBgColor
                val alpha = (Color.alpha(baseColor) * 0.5f).toInt()
                val r = (Color.red(baseColor) * 0.8f).toInt()
                val g = (Color.green(baseColor) * 0.8f).toInt()
                val b = (Color.blue(baseColor) * 0.8f).toInt()
                setColor(Color.argb(alpha, r, g, b))
                setStroke((1 * density).toInt(), Color.parseColor("#1AFFFFFF"))
            }
            it.background = sd
        }
        
        adapter.setIsLightMode(false)
        notificationAdapter.setIsLightMode(false)
    }

    fun setEditMode(enabled: Boolean) {
        if (isEditMode == enabled) return
        isEditMode = enabled
        tvHeader.text = if (isEditMode) "Manage Smart Edge" else "All Apps"
        btnEdit.text = if (isEditMode) "DONE" else "EDIT"
        
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

    fun loadApps(forceRefresh: Boolean = false) {
        if (!forceRefresh && allApps.isNotEmpty()) {
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
                adapter.notifyItemRangeChanged(0, adapter.itemCount, "PANEL_STATE_CHANGE")
            }
            return
        }
        _scope.launch {
            val apps = withContext(Dispatchers.IO) { repository.getAllApps() }
            allApps = apps
            adapter.submitList(allApps.toList()) {
                if (panelPrefs.rememberScroll) {
                    rvPickerGrid.post {
                        rvPickerGrid.scrollBy(0, panelPrefs.lastPickerScroll)
                    }
                } else {
                    rvPickerGrid.post {
                        rvPickerGrid.scrollToPosition(0)
                    }
                }
                updatePickerHeight()

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

    fun resetSearch() {
        if (etSearch.text.isNotEmpty()) {
            etSearch.setText("")
            adapter.submitList(allApps.toList()) {
                updatePickerHeight()
                
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
    }

    fun clearIcons() {
        adapter.refreshIcons()
    }

    private fun filter(query: String) {
        val filtered = allApps.filter { it.appName.contains(query, ignoreCase = true) }
        adapter.submitList(filtered) {
            updatePickerHeight()
            
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

    inner class PickerAdapter : androidx.recyclerview.widget.ListAdapter<AppInfo, PickerViewHolder>(AppDiffCallback()) {

        private var accentColor: Int = Color.parseColor("#4DFFFFFF")
        private var accentColorStateList: android.content.res.ColorStateList = android.content.res.ColorStateList.valueOf(accentColor)
        private var forceFreeform: Boolean = false
        private var isNotificationType: Boolean = false
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

        fun setIsNotificationType(value: Boolean) {
            isNotificationType = value
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickerViewHolder {
            val layoutId = if (isNotificationType) {
                R.layout.item_picker_notification
            } else if (panelPrefs.uiTheme == PanelPreferences.THEME_RICH) {
                R.layout.item_picker_app_rich
            } else {
                R.layout.item_picker_app_modern
            }
            
            val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
            return PickerViewHolder(view)
        }

        override fun onBindViewHolder(holder: PickerViewHolder, position: Int) {
            val app = getItem(position)
            if (isNotificationType) {
                android.util.Log.d("AppPickerPanelView", "Binding notification app: ${app.appName} (${app.packageName})")
            }
            
            val scale = context.getAutoScalingFactor() * panelPrefs.scaleFactor
            val isRich = panelPrefs.uiTheme == PanelPreferences.THEME_RICH
            
            // Notification icons should be a bit more compact but still respect user scale
            val baseIconSize = if (isNotificationType) 44 else (if (isRich) 48 else 44)
            val baseTextSize = if (isNotificationType) 10f else (if (isRich) 11f else 10f)
            val basePkgTextSize = if (isRich) 10f else 9f

            holder.ivIcon.layoutParams.let { lp ->
                lp.width = (context.dpToPx(baseIconSize) * scale).toInt()
                lp.height = (context.dpToPx(baseIconSize) * scale).toInt()
                holder.ivIcon.layoutParams = lp
            }
            holder.tvName.textSize = baseTextSize * scale
            holder.tvPackage?.textSize = basePkgTextSize * scale

            holder.tvName.text = app.appName
            holder.tvPackage?.text = app.packageName
            
            val textColor = if (isLightMode) Color.parseColor("#1E293B") else Color.WHITE
            val subTextColor = if (isLightMode) Color.parseColor("#64748B") else Color.parseColor("#B3FFFFFF")
            holder.tvName.setTextColor(textColor)
            holder.tvPackage?.setTextColor(subTextColor)

            // --- OPTIMIZED ICON LOADING WITH GLIDE ---
            Glide.with(holder.itemView.context)
                .load(AppIconRequest(app.packageName, panelPrefs.selectedIconPack))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .override((120 * scale).toInt(), (120 * scale).toInt())
                .into(holder.ivIcon)

            IconShapeHelper.applyShape(holder.ivIcon, panelPrefs.iconShape)

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

            holder.itemView.setOnClickListener {
                if (!isEditMode) {
                    if (panelPrefs.hapticEnabled) {
                        holder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    }
                    launchApp(app)
                }
            }

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

        private fun toggleAppSelection(app: AppInfo, position: Int, plusView: View) {
            val newState = !app.isInPanel
            app.isInPanel = newState
            onToggleApp?.invoke(app, newState)

            plusView.animate()
                .rotation(if (newState) 45f else 0f)
                .setDuration(200)
                .start()

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
                
                val shouldFreeform = forceFreeform || (panelPrefs.freeformEnabled && context.isFreeformEnabled())
                
                if (shouldFreeform && context.isFreeformEnabled()) {
                    launchFreeform(intent)
                } else {
                    context.startActivity(intent)
                }
            }
            onAppLaunched?.invoke()
        }

        @android.annotation.SuppressLint("BlockedPrivateApi")
        private fun launchFreeform(intent: android.content.Intent) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            try {
                val options = android.app.ActivityOptions
                    .makeCustomAnimation(context, android.R.anim.fade_in, 0)

                val displayMetrics = context.resources.displayMetrics
                val w = displayMetrics.widthPixels
                val h = displayMetrics.heightPixels

                val prefersLandscape = detectLandscapeOrientation(intent.`package`)

                val bounds: android.graphics.Rect = when (panelPrefs.freeformWindowMode) {
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
                        if (prefersLandscape) {
                            // 16:9 wide aspect for games/landscape apps
                            val targetW = if (w > h) (w * 0.80).toInt() else (w * 0.90).toInt()
                            val targetH = (targetW / 1.77).toInt().coerceAtMost((h * 0.85).toInt())
                            val left = (w - targetW) / 2
                            val top = (h - targetH) / 2
                            android.graphics.Rect(left, top, left + targetW, top + targetH)
                        } else {
                            // 9:16 portrait aspect for normal apps (fixed for landscape host)
                            val targetH = (h * 0.85).toInt()
                            val targetW = (targetH * 9 / 16).toInt().coerceAtMost((w * 0.85).toInt())
                            val left = (w - targetW) / 2
                            val top = (h - targetH) / 2
                            android.graphics.Rect(left, top, left + targetW, top + targetH)
                        }
                    }
                }
                options.launchBounds = bounds

                // Use HiddenApiBypass instead of direct reflection to avoid F-Droid lint errors
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        org.lsposed.hiddenapibypass.HiddenApiBypass.invoke(
                            android.app.ActivityOptions::class.java,
                            options,
                            "setLaunchWindowingMode",
                            5
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

        private fun detectLandscapeOrientation(packageName: String?): Boolean {
            if (packageName == null) return false
            return try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
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
}
