package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility to manage and retrieve icons from external Icon Packs.
 * INDUSTRY STANDARD: Parses appfilter.xml for accurate mapping.
 */
class IconPackManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    companion object {
        private val iconMap = ConcurrentHashMap<String, String>()
        @Volatile private var currentPackForMap = ""

        @Synchronized
        private fun loadAppFilter(context: Context, iconPackPackage: String) {
            if (currentPackForMap == iconPackPackage) return
            iconMap.clear()
            currentPackForMap = iconPackPackage
            
            try {
                val packageManager = context.packageManager
                val iconPackRes = packageManager.getResourcesForApplication(iconPackPackage)
                val am = iconPackRes.assets
                val inputStream = am.open("appfilter.xml")
                
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(InputStreamReader(inputStream))

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")
                        
                        if (component != null && drawable != null) {
                            val pkg = extractPackage(component)
                            if (pkg.isNotEmpty()) {
                                iconMap[pkg] = drawable
                            }
                        }
                    }
                    eventType = parser.next()
                }
                inputStream.close()
                Log.d("IconPackManager", "Loaded ${iconMap.size} mappings from $iconPackPackage")
            } catch (e: Exception) {
                Log.e("IconPackManager", "Error parsing appfilter for $iconPackPackage: ${e.message}")
            }
        }

        private fun extractPackage(component: String): String {
            val start = component.indexOf("{") + 1
            val end = component.indexOf("/")
            if (start > 0 && end > start) {
                return component.substring(start, end)
            }
            return ""
        }
    }

    fun getInstalledIconPacks(): List<IconPackInfo> {
        val iconPacks = mutableListOf<IconPackInfo>()
        val intents = arrayOf("com.novalauncher.THEME", "org.adw.launcher.THEMES", "com.gau.go.launcherex.theme")
        val packages = mutableSetOf<String>()
        for (action in intents) {
            val list = packageManager.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA)
            for (info in list) {
                val pkg = info.activityInfo.packageName
                if (packages.add(pkg)) {
                    iconPacks.add(IconPackInfo(pkg, info.loadLabel(packageManager).toString(), info.loadIcon(packageManager)))
                }
            }
        }
        return iconPacks.sortedBy { it.label.lowercase() }
    }

    fun getIcon(packageName: String, iconPackPackage: String): Drawable? {
        if (iconPackPackage == "none") return null
        
        loadAppFilter(context, iconPackPackage)
        
        val drawableName = iconMap[packageName]
        if (drawableName != null) {
            try {
                val iconPackRes = packageManager.getResourcesForApplication(iconPackPackage)
                val resId = iconPackRes.getIdentifier(drawableName, "drawable", iconPackPackage)
                if (resId != 0) {
                    return iconPackRes.getDrawable(resId, null)
                }
            } catch (e: Exception) {
                Log.e("IconPackManager", "Error fetching drawable $drawableName: ${e.message}")
            }
        }
        
        // Fallback to heuristic
        try {
            val iconPackRes = packageManager.getResourcesForApplication(iconPackPackage)
            val resName = packageName.replace(".", "_")
            val resId = iconPackRes.getIdentifier(resName, "drawable", iconPackPackage)
            if (resId != 0) return iconPackRes.getDrawable(resId, null)
        } catch (e: Exception) {}
        
        return null
    }
}

data class IconPackInfo(val packageName: String, val label: String, val icon: Drawable)
