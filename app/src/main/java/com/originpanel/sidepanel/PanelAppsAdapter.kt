package com.originpanel.sidepanel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for apps displayed inside the side panel.
 *
 * Interactions:
 * - Tap  → launch the app
 * - Long-press → remove from panel
 */
class PanelAppsAdapter(
    private val context: Context,
    private val onRemove: (AppInfo) -> Unit
) : ListAdapter<AppInfo, PanelAppsAdapter.AppViewHolder>(AppDiffCallback()) {

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvAppName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_panel_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = getItem(position)

        // App icon — loaded directly from PackageManager (already resolved in AppInfo)
        holder.ivIcon.setImageDrawable(app.icon)
        holder.tvName.text = app.appName

        // Tap → launch app
        holder.itemView.setOnClickListener {
            SpringAnimator.scalePulse(holder.itemView)
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "Can't open ${app.appName}", Toast.LENGTH_SHORT).show()
            }
        }

        // Long-press → remove from panel
        holder.itemView.setOnLongClickListener {
            SpringAnimator.scalePulse(holder.itemView)
            onRemove(app)
            true
        }
    }

    private class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo) =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo) =
            oldItem.appName == newItem.appName && oldItem.isInPanel == newItem.isInPanel
    }
}
