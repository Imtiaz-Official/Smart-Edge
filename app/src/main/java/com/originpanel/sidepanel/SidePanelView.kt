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
import android.widget.FrameLayout
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.originpanel.sidepanel.databinding.SidePanelLayoutBinding

/**
 * The main side panel view — a frosted glass pill containing:
 * - Scrollable list of pinned apps (RecyclerView)
 * - Tools section (Screenshot, etc)
 * - Close / collapse button
 *
 * This view is added/removed from WindowManager by [FloatingPanelService].
 */
class SidePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    /** Called when the user taps the close button or outside the panel. */
    var onClose: (() -> Unit)? = null

    /** Called when an app is removed (so FloatingPanelService can refresh). */
    var onAppsChanged: (() -> Unit)? = null

    /** Called when the plus button is tapped to open the picker panel. */
    var onAddClick: (() -> Unit)? = null

    private val binding: SidePanelLayoutBinding
    private val adapter: PanelAppsAdapter
    private val panelPrefs = PanelPreferences(context)
    private var isAnimating = false

    init {
        // Inflate using ViewBinding
        binding = SidePanelLayoutBinding.inflate(LayoutInflater.from(context), this, true)

        // Tapping anywhere outside the panelCard should close it
        setOnClickListener { 
            onClose?.invoke()
        }
        // Prevent clicks on the panel itself from bubbling up to the closer
        binding.panelCard.setOnClickListener { /* Consume */ }

        // Apply Custom Styles (Premium + Themes)
        applyTheme()

        // Tools visibility
        binding.toolsContainer.visibility = if (panelPrefs.showTools) View.VISIBLE else View.GONE

        // Set opacity
        val alphaVal = panelPrefs.panelOpacity / 100f
        binding.panelCard.alpha = alphaVal

        // Setup RecyclerView
        adapter = PanelAppsAdapter(
            context,
            onRemove = { removedApp ->
                panelPrefs.removeApp(removedApp.packageName)
                onAppsChanged?.invoke()
                Toast.makeText(context, "${removedApp.appName} removed from panel",
                    Toast.LENGTH_SHORT).show()
            },
            onAddClick = { onAddClick?.invoke() },
            onAppLaunched = { onClose?.invoke() }
        )

        binding.rvPanelApps.apply {
            val cols = if (panelPrefs.isPremium) panelPrefs.panelColumns else 1
            layoutManager = GridLayoutManager(context, cols)
            
            // Adjust panel width if 2 columns
            val params = binding.panelCard.layoutParams
            if (cols == 2) {
                params.width = (150 * context.resources.displayMetrics.density).toInt()
            } else {
                params.width = (72 * context.resources.displayMetrics.density).toInt()
            }
            binding.panelCard.layoutParams = params
            binding.panelCard.requestLayout()

            this.adapter = this@SidePanelView.adapter
            itemAnimator = null  
            setHasFixedSize(true) 
        }

        // Toggle Picker (Repurposed from Close button)
        binding.btnClose.setOnClickListener {
            if (panelPrefs.hapticEnabled) {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
            SpringAnimator.scalePulse(it)
            onAddClick?.invoke()
        }

        // Screenshot tool
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

        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        
        val bgColor = Color.parseColor(panelPrefs.panelBackgroundColor)
        drawable.setColor(bgColor)
        
        drawable.cornerRadius = panelPrefs.panelCornerRadius * density
        
        val accentColorHex = panelPrefs.accentColor
        Log.d("SidePanelView", "Applying theme: $theme, AccentColor: $accentColorHex")
        val accentColor = try {
            Color.parseColor(accentColorHex)
        } catch (e: Exception) {
            Color.parseColor("#4A9EFF") // Fallback
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
            else -> { // Origin (Default)
                drawable.setStroke((1 * density).toInt(), accentColor)
                drawable.cornerRadius = 48 * density 
            }
        }
        
        binding.panelCard.background = drawable
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.panelCard.clipToOutline = true
        }
    }

    fun setApps(apps: List<AppInfo>) {
        adapter.submitList(apps)
        binding.rvPanelApps.visibility = View.VISIBLE
    }

    fun scrollToTop() {
        binding.rvPanelApps.scrollToPosition(0)
    }

    fun setColumns(cols: Int) {
        val density = context.resources.displayMetrics.density
        val layoutManager = binding.rvPanelApps.layoutManager as? GridLayoutManager
        layoutManager?.spanCount = cols
        
        val params = binding.panelCard.layoutParams
        if (cols == 2) {
            params.width = (150 * density).toInt()
        } else {
            params.width = (72 * density).toInt()
        }
        binding.panelCard.layoutParams = params
        binding.panelCard.requestLayout()
    }

    fun animatePickerToggle(isOpen: Boolean) {
        if (isAnimating) return
        isAnimating = true

        binding.btnClose.post {
            val density = context.resources.displayMetrics.density
            val viewWidth = binding.panelCard.width.toFloat()
            val viewWidthDp = viewWidth / density
            val buttonWidthDp = 24f
            val middleX = (viewWidthDp / 2f) - (buttonWidthDp / 2f)
            val rightX = viewWidthDp - 8f - buttonWidthDp
            val translation = if (isOpen) (rightX - middleX) else 0f
            val targetRotation = if (isOpen) 0f else 180f

            binding.btnClose.animate()
                .translationX(translation * density)
                .rotation(targetRotation)
                .setDuration(400)
                .setInterpolator(android.view.animation.AnticipateOvershootInterpolator(1.0f))
                .withEndAction { isAnimating = false }
                .start()
        }
    }
}
