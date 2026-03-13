package com.originpanel.sidepanel

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppPickerPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onClose: (() -> Unit)? = null
    var onAppLaunched: (() -> Unit)? = null
    var onToggleApp: ((AppInfo, Boolean) -> Unit)? = null

    private val pickerPanelCard: View
    private val rvPickerGrid: RecyclerView
    private val etSearch: EditText
    private val btnSettings: ImageButton
    private val btnEdit: TextView
    private val tvHeader: TextView
    private val adapter = PickerAdapter()
    
    private val repository = AppRepository(context)
    private val panelPrefs = PanelPreferences(context)
    private var allApps = listOf<AppInfo>()
    var isEditMode = false
        private set
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.picker_panel_layout, this, true)
        pickerPanelCard = view.findViewById(R.id.pickerPanelCard)
        rvPickerGrid = view.findViewById(R.id.rvPickerGrid)
        etSearch = view.findViewById(R.id.etPickerSearch)
        btnSettings = view.findViewById(R.id.btnPickerClose) 
        btnEdit = view.findViewById(R.id.btnPickerEdit)
        tvHeader = view.findViewById(R.id.tvPickerHeader)

        if (panelPrefs.uiTheme == PanelPreferences.THEME_RICH) {
            rvPickerGrid.layoutManager = LinearLayoutManager(context)
        } else {
            rvPickerGrid.layoutManager = GridLayoutManager(context, 2)
        }
        
        rvPickerGrid.adapter = adapter

        btnSettings.setOnClickListener {
            val intent = android.content.Intent(context, SettingsActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onClose?.invoke() 
        }

        btnEdit.setOnClickListener {
            setEditMode(!isEditMode)
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadApps()
    }

    fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        tvHeader.text = if (isEditMode) "Manage Side Panel" else "All Apps"
        btnEdit.text = if (isEditMode) "DONE" else "EDIT"
        
        // Visual feedback for the edit button
        val accentColor = try {
            if (panelPrefs.useCustomAccent) Color.parseColor(panelPrefs.accentColor)
            else Color.parseColor("#4A9EFF")
        } catch (e: Exception) { Color.parseColor("#4A9EFF") }

        btnEdit.setTextColor(if (isEditMode) accentColor else Color.parseColor("#4A9EFF"))
        
        // Refresh everything to show/hide indicators
        adapter.notifyDataSetChanged()
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_UP && event.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            onClose?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    fun loadApps() {
        scope.launch {
            allApps = withContext(Dispatchers.IO) { repository.getAllApps() }
            adapter.submitList(allApps)
        }
    }

    private fun filter(query: String) {
        val filtered = allApps.filter { it.appName.contains(query, ignoreCase = true) }
        adapter.submitList(filtered)
    }

    inner class PickerAdapter : androidx.recyclerview.widget.ListAdapter<AppInfo, PickerViewHolder>(AppDiffCallback()) {

        private var accentColor: Int = Color.parseColor("#4DFFFFFF")
        private var accentColorStateList: android.content.res.ColorStateList = android.content.res.ColorStateList.valueOf(accentColor)

        init {
            updateAccentColor()
        }

        fun updateAccentColor() {
            accentColor = try {
                if (panelPrefs.useCustomAccent) {
                    Color.parseColor(panelPrefs.accentColor)
                } else {
                    Color.parseColor("#4DFFFFFF")
                }
            } catch (e: Exception) {
                Color.parseColor("#4DFFFFFF")
            }
            accentColorStateList = android.content.res.ColorStateList.valueOf(accentColor)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickerViewHolder {
            val layoutId = if (panelPrefs.uiTheme == PanelPreferences.THEME_RICH) 
                R.layout.item_picker_app_rich else R.layout.item_picker_app_modern
            
            val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
            return PickerViewHolder(view)
        }

        override fun onBindViewHolder(holder: PickerViewHolder, position: Int) {
            val app = getItem(position)
            holder.ivIcon.setImageDrawable(app.icon)
            holder.tvName.text = app.appName
            
            // Apply icon shape
            IconShapeHelper.applyShape(holder.ivIcon, panelPrefs.iconShape)
            holder.tvPackage?.text = app.packageName

            if (isEditMode) {
                holder.ivCheck.visibility = View.VISIBLE
                val isSelected = app.isInPanel
                
                // For PNG icon, use imageTintList to color the icon itself
                val iconTint = if (isSelected) accentColorStateList else android.content.res.ColorStateList.valueOf(Color.parseColor("#B3FFFFFF"))
                if (holder.ivCheck is ImageView) {
                    holder.ivCheck.imageTintList = iconTint
                }
                
                // Optional: keep the blue circle background only when selected
                holder.ivCheck.backgroundTintList = if (isSelected) accentColorStateList else android.content.res.ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
                
                // Rotate to 'x' if selected, 'plus' if not
                holder.ivCheck.rotation = if (isSelected) 45f else 0f
            } else {
                holder.ivCheck.visibility = View.GONE
            }

            holder.vHighlight.visibility = if (app.isInPanel && isEditMode) View.VISIBLE else View.GONE
            holder.vHighlight.backgroundTintList = accentColorStateList

            holder.itemView.setOnClickListener {
                if (panelPrefs.hapticEnabled) {
                    holder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                }
                
                if (isEditMode) {
                    toggleAppSelection(app, position, holder.ivCheck)
                } else {
                    launchApp(app)
                }
            }

            // Also allow clicking the plus icon directly
            holder.ivCheck.setOnClickListener {
                if (isEditMode) {
                    if (panelPrefs.hapticEnabled) {
                        it.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    }
                    toggleAppSelection(app, position, holder.ivCheck)
                }
            }
        }

        private fun toggleAppSelection(app: AppInfo, position: Int, plusView: View) {
            val newState = !app.isInPanel
            app.isInPanel = newState
            onToggleApp?.invoke(app, newState)

            // Animation for the plus icon
            plusView.animate()
                .rotation(if (newState) 45f else 0f)
                .setDuration(200)
                .start()
            
            val tint = if (newState) accentColorStateList else android.content.res.ColorStateList.valueOf(Color.parseColor("#4DFFFFFF"))
            plusView.backgroundTintList = tint

            notifyItemChanged(position, "TOGGLE_STATE")
        }

        private fun launchApp(app: AppInfo) {
            rvPickerGrid.findViewHolderForAdapterPosition(currentList.indexOf(app))?.itemView?.let {
                SpringAnimator.scalePulse(it)
            }
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            onAppLaunched?.invoke()
        }

        override fun onBindViewHolder(holder: PickerViewHolder, position: Int, payloads: List<Any>) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                val app = getItem(position)
                if (isEditMode) {
                    holder.ivCheck.visibility = View.VISIBLE
                    val isSelected = app.isInPanel
                    
                    val iconTint = if (isSelected) accentColorStateList else android.content.res.ColorStateList.valueOf(Color.parseColor("#B3FFFFFF"))
                    if (holder.ivCheck is ImageView) {
                        holder.ivCheck.imageTintList = iconTint
                    }
                    holder.ivCheck.backgroundTintList = if (isSelected) accentColorStateList else android.content.res.ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
                    holder.ivCheck.rotation = if (isSelected) 45f else 0f
                } else {
                    holder.ivCheck.visibility = View.GONE
                }
                holder.vHighlight.visibility = if (app.isInPanel && isEditMode) View.VISIBLE else View.GONE
            }
        }
    }

    private class AppDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            // Check if name or selection state changed
            return oldItem.appName == newItem.appName && oldItem.isInPanel == newItem.isInPanel
        }
    }

    inner class PickerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivPickerAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvPickerAppName)
        val ivCheck: View = view.findViewById(R.id.ivPickerCheck)
        val vHighlight: View = view.findViewById(R.id.vPickerBgHighlight)
        val tvPackage: TextView? = view.findViewById(R.id.tvPickerPackageName) 
    }
}
