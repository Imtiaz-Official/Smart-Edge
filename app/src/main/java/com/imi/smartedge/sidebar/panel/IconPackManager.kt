package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Advanced Icon Pack Manager.
 * Supports: appfilter.xml direct and heuristic mapping.
 */
class IconPackManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    
    // Using a simple cache to avoid repeated XML parsing during a session
    companion object {
        private val iconMapCache = ConcurrentHashMap<String, Map<String, String>>()
        private var currentPackName = ""
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

    private fun loadAppFilter(iconPackPackage: String): Map<String, String> {
        val cached = iconMapCache[iconPackPackage]
        if (cached != null) return cached

        val newMap = mutableMapOf<String, String>()
        if (iconPackPackage == "none") return newMap

        try {
            val iconPackRes = packageManager.getResourcesForApplication(iconPackPackage)
            val inputStream = try {
                iconPackRes.assets.open("appfilter.xml")
            } catch (e: Exception) {
                val resId = iconPackRes.getIdentifier("appfilter", "xml", iconPackPackage)
                if (resId != 0) iconPackRes.openRawResource(resId) else null
            } ?: return newMap

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
                        if (pkg.isNotEmpty()) newMap[pkg] = drawable
                    }
                }
                eventType = parser.next()
            }
            inputStream.close()
            iconMapCache[iconPackPackage] = newMap
        } catch (e: Exception) {
            Log.e("IconPackManager", "Error parsing appfilter: ${e.message}")
        }
        return newMap
    }

    private fun extractPackage(component: String): String {
        // Handle format: ComponentInfo{com.pkg/com.pkg.Activity}
        if (component.startsWith("ComponentInfo{")) {
            val start = component.indexOf("{") + 1
            val end = component.indexOf("/")
            if (start > 0 && end > start) {
                return component.substring(start, end)
            }
        }
        return ""
    }

    fun getThemedIcon(packageName: String, originalIcon: Drawable, iconPackPackage: String): Drawable {
        if (iconPackPackage == "none") return originalIcon
        
        val map = loadAppFilter(iconPackPackage)
        
        // 1. Try direct mapping from appfilter.xml
        val drawableName = map[packageName]
        if (drawableName != null) {
            try {
                val iconPackRes = packageManager.getResourcesForApplication(iconPackPackage)
                val resId = iconPackRes.getIdentifier(drawableName, "drawable", iconPackPackage)
                if (resId != 0) return iconPackRes.getDrawable(resId, null)
            } catch (e: Exception) {}
        }
        
        // 2. Try heuristic mapping (com.pkg.app -> com_pkg_app)
        try {
            val iconPackRes = packageManager.getResourcesForApplication(iconPackPackage)
            val resName = packageName.replace(".", "_").lowercase()
            val resId = iconPackRes.getIdentifier(resName, "drawable", iconPackPackage)
            if (resId != 0) return iconPackRes.getDrawable(resId, null)
        } catch (e: Exception) {}

        // Fallback to system icon (Masking is unreliable on some devices)
        return originalIcon
    }
}

data class IconPackInfo(val packageName: String, val label: String, val icon: Drawable)
