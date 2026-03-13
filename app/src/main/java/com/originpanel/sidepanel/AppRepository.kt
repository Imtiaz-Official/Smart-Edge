package com.originpanel.sidepanel

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads all user-installed, launchable apps from the PackageManager.
 * Also cross-references with PanelPreferences to mark which apps are pinned.
 *
 * Uses two-pass icon loading:
 *   1. getAllApps() returns metadata immediately with null icons (fast).
 *   2. loadIconForApp() loads a single icon on demand (call from worker thread).
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager
    private val panelPrefs = PanelPreferences(appContext)
    private val iconPackManager = IconPackManager(appContext)

    /**
     * Returns all launchable apps with metadata only (icon = null).
     * Call [loadIconForApp] afterwards to populate icons lazily.
     */
    suspend fun getAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }

        val prefs = PanelPreferences(appContext)
        val panelPackages = prefs.getPanelApps().toSet()

        packageManager.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                AppInfo(
                    packageName = pkg,
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    icon = null, // Loaded lazily via loadIconForApp()
                    isInPanel = panelPackages.contains(pkg)
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    /**
     * Loads and returns the icon for a single app.
     * Must be called from a background thread (Dispatchers.IO).
     */
    suspend fun loadIconForApp(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        val selectedPack = panelPrefs.selectedIconPack
        val customIcon = iconPackManager.getIcon(packageName, selectedPack)
        if (customIcon != null) return@withContext customIcon
        return@withContext try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns only the apps currently pinned to the panel.
     */
    suspend fun getPanelApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val prefs = PanelPreferences(appContext)
        val panelPackages = prefs.getPanelApps()
        if (panelPackages.isEmpty()) return@withContext emptyList()

        val selectedPack = prefs.selectedIconPack

        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val allLaunchable = packageManager.queryIntentActivities(intent, 0)
            .associateBy { it.activityInfo.packageName }

        panelPackages.mapNotNull { pkg ->
            val resolveInfo = allLaunchable[pkg]
            val customIcon = iconPackManager.getIcon(pkg, selectedPack)

            if (resolveInfo != null) {
                AppInfo(
                    packageName = pkg,
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    icon = customIcon ?: resolveInfo.loadIcon(packageManager),
                    isInPanel = true
                )
            } else {
                try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    AppInfo(
                        packageName = pkg,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        icon = customIcon ?: packageManager.getApplicationIcon(appInfo),
                        isInPanel = true
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
            .take(5)
            .map { it.activityInfo.packageName }
    }
}
