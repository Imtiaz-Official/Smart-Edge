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
    private val onAddClick: () -> Unit
) : ListAdapter<AppInfo, RecyclerView.ViewHolder>(AppDiffCallback()) {

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
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_panel_app, parent, false)
            AppViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_panel_add, parent, false)
            AddViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AppViewHolder) {
            val app = getItem(position)
            holder.ivIcon.setImageDrawable(app.icon)
            holder.tvName.text = app.appName

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
                }
            }

            holder.itemView.setOnLongClickListener {
                SpringAnimator.scalePulse(holder.itemView)
                onRemove(app)
                true
            }
        } else if (holder is AddViewHolder) {
            holder.itemView.setOnClickListener {
                SpringAnimator.scalePulse(holder.itemView)
                onAddClick()
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