package com.originpanel.sidepanel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class PanelAppsAdapter(
    private val context: Context,
    private val onRemove: (AppInfo) -> Unit,
    private val onAddClick: (Boolean) -> Unit, // Accept Boolean for Edit Mode
    private val onAppLaunched: () -> Unit
) : ListAdapter<AppInfo, RecyclerView.ViewHolder>(AppDiffCallback()) {

    private val panelPrefs = PanelPreferences(context)

    companion object {
        private const val VIEW_TYPE_APP = 0
        private const val VIEW_TYPE_ADD = 1
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvAppName)
    }

    inner class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAdd: ImageView = itemView.findViewById(R.id.ivAddIcon)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < currentList.size) VIEW_TYPE_APP else VIEW_TYPE_ADD
    }

    override fun getItemCount(): Int {
        return currentList.size + 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_APP) {
            val layoutId = if (panelPrefs.uiTheme == PanelPreferences.THEME_RICH)
                R.layout.item_panel_app_rich else R.layout.item_panel_app
            
            val view = LayoutInflater.from(parent.context)
                .inflate(layoutId, parent, false)
            AppViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_panel_add, parent, false)
            AddViewHolder(view)
        }
    }

    private var highlightPackage: String? = null

    fun highlightItem(packageName: String) {
        highlightPackage = packageName
        val index = currentList.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AppViewHolder) {
            val app = getItem(position)
            holder.ivIcon.setImageDrawable(app.icon)
            holder.tvName.text = app.appName
            
            // Apply icon shape
            IconShapeHelper.applyShape(holder.ivIcon, panelPrefs.iconShape)

            // Visual feedback for newly added app
            if (app.packageName == highlightPackage) {
                SpringAnimator.scalePulse(holder.itemView)
                highlightPackage = null // Only pulse once
            }

            holder.itemView.setOnClickListener {
                if (panelPrefs.hapticEnabled) {
                    holder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                }
                SpringAnimator.scalePulse(holder.itemView)
                onAppLaunched()

                val launchIntent = context.packageManager
                    .getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
                    context.startActivity(launchIntent)
                }
            }

            holder.itemView.setOnLongClickListener {
                if (panelPrefs.hapticEnabled) {
                    holder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                }
                SpringAnimator.scalePulse(holder.itemView)
                onRemove(app)
                true
            }
        } else if (holder is AddViewHolder) {
            holder.itemView.setOnClickListener {
                if (panelPrefs.hapticEnabled) {
                    holder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                }
                SpringAnimator.scalePulse(holder.itemView)
                onAddClick(true) // Open in Edit Mode
            }
        }
    }

    private class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo) =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo) =
            oldItem.appName == newItem.appName && oldItem.isInPanel == newItem.isInPanel
    }
}
