package com.originpanel.sidepanel

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.originpanel.sidepanel.databinding.ActivityAppPickerM3Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen activity to add/remove apps from the side panel.
 * Opens from MainActivity → "Manage Apps" button.
 * Changes are saved to PanelPreferences immediately on checkbox toggle.
 */
class AppPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppPickerM3Binding
    private lateinit var panelPrefs: PanelPreferences
    private lateinit var pickerAdapter: AppPickerAdapter
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppPickerM3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        panelPrefs = PanelPreferences(this)

        // Setup toolbar
        supportActionBar?.apply {
            title = getString(R.string.app_picker_title)
            setDisplayHomeAsUpEnabled(true)
        }

        pickerAdapter = AppPickerAdapter { app, isChecked ->
            if (isChecked) panelPrefs.addApp(app.packageName)
            else panelPrefs.removeApp(app.packageName)
        }

        binding.rvAllApps.apply {
            layoutManager = LinearLayoutManager(this@AppPickerActivity)
            adapter = pickerAdapter
        }

        // Search
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterApps(newText.orEmpty())
                return true
            }
        })

        binding.btnDone.setOnClickListener { finish() }

        loadApps()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun loadApps() {
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                AppRepository(this@AppPickerActivity).getAllApps()
            }
            allApps = apps
            pickerAdapter.submitList(apps)
        }
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) allApps
        else allApps.filter { it.appName.contains(query, ignoreCase = true) }
        pickerAdapter.submitList(filtered)
    }

    // ── Inner Adapter ────────────────────────────────────────────────────────

    inner class AppPickerAdapter(
        private val onToggle: (AppInfo, Boolean) -> Unit
    ) : ListAdapter<AppInfo, AppPickerAdapter.PickerViewHolder>(Diff) {

        inner class PickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivIcon: ImageView = itemView.findViewById(R.id.ivPickerIcon)
            val tvName: TextView = itemView.findViewById(R.id.tvPickerName)
            val cbInPanel: CheckBox = itemView.findViewById(R.id.cbInPanel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PickerViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_app_picker_m3, parent, false)
            )

        override fun onBindViewHolder(holder: PickerViewHolder, position: Int) {
            val app = getItem(position)
            holder.ivIcon.setImageDrawable(app.icon)
            holder.tvName.text = app.appName

            // Reset listener before setting checked (avoid spurious callbacks)
            holder.cbInPanel.setOnCheckedChangeListener(null)
            holder.cbInPanel.isChecked = panelPrefs.isInPanel(app.packageName)
            holder.cbInPanel.setOnCheckedChangeListener { _, checked ->
                onToggle(app, checked)
            }

            holder.itemView.setOnClickListener {
                holder.cbInPanel.toggle()
            }
        }
    }
}

private object Diff : DiffUtil.ItemCallback<AppInfo>() {
    override fun areItemsTheSame(o: AppInfo, n: AppInfo) = o.packageName == n.packageName
    override fun areContentsTheSame(o: AppInfo, n: AppInfo) = o.appName == n.appName
}
