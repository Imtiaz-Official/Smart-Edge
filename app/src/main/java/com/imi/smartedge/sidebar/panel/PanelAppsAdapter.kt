package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class PanelAppsAdapter(
    private val context: Context,
    private val onRemove: (AppInfo) -> Unit,
    private val onAddClick: (Boolean) -> Unit,
    private val onAppLaunched: () -> Unit
) : ListAdapter<AppInfo, RecyclerView.ViewHolder>(AppDiffCallback()) {

    private val panelPrefs = PanelPreferences(context)
    private var showAddButton: Boolean = false

    fun setShowAddButton(show: Boolean) {
        if (showAddButton != show) {
            showAddButton = show
            notifyDataSetChanged()
        }
    }

    fun refreshIcons() {
        notifyDataSetChanged()
    }

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
        return if (showAddButton) currentList.size + 1 else currentList.size
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
        val scale = context.getAutoScalingFactor() * panelPrefs.scaleFactor
        val isRich = panelPrefs.uiTheme == PanelPreferences.THEME_RICH
        
        if (holder is AppViewHolder) {
            // Restore original sizes + scaling
            val baseIconSize = if (isRich) 44 else 40
            val baseTextSize = if (isRich) 9f else 8f

            holder.ivIcon.layoutParams.let { lp ->
                lp.width = (context.dpToPx(baseIconSize) * scale).toInt()
                lp.height = (context.dpToPx(baseIconSize) * scale).toInt()
                holder.ivIcon.layoutParams = lp
            }
            holder.tvName.textSize = baseTextSize * scale

            val app = getItem(position)
            
            Glide.with(context)
                .load(AppIconRequest(app.packageName, panelPrefs.selectedIconPack))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .override((120 * scale).toInt(), (120 * scale).toInt())
                .into(holder.ivIcon)
                
            holder.tvName.text = app.appName
            IconShapeHelper.applyShape(holder.ivIcon, panelPrefs.iconShape)

            if (app.packageName == highlightPackage) {
                SpringAnimator.scalePulse(holder.itemView)
                highlightPackage = null
            }

            holder.itemView.setOnClickListener {
                if (panelPrefs.hapticEnabled) {
                    holder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                }
                SpringAnimator.scalePulse(holder.itemView)
                onAppLaunched()

                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
                    if (panelPrefs.freeformEnabled && context.isFreeformEnabled()) {
                        launchFreeform(launchIntent)
                    } else {
                        context.startActivity(launchIntent)
                    }
                }
            }

            holder.itemView.setOnLongClickListener {
                val currentPos = holder.bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    if (panelPrefs.hapticEnabled) {
                        holder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    }
                    SpringAnimator.scalePulse(holder.itemView)
                    onRemove(getItem(currentPos))
                }
                true
            }
        } else if (holder is AddViewHolder) {
            val baseIconSize = 40
            holder.ivAdd.layoutParams.let { lp ->
                lp.width = (context.dpToPx(baseIconSize) * scale).toInt()
                lp.height = (context.dpToPx(baseIconSize) * scale).toInt()
                holder.ivAdd.layoutParams = lp
            }

            holder.itemView.animate().cancel()
            holder.itemView.alpha = 1f
            holder.itemView.scaleX = 1f
            holder.itemView.scaleY = 1f
            
            holder.itemView.setOnClickListener {
                if (panelPrefs.hapticEnabled) {
                    holder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                }
                SpringAnimator.scalePulse(holder.itemView)
                onAddClick(true)
            }
        }
    }

    @android.annotation.SuppressLint("BlockedPrivateApi")
    private fun launchFreeform(intent: Intent) {
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        try {
            val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, 0)
            val displayMetrics = context.resources.displayMetrics
            val w = displayMetrics.widthPixels
            val h = displayMetrics.heightPixels
            val prefersLandscape = detectLandscapeOrientation(intent.`package`)

            val bounds: Rect = when (panelPrefs.freeformWindowMode) {
                PanelPreferences.FREEFORM_MODE_PORTRAIT -> {
                    val left = w / 3
                    val top = h / 15
                    Rect(left, top, w - left, h - top)
                }
                PanelPreferences.FREEFORM_MODE_MAXIMIZED -> Rect(0, 0, w, h)
                PanelPreferences.FREEFORM_MODE_CUSTOM -> {
                    val winW = (w * panelPrefs.freeformCustomWidth / 100.0).toInt()
                    val winH = (h * panelPrefs.freeformCustomHeight / 100.0).toInt()
                    val left = (w - winW) / 2
                    val top = (h - winH) / 2
                    Rect(left, top, left + winW, top + winH)
                }
                else -> {
                    if (prefersLandscape) {
                        // 16:9 wide aspect for games/landscape apps
                        val targetW = if (w > h) (w * 0.80).toInt() else (w * 0.90).toInt()
                        val targetH = (targetW / 1.77).toInt().coerceAtMost((h * 0.85).toInt())
                        val left = (w - targetW) / 2
                        val top = (h - targetH) / 2
                        Rect(left, top, left + targetW, top + targetH)
                    } else {
                        // 9:16 portrait aspect for normal apps (fixed for landscape host)
                        val targetH = (h * 0.85).toInt()
                        val targetW = (targetH * 9 / 16).toInt().coerceAtMost((w * 0.85).toInt())
                        val left = (w - targetW) / 2
                        val top = (h - targetH) / 2
                        Rect(left, top, left + targetW, top + targetH)
                    }
                }
            }
            options.launchBounds = bounds
            Log.d("PanelAppsAdapter", "Launching Freeform: pkg=${intent.`package`}, bounds=$bounds")

            // Use HiddenApiBypass instead of direct reflection to avoid F-Droid lint errors
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    org.lsposed.hiddenapibypass.HiddenApiBypass.invoke(
                        android.app.ActivityOptions::class.java,
                        options,
                        "setLaunchWindowingMode",
                        5
                    )
                    Log.d("PanelAppsAdapter", "HiddenApiBypass: setLaunchWindowingMode(5) success")
                }
            } catch (e: Exception) {
                Log.e("PanelAppsAdapter", "HiddenApiBypass fail: ${e.message}")
            }
            context.startActivity(intent, options.toBundle())
            Log.d("PanelAppsAdapter", "startActivity called with options")
        } catch (e: Exception) {
            context.startActivity(intent)
        }
    }

    private fun detectLandscapeOrientation(packageName: String?): Boolean {
        if (packageName == null) return false
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            val component = launchIntent?.component ?: return false
            val activityInfo = context.packageManager.getActivityInfo(component, android.content.pm.PackageManager.GET_META_DATA)
            when (activityInfo.screenOrientation) {
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE -> true
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo) = oldItem.packageName == newItem.packageName
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo) = oldItem.appName == newItem.appName && oldItem.isInPanel == newItem.isInPanel
    }
}
