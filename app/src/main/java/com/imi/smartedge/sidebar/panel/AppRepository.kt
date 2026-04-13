package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads metadata for all user-installed, launchable apps from the PackageManager.
 * Icons are handled separately by Glide using the package name.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager
    private val panelPrefs = PanelPreferences(appContext)
    private val iconPackManager = IconPackManager(appContext)

    companion object {
        // Fast in-memory cache specifically for SYSTEM icons.
        // System icons don't change when switching packs, so caching them here 
        // bypasses the expensive PackageManager IPC calls entirely.
        private val systemIconCache = java.util.concurrent.ConcurrentHashMap<String, android.graphics.drawable.Drawable>()

        fun clearSystemIconCache(packageName: String) {
            systemIconCache.remove(packageName)
        }
    }

    /**
     * Loads and returns the icon for a single app.
     * Synchronous version for background threads.
     */
    fun loadIconForAppSync(packageName: String): android.graphics.drawable.Drawable? {
        val selectedPack = panelPrefs.selectedIconPack
        
        // Always load the base system icon first
        val systemIcon = systemIconCache[packageName] ?: try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val icon = appInfo.loadIcon(packageManager)
            if (icon != null) {
                systemIconCache[packageName] = icon
            }
            icon
        } catch (e: Exception) {
            try {
                val icon = packageManager.getApplicationIcon(packageName)
                if (icon != null) {
                    systemIconCache[packageName] = icon
                }
                icon
            } catch (e2: Exception) {
                null
            }
        }

        if (systemIcon == null) return null

        // Apply icon pack (with masking support)
        return iconPackManager.getThemedIcon(packageName, systemIcon, selectedPack)
    }

    /**
     * Returns all launchable apps with metadata only.
     */
    suspend fun getAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }

        val panelPackages = panelPrefs.getPanelApps().toSet()

        val list = packageManager.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                AppInfo(
                    packageName = pkg,
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    isInPanel = panelPackages.contains(pkg)
                )
            }
            .toMutableList()

        // Add Pseudo Shortcuts
        val pseudoShortcuts = listOf(
            AppInfo("smartedge.shortcut.one_hand", "One-Handed Mode", panelPackages.contains("smartedge.shortcut.one_hand"))
        )
        list.addAll(pseudoShortcuts)
        
        list.sortedBy { it.appName.lowercase() }
    }

    /**
     * Returns only the apps currently pinned to the panel.
     */
    suspend fun getPanelApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pinnedPackages = panelPrefs.getPanelApps().toMutableList()
        val allPackages = mutableListOf<String>()

        // Prepend notification apps if the feature is enabled
        if (panelPrefs.showNotificationApps) {
            val notifyApps = NotificationTrackingService.getActiveNotificationPackages()
            allPackages.addAll(notifyApps)
        }
        
        // Add pinned packages, avoiding duplicates
        for (pkg in pinnedPackages) {
            if (!allPackages.contains(pkg)) {
                allPackages.add(pkg)
            }
        }
        
        if (allPackages.isEmpty()) return@withContext emptyList()

        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val allLaunchable = packageManager.queryIntentActivities(intent, 0)
            .associateBy { it.activityInfo.packageName }

        allPackages.mapNotNull { pkg ->
            if (pkg == "smartedge.shortcut.one_hand") {
                return@mapNotNull AppInfo(pkg, "One-Handed Mode", true)
            }
            
            val resolveInfo = allLaunchable[pkg]
            if (resolveInfo != null) {
                AppInfo(
                    packageName = pkg,
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    isInPanel = pinnedPackages.contains(pkg) // only mark as inPanel if it's explicitly pinned
                )
            } else {
                try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    AppInfo(
                        packageName = pkg,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        isInPanel = pinnedPackages.contains(pkg)
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    suspend fun getTop5Apps(): List<String> = withContext(Dispatchers.IO) {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        packageManager.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .take(5)
            .map { it.activityInfo.packageName }
    }
}
