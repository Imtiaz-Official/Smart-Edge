package com.originpanel.sidepanel

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
import android.widget.Toast
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.originpanel.sidepanel.databinding.SidePanelLayoutBinding

/**
 * High-performance Side Panel.
 * Fixed Arrow Logic: Uses deterministic constant-based positioning to prevent layout-race glitches.
 */
class SidePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onClose: (() -> Unit)? = null
    var onAppsChanged: (() -> Unit)? = null
    var onAddClick: ((Boolean) -> Unit)? = null // Accept Boolean for Edit Mode

    private val binding: SidePanelLayoutBinding = SidePanelLayoutBinding.inflate(LayoutInflater.from(context), this, true)
    private val adapter: PanelAppsAdapter
    private val panelPrefs = PanelPreferences(context)

    // Physics Springs - Initialized directly to binding view
    private val springX: SpringAnimation = SpringAnimation(binding.btnClose, SpringAnimation.TRANSLATION_X)
    private val springRotation: SpringAnimation = SpringAnimation(binding.btnClose, SpringAnimation.ROTATION)

    private val width1ColDp = 72f
    private val width2ColDp = 150f
    private val buttonWidthDp = 24f
    private val horizontalMarginDp = 8f

    init {
        // Configure Springs
        springX.spring = SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            stiffness = SpringForce.STIFFNESS_LOW
        }
        springRotation.spring = SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            stiffness = SpringForce.STIFFNESS_MEDIUM
        }

        setOnClickListener { onClose?.invoke() }
        binding.panelCard.setOnClickListener { }

        applyTheme()
        binding.toolsContainer.visibility = if (panelPrefs.showTools) View.VISIBLE else View.GONE
        binding.panelCard.alpha = panelPrefs.panelOpacity / 100f

        adapter = PanelAppsAdapter(
            context,
            onRemove = { removedApp ->
                panelPrefs.removeApp(removedApp.packageName)
                onAppsChanged?.invoke()
            },
            onAddClick = { isEdit -> onAddClick?.invoke(isEdit) },
            onAppLaunched = { onClose?.invoke() }
        )

        val cols = if (panelPrefs.isPremium) panelPrefs.panelColumns else 1
        binding.rvPanelApps.layoutManager = GridLayoutManager(context, cols)
        binding.rvPanelApps.adapter = adapter
        
        // Initial width setup
        val lp = binding.panelCard.layoutParams
        lp.width = dpToPx(if (cols == 2) width2ColDp.toInt() else width1ColDp.toInt())
        binding.panelCard.layoutParams = lp

        // Dynamic alignment based on side
        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT
        
        // Initial arrow rotation based on side
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

        binding.btnClose.setOnClickListener {
            if (panelPrefs.hapticEnabled) {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
            SpringAnimator.scalePulse(it)
            onAddClick?.invoke(false) // Arrow click opens All Apps (Normal Mode)
        }

        binding.btnScreenshot.setOnClickListener {
            if (panelPrefs.hapticEnabled) {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            }
            onClose?.invoke() 
            val intent = Intent(context, ScreenshotActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
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
            setColor(Color.parseColor(panelPrefs.panelBackgroundColor))
            cornerRadius = panelPrefs.panelCornerRadius * density
        }
        
        val accentColor = if (panelPrefs.useCustomAccent) {
            try { Color.parseColor(panelPrefs.accentColor) } catch (e: Exception) { Color.parseColor("#4A9EFF") }
        } else {
            when (theme) {
                PanelPreferences.THEME_HYPEROS -> Color.parseColor("#33FFFFFF")
                PanelPreferences.THEME_REALME -> Color.parseColor("#26FFFFFF")
                PanelPreferences.THEME_RICH -> Color.parseColor("#4DFFFFFF")
                else -> Color.parseColor("#26FFFFFF")
            }
        }

        when (theme) {
            PanelPreferences.THEME_HYPEROS -> {
                drawable.setStroke((1 * density).toInt(), accentColor)
                drawable.cornerRadius = 12 * density 
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

    fun setApps(apps: List<AppInfo>, onComplete: (() -> Unit)? = null) {
        adapter.submitList(apps) {
            onComplete?.invoke()
        }
        binding.rvPanelApps.visibility = View.VISIBLE
    }

    fun scrollToTop() {
        val scroller = object : androidx.recyclerview.widget.LinearSmoothScroller(context) {
            override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
                // Higher value = Slower scroll. Default is 25f. 
                // 100f makes it significantly "calmer" and more deliberate.
                return 100f / displayMetrics.densityDpi
            }
        }
        scroller.targetPosition = 0
        binding.rvPanelApps.layoutManager?.startSmoothScroll(scroller)
    }

    fun scrollToPosition(pos: Int) {
        binding.rvPanelApps.scrollToPosition(pos)
    }

    fun scrollToBottom() {
        val scroller = object : androidx.recyclerview.widget.LinearSmoothScroller(context) {
            override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
                return 60f / displayMetrics.densityDpi // Slightly faster for adding new apps
            }
        }
        val count = adapter.itemCount
        if (count > 0) {
            scroller.targetPosition = count - 1
            binding.rvPanelApps.layoutManager?.startSmoothScroll(scroller)
        }
    }

    fun scrollToApp(packageName: String) {
        // Post to ensure the RecyclerView has updated its child count after submitList
        binding.rvPanelApps.post {
            val apps = adapter.currentList
            val index = apps.indexOfFirst { it.packageName == packageName }
            if (index != -1) {
                val scroller = object : androidx.recyclerview.widget.LinearSmoothScroller(context) {
                    override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
                        return 80f / displayMetrics.densityDpi
                    }
                }
                scroller.targetPosition = index
                binding.rvPanelApps.layoutManager?.startSmoothScroll(scroller)
                
                // Pulse the item for visibility
                adapter.highlightItem(packageName)
            }
        }
    }

    fun setColumns(cols: Int) {
        val density = context.resources.displayMetrics.density
        val layoutManager = binding.rvPanelApps.layoutManager as? GridLayoutManager
        layoutManager?.spanCount = cols
        
        val lp = binding.panelCard.layoutParams
        lp.width = dpToPx(if (cols == 2) width2ColDp.toInt() else width1ColDp.toInt())
        binding.panelCard.layoutParams = lp
        binding.panelCard.requestLayout()
    }

    fun animatePickerToggle(isPickerOpen: Boolean) {
        val density = context.resources.displayMetrics.density
        val isRight = panelPrefs.panelSide == PanelPreferences.SIDE_RIGHT

        val currentPanelWidthDp = if (isPickerOpen) width1ColDp else {
            if (panelPrefs.isPremium && panelPrefs.panelColumns == 2) width2ColDp else width1ColDp
        }
        val middleXDp = (currentPanelWidthDp / 2f) - (buttonWidthDp / 2f)
        val rightXDp = currentPanelWidthDp - horizontalMarginDp - buttonWidthDp
        val targetTranslationXDp = if (isPickerOpen) (rightXDp - middleXDp) else 0f
        
        // Mirror rotation based on side
        val targetRotation = if (isRight) {
            if (isPickerOpen) 0f else 180f
        } else {
            if (isPickerOpen) 180f else 0f
        }

        springX.cancel()
        springRotation.cancel()
        springX.animateToFinalPosition(targetTranslationXDp * density)
        springRotation.animateToFinalPosition(targetRotation)
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
