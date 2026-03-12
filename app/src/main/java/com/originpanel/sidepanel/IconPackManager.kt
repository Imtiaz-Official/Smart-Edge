package com.originpanel.sidepanel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log

/**
 * Utility to manage and retrieve icons from external Icon Packs.
 */
class IconPackManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Returns a list of installed Icon Pack package names.
     */
    fun getInstalledIconPacks(): List<IconPackInfo> {
        val iconPacks = mutableListOf<IconPackInfo>()
        
        // Typical intents used by Icon Packs
        val intents = arrayOf(
            "com.novalauncher.THEME",
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme"
        )

        val packages = mutableSetOf<String>()
        for (action in intents) {
            val intent = Intent(action)
            val list = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (info in list) {
                val pkg = info.activityInfo.packageName
                if (packages.add(pkg)) {
                    val label = info.loadLabel(packageManager).toString()
                    val icon = info.loadIcon(packageManager)
                    iconPacks.add(IconPackInfo(pkg, label, icon))
                }
            }
        }
        return iconPacks.sortedBy { it.label.lowercase() }
    }

    /**
     * Tries to load an icon for a given packageName from the specified iconPack.
     * Returns null if not found or no pack selected.
     */
    fun getIcon(packageName: String, iconPackPackage: String): Drawable? {
        if (iconPackPackage == "none") return null
        
        try {
            val iconPackRes = packageManager.getResourcesForApplication(iconPackPackage)
            
            // Standard appfilter.xml parsing would be here for a full implementation.
            // For this version, we will use a common heuristic: looking for resource name matches.
            // Many packs name their icons by the package name (dots replaced by underscores).
            val resName = packageName.replace(".", "_")
            val resId = iconPackRes.getIdentifier(resName, "drawable", iconPackPackage)
            
            if (resId != 0) {
                return iconPackRes.getDrawable(resId, null)
            }
        } catch (e: Exception) {
            Log.e("IconPackManager", "Error loading icon from $iconPackPackage: ${e.message}")
        }
        return null
    }
}

data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)
