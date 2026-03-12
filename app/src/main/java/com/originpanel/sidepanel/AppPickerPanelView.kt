package com.originpanel.sidepanel

import android.content.Context
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
    var onToggleApp: ((AppInfo, Boolean) -> Unit)? = null

    private val rvPickerGrid: RecyclerView
    private val etSearch: EditText
    private val btnSettings: ImageButton
    private val adapter = PickerAdapter()
    
    private val repository = AppRepository(context)
    private var allApps = listOf<AppInfo>()
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.picker_panel_layout, this, true)
        rvPickerGrid = view.findViewById(R.id.rvPickerGrid)
        etSearch = view.findViewById(R.id.etPickerSearch)
        btnSettings = view.findViewById(R.id.btnPickerClose) // It's still named btnPickerClose in XML

        rvPickerGrid.layoutManager = GridLayoutManager(context, 2)
        rvPickerGrid.adapter = adapter

        btnSettings.setOnClickListener {
            val intent = android.content.Intent(context, SettingsActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onClose?.invoke() // Close picker after opening settings
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

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_UP && event.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            onClose?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun loadApps() {
        scope.launch {
            allApps = withContext(Dispatchers.IO) { repository.getAllApps() }
            adapter.setItems(allApps)
        }
    }

    private fun filter(query: String) {
        val filtered = allApps.filter { it.appName.contains(query, ignoreCase = true) }
        adapter.setItems(filtered)
    }

    inner class PickerAdapter : RecyclerView.Adapter<PickerViewHolder>() {
        private var items = listOf<AppInfo>()

        fun setItems(newItems: List<AppInfo>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_picker_app_modern, parent, false)
            return PickerViewHolder(view)
        }

        override fun onBindViewHolder(holder: PickerViewHolder, position: Int) {
            val app = items[position]
            holder.ivIcon.setImageDrawable(app.icon)
            holder.tvName.text = app.appName
            
            val isSelected = app.isInPanel
            holder.ivCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.vHighlight.visibility = if (isSelected) View.VISIBLE else View.GONE

            holder.itemView.setOnClickListener {
                SpringAnimator.scalePulse(holder.itemView)
                val newState = !app.isInPanel
                app.isInPanel = newState
                onToggleApp?.invoke(app, newState)
                notifyItemChanged(position)
            }
        }

        override fun getItemCount() = items.size
    }

    inner class PickerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivPickerAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvPickerAppName)
        val ivCheck: View = view.findViewById(R.id.ivPickerCheck)
        val vHighlight: View = view.findViewById(R.id.vPickerBgHighlight)
    }
}