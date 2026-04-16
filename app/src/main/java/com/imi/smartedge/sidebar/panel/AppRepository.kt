package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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

        val panelIdentifiers = panelPrefs.getPanelApps().toSet()

        val list = packageManager.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                AppInfo(
                    packageName = pkg,
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    isInPanel = panelIdentifiers.contains(pkg),
                    type = AppInfo.Type.APP
                )
            }
            .toMutableList()

        // Add Pseudo Shortcuts
        val oneHandPkg = "smartedge.shortcut.one_hand"
        list.add(AppInfo(oneHandPkg, "One-Handed Mode", panelIdentifiers.contains(oneHandPkg), AppInfo.Type.SHORTCUT))
        
        list.sortedBy { it.appName.lowercase() }
    }

    /**
     * Returns ALL exported activities for each installed app.
     */
    suspend fun getAllActivities(): List<AppInfo> = withContext(Dispatchers.IO) {
        val panelIdentifiers = panelPrefs.getPanelApps().toSet()
        val allActivities = mutableListOf<AppInfo>()

        try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()))
            } else {
                packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)
            }

            android.util.Log.d("AppRepository", "Found ${packages.size} packages")

            for (pkg in packages) {
                val activities = pkg.activities ?: continue
                for (act in activities) {
                    try {
                        if (!act.exported) continue
                        
                        // Construct a URI for this specific activity
                        val intent = android.content.Intent().apply {
                            setClassName(pkg.packageName, act.name)
                        }
                        val uri = intent.toUri(android.content.Intent.URI_INTENT_SCHEME)
                        
                        allActivities.add(AppInfo(
                            packageName = pkg.packageName,
                            appName = act.loadLabel(packageManager).toString().takeIf { it.isNotBlank() } ?: act.name.substringAfterLast("."),
                            isInPanel = panelIdentifiers.contains(uri),
                            type = AppInfo.Type.ACTIVITY,
                            intentUri = uri,
                            activityName = act.name
                        ))
                    } catch (e: Exception) {
                        // Skip individual activity failures
                        continue
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Error loading activities", e)
        }

        android.util.Log.d("AppRepository", "Returning ${allActivities.size} activities")
        return@withContext allActivities.sortedBy { it.appName.lowercase() }
    }

    /**
     * Returns only the items currently pinned to the panel.
     */
    suspend fun getPanelApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pinnedIdentifiers = panelPrefs.getPanelApps()
        val allIdentifiers = mutableListOf<String>()

        // Prepend notification apps if the feature is enabled
        if (panelPrefs.showNotificationApps) {
            val notifyApps = NotificationTrackingService.getActiveNotificationPackages()
            allIdentifiers.addAll(notifyApps)
        }
        
        // Add pinned identifiers, avoiding duplicates
        for (id in pinnedIdentifiers) {
            if (!allIdentifiers.contains(id)) {
                allIdentifiers.add(id)
            }
        }
        
        if (allIdentifiers.isEmpty()) return@withContext emptyList()

        allIdentifiers.mapNotNull { id ->
            if (id == "smartedge.shortcut.one_hand") {
                return@mapNotNull AppInfo(id, "One-Handed Mode", true, AppInfo.Type.SHORTCUT)
            }

            // If it's a URI, it's an Activity or Shortcut
            if (id.startsWith("intent:")) {
                try {
                    val intent = android.content.Intent.parseUri(id, android.content.Intent.URI_INTENT_SCHEME)
                    val pkg = intent.getPackage() ?: intent.component?.packageName ?: ""
                    
                    val resolveInfo = packageManager.resolveActivity(intent, 0)
                    val name = resolveInfo?.loadLabel(packageManager)?.toString() 
                               ?: intent.component?.shortClassName?.substringAfterLast(".")
                               ?: "Unknown Activity"

                    return@mapNotNull AppInfo(
                        packageName = pkg,
                        appName = name,
                        isInPanel = pinnedIdentifiers.contains(id),
                        type = AppInfo.Type.ACTIVITY,
                        intentUri = id,
                        activityName = intent.component?.className
                    )
                } catch (e: Exception) {
                    return@mapNotNull null
                }
            }

            // Otherwise, it's a standard APP (Package Name)
            try {
                val appInfo = packageManager.getApplicationInfo(id, 0)
                AppInfo(
                    packageName = id,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    isInPanel = pinnedIdentifiers.contains(id),
                    type = AppInfo.Type.APP
                )
            } catch (e: Exception) {
                // Not a valid package, but maybe it's still in the launcher cache?
                null
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
