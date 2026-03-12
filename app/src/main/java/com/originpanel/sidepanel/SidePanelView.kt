package com.originpanel.sidepanel

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
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
 * - Empty state text
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
    }

    private fun applyTheme() {
        val theme = panelPrefs.uiTheme
        val density = context.resources.displayMetrics.density
        binding.panelHandle.visibility = if (theme == PanelPreferences.THEME_RICH) View.VISIBLE else View.GONE
        
        // Dynamically create background instead of static XML to allow customization
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        
        // Use custom background color if set, else fallback to theme default
        val bgColor = Color.parseColor(panelPrefs.panelBackgroundColor)
        drawable.setColor(bgColor)
        
        // Corner Radius
        drawable.cornerRadius = panelPrefs.panelCornerRadius * density
        
        // Stroke/Border based on theme
        when (theme) {
            PanelPreferences.THEME_HYPEROS -> {
                drawable.setStroke((1 * density).toInt(), Color.parseColor("#33FFFFFF"))
            }
            PanelPreferences.THEME_REALME -> {
                drawable.setStroke((2 * density).toInt(), Color.parseColor("#FF4A9EFF"))
                drawable.cornerRadius = 40 * density // Override for Realme
            }
            PanelPreferences.THEME_RICH -> {
                drawable.setStroke((2 * density).toInt(), Color.parseColor("#FF4A9EFF"))
            }
            else -> { // Origin
                drawable.setStroke((1 * density).toInt(), Color.parseColor("#26FFFFFF"))
            }
        }
        
        binding.panelCard.background = drawable
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.panelCard.clipToOutline = true
        }
    }

    /**
     * Updates the displayed app list.
     * Uses DiffUtil internally via ListAdapter — only changed items are redrawn.
     */
    fun setApps(apps: List<AppInfo>) {
        adapter.submitList(apps)
        binding.rvPanelApps.visibility = View.VISIBLE
    }

    /** Reset scroll to the top when the panel is re-opened. */
    fun scrollToTop() {
        binding.rvPanelApps.scrollToPosition(0)
    }

    /**
     * Dynamically changes the column count and panel width.
     * Used to squeeze the panel to 1-col when picker is open.
     */
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

    /**
     * Animates the bottom arrow (btnClose) to move from middle to the right side.
     * @param isOpen Whether the picker is currently open.
     */
    fun animatePickerToggle(isOpen: Boolean) {
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
                .start()
        }
    }
}
