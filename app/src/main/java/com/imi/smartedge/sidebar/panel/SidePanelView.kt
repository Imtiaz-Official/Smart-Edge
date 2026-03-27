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

    private val springRotation: SpringAnimation = SpringAnimation(binding.btnClose, SpringAnimation.ROTATION)   

    private val width1ColDp = 72f
    private val width2ColDp = 150f

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
            // RAM Info
            val mi = android.app.ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.getMemoryInfo(mi)
            val availableMegs = mi.availMem / 1048576L
            val totalMegs = mi.totalMem / 1048576L
            val usedMegs = totalMegs - availableMegs
            binding.tvRamUsage.text = "RAM: ${usedMegs}MB"

            // Battery Info (Simple approach)
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

        val cols = if (panelPrefs.isUnlocked) panelPrefs.panelColumns else 1
        binding.rvPanelApps.layoutManager = GridLayoutManager(context, cols)
        binding.rvPanelApps.adapter = adapter

        binding.rvPanelApps.setHasFixedSize(true)
        binding.rvPanelApps.isNestedScrollingEnabled = false
        binding.rvPanelApps.setItemViewCacheSize(30)
        (binding.rvPanelApps.itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)?.supportsChangeAnimations = false
        binding.rvPanelApps.recycledViewPool.setMaxRecycledViews(0, 30)

        val lp = binding.panelCard.layoutParams
        lp.width = dpToPx(if (cols == 2) width2ColDp.toInt() else width1ColDp.toInt())
        binding.panelCard.layoutParams = lp

        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        binding.btnClose.rotation = if (isRight) 180f else 0f

        val containerLp = binding.panelContainer.layoutParams as? android.widget.RelativeLayout.LayoutParams    
        if (containerLp != null) {
            if (isRight) {
                containerLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END)
                containerLp.marginEnd = dpToPx(12)
                containerLp.marginStart = 0
            } else {
                containerLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START)
                containerLp.marginStart = dpToPx(12)
                containerLp.marginEnd = 0
            }
            binding.panelContainer.layoutParams = containerLp
        }

        binding.btnClose.setOnClickListener {
            if (panelPrefs.hapticEnabled) {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
            SpringAnimator.scalePulse(it)
            onAddClick?.invoke(false)
        }

        binding.btnScreenshot.setOnClickListener {
            if (panelPrefs.hapticEnabled) {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            }
            onScreenshot?.invoke()
        }

        binding.btnReboot.setOnClickListener {
            if (panelPrefs.hapticEnabled) {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }

            // Always try Accessibility first as it's more reliable for System UI actions
            val intent = Intent(context, PanelAccessibilityService::class.java).apply {
                action = PanelAccessibilityService.ACTION_SHOW_POWER_MENU
            }
            context.startService(intent)

            // Also try Shizuku as a simultaneous fallback
            /*
            if (ShizukuHelper.hasShizukuPermission()) {
                ShizukuHelper.triggerPowerMenu()
            }
            */

            // Short delay before closing
            binding.root.postDelayed({
                onClose?.invoke()
            }, 300)
        }

        binding.btnReboot.setOnLongClickListener {
            val themedContext = androidx.appcompat.view.ContextThemeWrapper(context, R.style.Theme_SidePanel)   
            val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(themedContext)
                .setTitle("Power Options")
                .setItems(arrayOf("Reboot to Recovery", "Reboot to Bootloader")) { _, which ->
                    /*
                    if (!ShizukuHelper.hasShizukuPermission()) {
                        android.widget.Toast.makeText(context, "Shizuku Permission Required for Advanced Reboot", android.widget.Toast.LENGTH_SHORT).show()
                        return@setItems
                    }
                    */
                    val type = when(which) {
                        0 -> "recovery"
                        1 -> "bootloader"
                        else -> ""
                    }
                    android.widget.Toast.makeText(context, "Advanced reboot requires Shizuku (Currently Disabled)", android.widget.Toast.LENGTH_SHORT).show()
                    // ShizukuHelper.reboot(type)
                }
                .create()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dialog.window?.setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)        
            } else {
                dialog.window?.setType(android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
            }

            dialog.show()
            true
        }

        updateStyles()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (panelPrefs.showSysInfo) {
            updateHandler.post(updateRunnable)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        updateHandler.removeCallbacks(updateRunnable)
    }

    fun updateSideLayout() {
        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        binding.btnClose.rotation = if (isRight) 180f else 0f

        val containerLp = binding.panelContainer.layoutParams as? android.widget.RelativeLayout.LayoutParams    
        if (containerLp != null) {
            containerLp.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_END)
            containerLp.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_START)

            if (isRight) {
                containerLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END)
                containerLp.marginEnd = dpToPx(12)
                containerLp.marginStart = 0
            } else {
                containerLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START)
                containerLp.marginStart = dpToPx(12)
                containerLp.marginEnd = 0
            }
            binding.panelContainer.layoutParams = containerLp
        }
    }

    fun updateStyles() {
        applyTheme()
        updateSideLayout()

        binding.toolsContainer.visibility = if (panelPrefs.showTools) View.VISIBLE else View.GONE
        binding.layoutPowerTools.visibility = if (panelPrefs.showPowerMenu && panelPrefs.showTools) View.VISIBLE else View.GONE
        binding.layoutSysInfo.visibility = if (panelPrefs.showSysInfo && panelPrefs.showTools) View.VISIBLE else View.GONE

        if (panelPrefs.showSysInfo && panelPrefs.showTools) {
            updateHandler.removeCallbacks(updateRunnable)
            updateHandler.post(updateRunnable)
        } else {
            updateHandler.removeCallbacks(updateRunnable)
        }

        binding.panelCard.alpha = panelPrefs.panelOpacity / 100f
        val cols = if (panelPrefs.isUnlocked) panelPrefs.panelColumns else 1
        val lp = binding.panelCard.layoutParams
        lp.width = dpToPx(if (cols == 2) width2ColDp.toInt() else width1ColDp.toInt())
        binding.panelCard.layoutParams = lp
        (binding.rvPanelApps.layoutManager as? GridLayoutManager)?.spanCount = cols
        binding.panelCard.requestLayout()
    }

    private fun applyTheme() {
        val theme = panelPrefs.uiTheme
        val density = context.resources.displayMetrics.density
        binding.panelHandle.visibility = if (theme == PanelPreferences.THEME_RICH) View.VISIBLE else View.GONE  

        if (panelPrefs.hideBackground) {
            binding.panelCard.setBackgroundColor(Color.TRANSPARENT)
            return
        }

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = panelPrefs.panelCornerRadius * density
        }

        val themeBgColor = when (theme) {
            PanelPreferences.THEME_ORIGIN -> Color.parseColor("#1F1F1F")
            PanelPreferences.THEME_M3 -> Color.parseColor("#E61C1B1F") // Modern M3 Dark Surface
            else -> Color.parseColor(panelPrefs.panelBackgroundColor)
        }
        drawable.setColor(themeBgColor)

        val accentColor = if (panelPrefs.useCustomAccent) {
            try { Color.parseColor(panelPrefs.accentColor) } catch (e: Exception) { Color.parseColor("#4A9EFF") }
        } else {
            when (theme) {
                PanelPreferences.THEME_ORIGIN -> Color.parseColor("#33FFFFFF")
                PanelPreferences.THEME_HYPEROS -> Color.parseColor("#33FFFFFF")
                PanelPreferences.THEME_REALME -> Color.parseColor("#26FFFFFF")
                PanelPreferences.THEME_RICH -> Color.parseColor("#4DFFFFFF")
                PanelPreferences.THEME_M3 -> Color.parseColor("#D0BCFF") // M3 Primary
                else -> Color.parseColor("#26FFFFFF")
            }
        }

        when (theme) {
            PanelPreferences.THEME_ORIGIN -> {
                drawable.setStroke(0, Color.TRANSPARENT)
                binding.btnClose.backgroundTintList = ColorStateList.valueOf(accentColor)
            }
            PanelPreferences.THEME_M3 -> {
                drawable.setStroke((1 * density).toInt(), Color.parseColor("#49454F")) // M3 Outline
                drawable.cornerRadius = 28 * density // Standard M3 Card Corner
                binding.btnClose.backgroundTintList = ColorStateList.valueOf(accentColor)
                binding.btnClose.imageTintList = ColorStateList.valueOf(Color.parseColor("#381E72")) // M3 OnPrimary
            }
            PanelPreferences.THEME_HYPEROS -> {
                drawable.setStroke((1.5 * density).toInt(), Color.parseColor("#4DFFFFFF"))
                drawable.cornerRadius = 16 * density
                drawable.setColor(Color.parseColor("#E6252525"))
            }
            PanelPreferences.THEME_REALME -> {
                drawable.setStroke((2 * density).toInt(), accentColor)
                drawable.cornerRadius = 40 * density
            }
            PanelPreferences.THEME_RICH -> {
                drawable.setStroke((2 * density).toInt(), accentColor)
                binding.panelHandle.backgroundTintList = ColorStateList.valueOf(accentColor)
            }
            else -> {
                drawable.setStroke((1 * density).toInt(), accentColor)
                drawable.cornerRadius = 48 * density
            }
        }
        binding.panelCard.background = drawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) binding.panelCard.clipToOutline = true
    }

    fun setEditButtonVisible(visible: Boolean) {
        adapter.setShowAddButton(visible)
    }

    fun refreshIcons() {
        adapter.refreshIcons()
    }

    fun setApps(apps: List<AppInfo>, onComplete: (() -> Unit)? = null) {
        adapter.submitList(apps) {
            onComplete?.invoke()
        }
        binding.rvPanelApps.visibility = View.VISIBLE
    }

    fun scrollToTop() {
        binding.rvPanelApps.scrollToPosition(0)
    }

    fun scrollToPosition(pos: Int) {
        binding.rvPanelApps.scrollToPosition(pos)
    }

    fun scrollToBottom() {
        val count = adapter.itemCount
        if (count > 0) binding.rvPanelApps.scrollToPosition(count - 1)
    }

    fun scrollToApp(packageName: String) {
        binding.rvPanelApps.post {
            val apps = adapter.currentList
            val index = apps.indexOfFirst { it.packageName == packageName }
            if (index != -1) {
                binding.rvPanelApps.smoothScrollToPosition(index)
                adapter.highlightItem(packageName)
            }
        }
    }

    fun setColumns(cols: Int) {
        val layoutManager = binding.rvPanelApps.layoutManager as? GridLayoutManager ?: return
        layoutManager.spanCount = cols
        val lp = binding.panelCard.layoutParams
        lp.width = dpToPx(if (cols == 2) width2ColDp.toInt() else width1ColDp.toInt())
        binding.panelCard.layoutParams = lp
        binding.panelCard.requestLayout()
    }

    fun animatePickerToggle(isPickerOpen: Boolean) {
        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        val targetRotation = if (isRight) {
            if (isPickerOpen) 0f else 180f
        } else {
            if (isPickerOpen) 180f else 0f
        }
        springRotation.cancel()
        springRotation.animateToFinalPosition(targetRotation)
    }

    fun getPanelCardRect(outRect: android.graphics.Rect) {
        binding.panelCard.getGlobalVisibleRect(outRect)
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
