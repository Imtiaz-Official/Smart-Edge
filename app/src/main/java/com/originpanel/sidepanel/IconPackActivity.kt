package com.originpanel.sidepanel

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class IconPackActivity : AppCompatActivity() {

    private lateinit var panelPrefs: PanelPreferences
    private lateinit var iconPackManager: IconPackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_icon_pack)

        supportActionBar?.apply {
            title = "Select Icon Pack"
            setDisplayHomeAsUpEnabled(true)
        }

        panelPrefs = PanelPreferences(this)
        iconPackManager = IconPackManager(this)

        val rv = findViewById<RecyclerView>(R.id.rvIconPacks)
        rv.layoutManager = LinearLayoutManager(this)

        val packs = mutableListOf<IconPackInfo>()
        // Add "None" option
        packs.add(IconPackInfo("none", "System Default", getDrawable(android.R.drawable.sym_def_app_icon)!!))
        packs.addAll(iconPackManager.getInstalledIconPacks())

        rv.adapter = IconPackAdapter(packs, panelPrefs.selectedIconPack) { selectedPkg ->
            panelPrefs.selectedIconPack = selectedPkg
            AppRepository.clearCache() // IMPORTANT: Clear cache before refresh
            
            // Notify service to refresh icons immediately
            val intent = android.content.Intent(this, FloatingPanelService::class.java).apply {
                action = FloatingPanelService.ACTION_REFRESH
            }
            startForegroundService(intent)
            
            finish()
        }    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class IconPackAdapter(
        private val items: List<IconPackInfo>,
        private val currentPack: String,
        private val onSelect: (String) -> Unit
    ) : RecyclerView.Adapter<IconPackAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon = v.findViewById<ImageView>(R.id.ivPackIcon)
            val name = v.findViewById<TextView>(R.id.tvPackName)
            val pkg = v.findViewById<TextView>(R.id.tvPackPackage)
            val radio = v.findViewById<RadioButton>(R.id.rbSelected)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_icon_pack, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.icon.setImageDrawable(item.icon)
            holder.name.text = item.label
            holder.pkg.text = if (item.packageName == "none") "" else item.packageName
            holder.radio.isChecked = item.packageName == currentPack

            holder.itemView.setOnClickListener {
                onSelect(item.packageName)
            }
        }

        override fun getItemCount() = items.size
    }
}
