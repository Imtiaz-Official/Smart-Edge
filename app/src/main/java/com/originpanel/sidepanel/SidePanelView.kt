package com.originpanel.sidepanel

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
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
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@SidePanelView.adapter
            itemAnimator = null  // Disable default animation (spring handles it)
        }

        // Close button
        binding.btnClose.setOnClickListener {
            SpringAnimator.scalePulse(it)
            onClose?.invoke()
        }

        // AI button (placeholder — add your AI integration here)
        binding.btnAI.setOnClickListener {
            SpringAnimator.scalePulse(it)
            Toast.makeText(context, "AI Assistant coming soon!", Toast.LENGTH_SHORT).show()
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
        binding.tvEmptyPanel.visibility = View.GONE
    }
}
