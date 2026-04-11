package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.GridLayoutManager
import com.imi.smartedge.sidebar.panel.databinding.SidePanelLayoutBinding

/**
 * High-performance Side Panel using RecyclerView.
 */
class SidePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onClose: (() -> Unit)? = null
    var onAppsChanged: (() -> Unit)? = null
    var onAddClick: ((Boolean) -> Unit)? = null
    var onScreenshot: (() -> Unit)? = null

    private val binding: SidePanelLayoutBinding = SidePanelLayoutBinding.inflate(LayoutInflater.from(context), this, true)
    private val adapter: PanelAppsAdapter
    private val panelPrefs = PanelPreferences(context)
    private var currentCols = 1
    private var isPickerOpenInternal = false

    private val springRotation: SpringAnimation = SpringAnimation(binding.btnClose, SpringAnimation.ROTATION)

    private val width1ColDp = 72f
    private val width2ColDp = 140f

    private fun getFinalScaleFactor(): Float {
        return context.getAutoScalingFactor() * panelPrefs.scaleFactor
    }

    private val updateHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateSystemInfo()
            updateHandler.postDelayed(this, 3000)
        }
    }

    private fun updateSystemInfo() {
        if (!panelPrefs.showSysInfo) return

        try {
            val mi = android.app.ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.getMemoryInfo(mi)
            val availableMegs = mi.availMem / 1048576L
            val totalMegs = mi.totalMem / 1048576L
            val usedMegs = totalMegs - availableMegs
            binding.tvRamUsage.text = "RAM: ${usedMegs}MB"

            val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val temp = intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            binding.tvBatTemp.text = "BAT: ${temp / 10}°C"
        } catch (e: Exception) {}
    }

    private val gestureDetector = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
            if (Math.abs(velocityY) > Math.abs(velocityX)) return false
            if (isRight && velocityX > 1200f) {
                onClose?.invoke()
                return true
            } else if (!isRight && velocityX < -1200f) {
                onClose?.invoke()
                return true
            }
            return false
        }
    })

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    init {
        springRotation.spring = SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            stiffness = SpringForce.STIFFNESS_MEDIUM
        }

        binding.panelCard.setOnClickListener { }

        adapter = PanelAppsAdapter(
            context,
            onRemove = { removedApp ->
                panelPrefs.removeApp(removedApp.packageName)
                onAppsChanged?.invoke()
            },
            onAddClick = { isEdit -> onAddClick?.invoke(isEdit) },
            onAppLaunched = { onClose?.invoke() }
        )

        currentCols = panelPrefs.panelColumns
        adapter.setColumns(currentCols)
        binding.rvPanelApps.layoutManager = GridLayoutManager(context, currentCols)
        binding.rvPanelApps.adapter = adapter

        binding.rvPanelApps.setHasFixedSize(true)
        binding.rvPanelApps.isNestedScrollingEnabled = false
        binding.rvPanelApps.setItemViewCacheSize(30)
        (binding.rvPanelApps.itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)?.supportsChangeAnimations = false
        binding.rvPanelApps.recycledViewPool.setMaxRecycledViews(0, 30)

        updateSideLayout()

        binding.btnClose.setOnClickListener {
            if (panelPrefs.hapticEnabled) {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            }
            SpringAnimator.scalePulse(it)
            onAddClick?.invoke(false)
        }

        binding.btnScreenshot.setOnClickListener {
            if (panelPrefs.hapticEnabled) {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            }
            SpringAnimator.scalePulse(it)
            onScreenshot?.invoke()
        }

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val actionRunnables = mutableMapOf<Int, Runnable>()
        
        var indicatorText: android.widget.TextView? = null
        var indicatorFadeRunnable: Runnable? = null

        val showIndicator = { text: String ->
            val root = parent as? android.widget.FrameLayout
            if (root != null) {
                if (indicatorText == null) {
                    val density = context.resources.displayMetrics.density

                    indicatorText = android.widget.TextView(context).apply {
                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 14f
                        setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
                        gravity = android.view.Gravity.CENTER
                        
                        // Custom Toast-like rounded background
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(android.graphics.Color.parseColor("#E6303030"))
                            cornerRadius = 24f * density
                        }

                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                            bottomMargin = (90 * density).toInt()
                        }
                        elevation = 8f * density
                    }
                    root.addView(indicatorText)
                }

                indicatorText?.text = text
                indicatorText?.visibility = View.VISIBLE
                indicatorText?.alpha = 1f
                indicatorText?.animate()?.cancel()
                
                indicatorFadeRunnable?.let { handler.removeCallbacks(it) }
                indicatorFadeRunnable = Runnable {
                    indicatorText?.animate()
                        ?.alpha(0f)
                        ?.setDuration(300)
                        ?.withEndAction { indicatorText?.visibility = View.GONE }
                        ?.start()
                }
                handler.postDelayed(indicatorFadeRunnable!!, 1500)
            }
        }

        val performVolumeChange = { direction: Int, view: View ->
            if (panelPrefs.hapticEnabled) view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            SpringAnimator.scalePulse(view)
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, direction, 0)
            
            val current = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val percent = if (max > 0) (current * 100) / max else 0
            showIndicator("Volume: $percent%")
        }

        // Click listeners for single taps
        binding.btnVolumeUp.setOnClickListener { performVolumeChange(android.media.AudioManager.ADJUST_RAISE, it) }
        binding.btnVolumeDown.setOnClickListener { performVolumeChange(android.media.AudioManager.ADJUST_LOWER, it) }

        // Long click for continuous repeat
        val setupVolumeRepeat = { btn: View, direction: Int ->
            btn.setOnLongClickListener { v ->
                val runnable = object : Runnable {
                    override fun run() {
                        performVolumeChange(direction, v)
                        handler.postDelayed(this, 100)
                    }
                }
                actionRunnables[v.id] = runnable
                handler.post(runnable)
                true
            }
        }
        setupVolumeRepeat(binding.btnVolumeUp, android.media.AudioManager.ADJUST_RAISE)
        setupVolumeRepeat(binding.btnVolumeDown, android.media.AudioManager.ADJUST_LOWER)

        val performBrightnessChange = { direction: Int, view: View ->
            if (panelPrefs.hapticEnabled) view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            SpringAnimator.scalePulse(view)
            try {
                val cResolver = context.contentResolver
                var brightness = android.provider.Settings.System.getInt(cResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, 125)
                brightness = (brightness + direction).coerceIn(0, 255)
                android.provider.Settings.System.putInt(cResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness)
                
                val percent = (brightness * 100) / 255
                showIndicator("Brightness: $percent%")
            } catch (e: Exception) {
                actionRunnables[view.id]?.let { handler.removeCallbacks(it) }
                android.widget.Toast.makeText(context, "Requires 'Write System Settings' permission", android.widget.Toast.LENGTH_SHORT).show()
                try {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (ex: Exception) {}
            }
        }

        binding.btnBrightnessUp.setOnClickListener { performBrightnessChange(15, it) }
        binding.btnBrightnessDown.setOnClickListener { performBrightnessChange(-15, it) }

        val setupBrightnessRepeat = { btn: View, direction: Int ->
            btn.setOnLongClickListener { v ->
                val runnable = object : Runnable {
                    override fun run() {
                        performBrightnessChange(direction, v)
                        handler.postDelayed(this, 100)
                    }
                }
                actionRunnables[v.id] = runnable
                handler.post(runnable)
                true
            }
        }
        setupBrightnessRepeat(binding.btnBrightnessUp, 15)
        setupBrightnessRepeat(binding.btnBrightnessDown, -15)

        // Cancel repeat on release
        @android.annotation.SuppressLint("ClickableViewAccessibility")
        val stopRepeatListener = View.OnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP || event.action == android.view.MotionEvent.ACTION_CANCEL) {
                actionRunnables[v.id]?.let { handler.removeCallbacks(it) }
                actionRunnables.remove(v.id)
            }
            false // Let click and long click propagate
        }
        
        binding.btnVolumeUp.setOnTouchListener(stopRepeatListener)
        binding.btnVolumeDown.setOnTouchListener(stopRepeatListener)
        binding.btnBrightnessUp.setOnTouchListener(stopRepeatListener)
        binding.btnBrightnessDown.setOnTouchListener(stopRepeatListener)

        binding.btnReboot.setOnClickListener {
            if (panelPrefs.hapticEnabled) {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            }
            SpringAnimator.scalePulse(it)
            
            // Trigger System Power Menu via Accessibility Service
            val intent = Intent(context, PanelAccessibilityService::class.java).apply {
                action = PanelAccessibilityService.ACTION_SHOW_POWER_MENU
            }
            context.startService(intent)
            onClose?.invoke()
        }

        applyTheme()
    }

    fun updateSideLayout() {
        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        binding.btnClose.rotation = if (isRight) 180f else 0f

        val scale = getFinalScaleFactor()
        val lp = binding.panelCard.layoutParams
        
        // Scale only the icon area, keeping the padding/chrome fixed
        val newWidthDp = if (currentCols == 2) {
            52f + (88f * scale)
        } else {
            32f + (40f * scale)
        }
        
        lp.width = context.dpToPx(newWidthDp.toInt())
        binding.panelCard.layoutParams = lp

        // Calculate maximum allowed height for the RecyclerView to ensure the panel fits on screen
        val displayMetrics = context.resources.displayMetrics
        val screenHeightPx = displayMetrics.heightPixels
        val screenHeightDp = screenHeightPx / displayMetrics.density
        
        // Subtract estimated height of other UI elements (paddings, tools, close button)
        // Top Padding (12) + Bottom Padding (4) + Tools Margin (4) + Close Btn (48) = 68dp
        var nonAppHeightDp = 68f
        if (panelPrefs.showTools) {
            nonAppHeightDp += 50f // Divider + Screenshot + Labels
            if (panelPrefs.showPowerMenu) nonAppHeightDp += 42f
            if (panelPrefs.showSysInfo) nonAppHeightDp += 24f
            if (panelPrefs.showVolumeKeys) nonAppHeightDp += 84f // Two buttons + labels
            if (panelPrefs.showBrightnessKeys) nonAppHeightDp += 84f
        }
        
        // Maximum allowed height for RV to keep panel within screen (with 24dp safety margin)
        val maxAllowedRvHeightDp = (screenHeightDp - nonAppHeightDp - 24).coerceAtLeast(100f)
        val targetRvHeightDp = panelPrefs.panelMaxHeight.toFloat().coerceAtMost(maxAllowedRvHeightDp)

        // Apply dynamic height directly to the RecyclerView
        val rvLp = binding.rvPanelApps.layoutParams
        val maxRvHeightPx = context.dpToPx((targetRvHeightDp * scale).toInt())
        
        // Use wrap_content for few apps, but cap at maxRvHeightPx
        // We can use a custom view or just calculate items.
        val itemsCount = adapter.itemCount
        val isRich = panelPrefs.uiTheme == PanelPreferences.THEME_RICH
        val baseItemHeightDp = if (isRich) 68 else 62 // Rich theme items are slightly taller
        val itemHeightPx = context.dpToPx((baseItemHeightDp * scale).toInt())
        
        val rows = Math.ceil(itemsCount.toDouble() / currentCols).toInt()
        val estimatedContentHeightPx = rows * itemHeightPx
        
        if (estimatedContentHeightPx > 0 && estimatedContentHeightPx < maxRvHeightPx) {
            rvLp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        } else if (itemsCount == 0) {
            rvLp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        } else {
            rvLp.height = maxRvHeightPx
        }
        binding.rvPanelApps.layoutParams = rvLp

        val containerLp = binding.panelContainer.layoutParams as? android.widget.RelativeLayout.LayoutParams    
        if (containerLp != null) {
            if (isRight) {
                containerLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END)
                containerLp.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_START)
                containerLp.marginEnd = context.dpToPx(12)
                containerLp.marginStart = 0
            } else {
                containerLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START)
                containerLp.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_END)
                containerLp.marginStart = context.dpToPx(12)
                containerLp.marginEnd = 0
            }
            binding.panelContainer.layoutParams = containerLp
        }
    }

    fun scrollToTop() {
        binding.rvPanelApps.scrollToPosition(0)
    }

    fun scrollToBottom() {
        val count = adapter.itemCount
        if (count > 0) binding.rvPanelApps.scrollToPosition(count - 1)
    }

    fun scrollToApp(packageName: String) {
        val apps = adapter.currentList
        val index = apps.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            binding.rvPanelApps.smoothScrollToPosition(index)
            adapter.highlightItem(packageName)
        }
    }

    fun animatePickerToggle(isOpen: Boolean) {
        isPickerOpenInternal = isOpen
        val targetRotation = if (isOpen) 90f else (if (panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT) 180f else 0f)
        springRotation.animateToFinalPosition(targetRotation)
    }

    fun setColumns(cols: Int) {
        currentCols = cols
        adapter.setColumns(cols)
        (binding.rvPanelApps.layoutManager as? GridLayoutManager)?.spanCount = currentCols
        updateSideLayout()
    }

    fun setEditButtonVisible(visible: Boolean) {
        adapter.setShowAddButton(visible)
        updateSideLayout()
    }

    fun setApps(apps: List<AppInfo>, onComplete: (() -> Unit)? = null) {
        adapter.submitList(apps) {
            updateSideLayout()
            onComplete?.invoke()
        }
    }

    fun updateStyles() {
        if (!isPickerOpenInternal) {
            currentCols = panelPrefs.panelColumns
            (binding.rvPanelApps.layoutManager as? GridLayoutManager)?.spanCount = currentCols
        }
        applyTheme()
        updateSideLayout()
    }

    fun refreshIcons() {
        adapter.refreshIcons()
    }

    fun applyTheme() {
        binding.toolsContainer.visibility = if (panelPrefs.showTools) View.VISIBLE else View.GONE
        binding.layoutPowerTools.visibility = if (panelPrefs.showPowerMenu) View.VISIBLE else View.GONE

        if (panelPrefs.hideBackground) {
            binding.panelCard.background = null
        } else {
            val theme = panelPrefs.uiTheme
            val bgColor = when (theme) {
                PanelPreferences.THEME_ORIGIN -> Color.parseColor("#1F1F1F")
                PanelPreferences.THEME_HYPEROS -> Color.parseColor("#E6252525")
                else -> try { Color.parseColor(panelPrefs.panelBackgroundColor) } catch (e: Exception) { Color.parseColor("#E61A1C1E") }
            }
            
            val radius = context.dpToPx(if (theme == PanelPreferences.THEME_HYPEROS) 16 else panelPrefs.panelCornerRadius).toFloat()
            
            val shape = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = radius
                
                if (theme == PanelPreferences.THEME_HYPEROS) {
                    setStroke(context.dpToPx(1), Color.parseColor("#4DFFFFFF"))
                } else if (theme == PanelPreferences.THEME_RICH) {
                    // Glowing Rich UI: Colored border + inner glow look
                    val accent = try { Color.parseColor(panelPrefs.accentColor) } catch (e: Exception) { Color.parseColor("#4A9EFF") }
                    setStroke(context.dpToPx(2), accent)
                } else if (theme == PanelPreferences.THEME_REALME) {
                    // Realme UI: Subtle Gradient + Light Border
                    val color1 = Color.parseColor("#333333")
                    val color2 = Color.parseColor("#1A1A1A")
                    colors = intArrayOf(color1, color2)
                    orientation = GradientDrawable.Orientation.TOP_BOTTOM
                    setStroke(context.dpToPx(1), Color.parseColor("#33FFFFFF"))
                }
            }
            binding.panelCard.background = shape
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                binding.panelCard.clipToOutline = true
            }
        }
        
        binding.layoutSysInfo.visibility = if (panelPrefs.showSysInfo) View.VISIBLE else View.GONE
        binding.toolsContainer.visibility = if (panelPrefs.showTools) View.VISIBLE else View.GONE
        binding.layoutPowerTools.visibility = if (panelPrefs.showPowerMenu) View.VISIBLE else View.GONE
        binding.layoutVolumeTools.visibility = if (panelPrefs.showVolumeKeys) View.VISIBLE else View.GONE
        binding.layoutBrightnessTools.visibility = if (panelPrefs.showBrightnessKeys) View.VISIBLE else View.GONE
        
        if (panelPrefs.showSysInfo) {
            updateSystemInfo()
            updateHandler.removeCallbacks(updateRunnable)
            updateHandler.post(updateRunnable)
        } else {
            updateHandler.removeCallbacks(updateRunnable)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        updateHandler.removeCallbacks(updateRunnable)
    }
}
