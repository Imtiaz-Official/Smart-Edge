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
            if (cols == 2) {
                binding.panelCard.layoutParams.width = (136 * context.resources.displayMetrics.density).toInt()
            } else {
                binding.panelCard.layoutParams.width = (72 * context.resources.displayMetrics.density).toInt()
            }

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
            // Fallback to default blue if color string is invalid
            binding.btnAI.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4A9EFF"))
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
        // Center of current width.
        val currentWidth = binding.panelCard.width / density
        val buttonWidth = 24f
        
        // Start at middle: (currentWidth / 2) - (buttonWidth / 2)
        // We want to move it to (currentWidth - 8dp - buttonWidth)
        val middleX = (currentWidth / 2f) - (buttonWidth / 2f)
        val rightX = currentWidth - 8f - buttonWidth
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
