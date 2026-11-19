package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
        // Aggressive memory cache for fully processed static bitmap icons to ensure buttery smooth scrolling
        val iconCache = android.util.LruCache<String, android.graphics.drawable.Drawable>(300)

        fun clearSystemIconCache(packageName: String? = null) {
            iconCache.evictAll()
        }
    }

    /**
     * Loads, processes, and caches the icon for a single app.
     * Uses the "Nuclear Option" to convert Adaptive Icons to static BitmapDrawables.
     */
    fun getProcessedIcon(packageName: String, appearanceKey: String): android.graphics.drawable.Drawable? {
        val cacheKey = "pkg:$packageName|state:$appearanceKey"
        iconCache.get(cacheKey)?.let { return it }

        val rawIcon = loadIconForAppSync(packageName) ?: return null

        val processedIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && rawIcon is android.graphics.drawable.AdaptiveIconDrawable) {
            try {
                val density = appContext.resources.displayMetrics.density
                // Double the size for 2x super-sampling (ensures perfectly smooth edges)
                val size = (144 * density).toInt().coerceAtLeast(256)
                val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)

                // To draw 108dp unclipped layers into a 72dp bitmap:
                // The layers must be 1.5x larger than the bitmap.
                val layerSize = (size * 1.5f).toInt()
                val offset = (size - layerSize) / 2
                val layerBounds = android.graphics.Rect(offset, offset, offset + layerSize, offset + layerSize)

                rawIcon.background?.mutate()?.let {
                    it.bounds = layerBounds
                    it.draw(canvas)
                }
                rawIcon.foreground?.mutate()?.let {
                    it.bounds = layerBounds
                    it.draw(canvas)
                }

                android.graphics.drawable.BitmapDrawable(appContext.resources, bitmap)
            } catch (e: Exception) {
                rawIcon.mutate()
            }
        } else {
            rawIcon.mutate()
        }

        iconCache.put(cacheKey, processedIcon)
        return processedIcon
    }

    /**
     * Loads and returns the icon for a single app.
     * Synchronous version for background threads.
     */
    fun loadIconForAppSync(packageName: String): android.graphics.drawable.Drawable? {
        val selectedPack = panelPrefs.selectedIconPack
        
        // try getting the properly colored launcher icons using LauncherApps
        val systemIcon = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? android.content.pm.LauncherApps
                val activityList = launcherApps?.getActivityList(packageName, android.os.Process.myUserHandle())
                if (!activityList.isNullOrEmpty()) {
                    activityList[0].getIcon(0)
                } else {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    appInfo.loadIcon(packageManager)
                }
            } else {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                appInfo.loadIcon(packageManager)
            }
        } catch (e: Exception) {
            try {
                packageManager.getApplicationIcon(packageName)
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
                        type = AppInfo.Type.APP,
                        appearanceKey = panelPrefs.appearanceKey
                    )
            }
            .toMutableList()

        // Add Pseudo Shortcuts
        val oneHandPkg = "smartedge.shortcut.one_hand"
        list.add(AppInfo(oneHandPkg, "One-Handed Mode", panelIdentifiers.contains(oneHandPkg), AppInfo.Type.SHORTCUT))
        
        val sortedList = list.sortedBy { it.appName.lowercase() }

        // Aggressively pre-load icons into the memory cache in the background
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            sortedList.take(50).forEach { appInfo ->
                getProcessedIcon(appInfo.packageName, appInfo.appearanceKey ?: "")
            }
        }

        sortedList
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
                            activityName = act.name,
                            appearanceKey = panelPrefs.appearanceKey
                        ))
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        val sortedList = allActivities.sortedBy { it.appName.lowercase() }
        
        // Aggressively pre-load icons into the memory cache in the background
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            sortedList.take(50).forEach { appInfo ->
                getProcessedIcon(appInfo.packageName, appInfo.appearanceKey ?: "")
            }
        }

        return@withContext sortedList
    }

    /**
     * Resolves a list of identifiers into AppInfo objects.
     */
    suspend fun getAppsForIdentifiers(identifiers: List<String>): List<AppInfo> = withContext(Dispatchers.IO) {
        if (identifiers.isEmpty()) return@withContext emptyList()

        identifiers.mapNotNull { id ->
            when {
                id == "smartedge.shortcut.one_hand" -> {
                    AppInfo(id, "One-Handed Mode", true, AppInfo.Type.SHORTCUT, appearanceKey = panelPrefs.appearanceKey)
                }
                id == "smartedge.shortcut.reboot" -> {
                    AppInfo(id, "Power Menu", true, AppInfo.Type.SHORTCUT, appearanceKey = panelPrefs.appearanceKey)
                }
                id.startsWith("smartedge.folder.") -> {
                    val name = id.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                    AppInfo(id, name, true, AppInfo.Type.FOLDER, appearanceKey = panelPrefs.appearanceKey)
                }
                id.startsWith("smartedge.tool.") -> {
                    val name = id.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                    AppInfo(id, name, true, AppInfo.Type.TOOL, appearanceKey = panelPrefs.appearanceKey)
                }
                id.startsWith("intent:") -> {
                    try {
                        val intent = android.content.Intent.parseUri(id, android.content.Intent.URI_INTENT_SCHEME)
                        val pkg = intent.getPackage() ?: intent.component?.packageName ?: ""
                        
                        val resolveInfo = packageManager.resolveActivity(intent, 0)
                        val name = resolveInfo?.loadLabel(packageManager)?.toString() 
                                   ?: intent.component?.shortClassName?.substringAfterLast(".")
                                   ?: "Unknown Activity"

                        AppInfo(
                            packageName = pkg,
                            appName = name,
                            isInPanel = true,
                            type = AppInfo.Type.ACTIVITY,
                            intentUri = id,
                            activityName = intent.component?.className,
                            appearanceKey = panelPrefs.appearanceKey
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                else -> {
                    try {
                        val appInfo = packageManager.getApplicationInfo(id, 0)
                        AppInfo(
                            packageName = id,
                            appName = packageManager.getApplicationLabel(appInfo).toString(),
                            isInPanel = true,
                            type = AppInfo.Type.APP,
                            appearanceKey = panelPrefs.appearanceKey
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
    }

    /**
     * Returns only the items currently pinned to the panel.
     */
    suspend fun getPanelApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pinnedIdentifiers = panelPrefs.getPanelApps()
        val allIdentifiers = pinnedIdentifiers.toMutableList()

        if (panelPrefs.showNotificationApps) {
            val notifyApps = NotificationTrackingService.getActiveNotificationPackages()
            for (pkg in notifyApps.reversed()) {
                if (!allIdentifiers.contains(pkg)) {
                    allIdentifiers.add(0, pkg)
                }
            }
        }
        
        getAppsForIdentifiers(allIdentifiers)
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

    suspend fun getRecommendedApps(limit: Int = 6): List<AppInfo> = withContext(Dispatchers.IO) {
        val recommendedPackages = mutableListOf<String>()
        val selfPkg = appContext.packageName

        // 1. Internally tracked launches / recents first (tracks every open from drawer, sidebar, and accessibility)
        val tracked = panelPrefs.getTopLaunchedPackages(limit)
        for (pkg in tracked) {
            if (!recommendedPackages.contains(pkg) && pkg != selfPkg) {
                recommendedPackages.add(pkg)
            }
        }

        // 2. Supplement with system UsageStatsManager if fewer than limit and permission is granted
        if (recommendedPackages.size < limit) {
            try {
                val usageStatsManager = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
                if (usageStatsManager != null) {
                    val endTime = System.currentTimeMillis()
                    val startTime = endTime - (1000L * 60 * 60 * 24 * 7) // Last 7 days
                    val stats = usageStatsManager.queryUsageStats(
                        android.app.usage.UsageStatsManager.INTERVAL_WEEKLY,
                        startTime,
                        endTime
                    )
                    if (!stats.isNullOrEmpty()) {
                        val sortedStats = stats
                            .filter { it.totalTimeInForeground > 0 && it.packageName != selfPkg }
                            .sortedByDescending { it.lastTimeUsed }
                            .map { it.packageName }
                        for (pkg in sortedStats) {
                            if (!recommendedPackages.contains(pkg)) {
                                recommendedPackages.add(pkg)
                            }
                            if (recommendedPackages.size >= limit) break
                        }
                    }
                }
            } catch (e: Exception) {}
        }

        // 3. Fallback to top launcher activities to guarantee we always fill up to 6 apps
        if (recommendedPackages.size < limit) {
            val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val resolved = packageManager.queryIntentActivities(launcherIntent, 0)
                .map { it.activityInfo.packageName }
                .distinct()
                .filter { it != selfPkg }
            for (pkg in resolved) {
                if (!recommendedPackages.contains(pkg)) {
                    recommendedPackages.add(pkg)
                }
                if (recommendedPackages.size >= limit) break
            }
        }

        val allApps = getAllApps()
        val appMap = allApps.associateBy { it.packageName }

        recommendedPackages
            .mapNotNull { appMap[it] }
            .distinctBy { it.packageName }
            .take(limit)
    }
}
