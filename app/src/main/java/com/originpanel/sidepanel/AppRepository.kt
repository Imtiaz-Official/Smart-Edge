package com.originpanel.sidepanel

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads all user-installed, launchable apps from the PackageManager.
 * Also cross-references with PanelPreferences to mark which apps are pinned.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager
    private val panelPrefs = PanelPreferences(appContext)

    /**
     * Returns all launchable apps installed on the device, sorted alphabetically.
     * Each [AppInfo] has [AppInfo.isInPanel] set according to current panel preferences.
     */
    suspend fun getAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }

        val panelPackages = panelPrefs.getPanelApps().toSet()

        packageManager.queryIntentActivities(intent, 0)
            .map { resolveInfo ->
                AppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    icon = resolveInfo.loadIcon(packageManager),
                    isInPanel = panelPackages.contains(resolveInfo.activityInfo.packageName)
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    /**
     * Returns only the apps currently pinned to the panel,
     * in their saved order (panel order is preserved).
     * Robust: Queries all launchable apps once and filters them to ensure visibility.
     */
    suspend fun getPanelApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val panelPackages = panelPrefs.getPanelApps()
        if (panelPackages.isEmpty()) return@withContext emptyList()

        // Cache all launchable apps once (Reliable on all devices)
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val allLaunchable = packageManager.queryIntentActivities(intent, 0)
            .associateBy { it.activityInfo.packageName }

        panelPackages.mapNotNull { pkg ->
            val resolveInfo = allLaunchable[pkg]
            if (resolveInfo != null) {
                AppInfo(
                    packageName = pkg,
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    icon = resolveInfo.loadIcon(packageManager),
                    isInPanel = true
                )
            } else {
                // Fallback for system apps or non-launcher apps that were manually added
                try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    AppInfo(
                        packageName = pkg,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        icon = packageManager.getApplicationIcon(appInfo),
                        isInPanel = true
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Returns the first 5 launchable apps found on the device.
     * Used for initial population on first install.
     */
    suspend fun getTop5Apps(): List<String> = withContext(Dispatchers.IO) {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        packageManager.queryIntentActivities(intent, 0)
            .take(5)
            .map { it.activityInfo.packageName }
    }
}
