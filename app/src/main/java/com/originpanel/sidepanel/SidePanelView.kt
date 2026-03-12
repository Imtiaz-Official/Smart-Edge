package com.originpanel.sidepanel

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.originpanel.sidepanel.databinding.SidePanelLayoutBinding

/**
 * The main side panel view — a frosted glass pill containing:
 * - Scrollable list of pinned apps (RecyclerView)
 * - Empty state text
 * - AI / BlueLM button (placeholder — extend as needed)
 * - Close / collapse button
 *
 * This view is added/removed from WindowManager by [FloatingPanelService].
 */
class SidePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

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

        // Apply UI Theme (Premium)
        applyTheme()

        // Set opacity
        val alphaVal = panelPrefs.panelOpacity / 100f
        binding.panelCard.alpha = alphaVal
        binding.btnAI.alpha = alphaVal

        // Setup RecyclerView
        adapter = PanelAppsAdapter(
            context,
            onRemove = { removedApp ->
                panelPrefs.removeApp(removedApp.packageName)
                onAppsChanged?.invoke()
                Toast.makeText(context, "${removedApp.appName} removed from panel",
                    Toast.LENGTH_SHORT).show()
            },
            onAddClick = { onAddClick?.invoke() }
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
            itemAnimator = null  // Disable default animation (spring handles it)
            setHasFixedSize(true) // Avoids re-measuring parent during spring animations
        }

        // Toggle Picker (Repurposed from Close button)
        binding.btnClose.setOnClickListener {
            if (panelPrefs.hapticEnabled) {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
            SpringAnimator.scalePulse(it)
            onAddClick?.invoke()
        }

        // AI button (placeholder — add your AI integration here)
        binding.btnAI.setOnClickListener {
            if (panelPrefs.hapticEnabled) {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            }
            SpringAnimator.scalePulse(it)
            Toast.makeText(context, "AI Assistant coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Apply accent color (Premium)
        try {
            val accentColor = Color.parseColor(panelPrefs.accentColor)
            binding.btnAI.backgroundTintList = ColorStateList.valueOf(accentColor)
        } catch (e: Exception) {
            binding.btnAI.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4A9EFF"))
        }
    }

    private fun applyTheme() {
        val theme = panelPrefs.uiTheme
        binding.panelHandle.visibility = if (theme == PanelPreferences.THEME_RICH) View.VISIBLE else View.GONE
        
        when (theme) {
            PanelPreferences.THEME_HYPEROS -> {
                binding.panelCard.setBackgroundResource(R.drawable.bg_panel_hyperos)
                binding.btnAI.setBackgroundResource(R.drawable.bg_ai_button) // Or specific hyperos AI bg
            }
            PanelPreferences.THEME_REALME -> {
                binding.panelCard.setBackgroundResource(R.drawable.bg_panel_realme)
            }
            PanelPreferences.THEME_RICH -> {
                binding.panelCard.setBackgroundResource(R.drawable.bg_panel_rich)
            }
            else -> { // Origin (Default)
                binding.panelCard.setBackgroundResource(R.drawable.bg_panel)
            }
        }
    }

    /**
     * Updates the displayed app list.
     * Uses DiffUtil internally via ListAdapter — only changed items are redrawn.
     */
    fun setApps(apps: List<AppInfo>) {
        adapter.submitList(apps)
        // RecyclerView always visible now because it contains the '+' button
        binding.rvPanelApps.visibility = View.VISIBLE
    }

    /** Reset scroll to the top when the panel is re-opened. */
    fun scrollToTop() {
        binding.rvPanelApps.scrollToPosition(0)
    }

    /**
     * Animates the bottom arrow (btnClose) to move from middle to the right side.
     * @param isOpen Whether the picker is currently open.
     */
    fun animatePickerToggle(isOpen: Boolean) {
        val density = context.resources.displayMetrics.density
        // Use current layout params width if width is 0 (view not measured)
        val viewWidth = if (binding.panelCard.width > 0) binding.panelCard.width.toFloat() 
                        else binding.panelCard.layoutParams.width.toFloat()
        
        val viewWidthDp = viewWidth / density
        val buttonWidthDp = 24f
        
        // Start at middle: (viewWidthDp / 2) - (buttonWidthDp / 2)
        // We want to move it to (viewWidthDp - 8dp - buttonWidthDp)
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
