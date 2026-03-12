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
    private val iconPackManager = IconPackManager(appContext)

    /**
     * Returns all launchable apps installed on the device, sorted alphabetically.
     */
    suspend fun getAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }

        val panelPackages = panelPrefs.getPanelApps().toSet()
        val selectedPack = panelPrefs.selectedIconPack

        packageManager.queryIntentActivities(intent, 0)
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val customIcon = iconPackManager.getIcon(pkg, selectedPack)
                
                AppInfo(
                    packageName = pkg,
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    icon = customIcon ?: resolveInfo.loadIcon(packageManager),
                    isInPanel = panelPackages.contains(pkg)
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    /**
     * Returns only the apps currently pinned to the panel.
     */
    suspend fun getPanelApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val panelPackages = panelPrefs.getPanelApps()
        if (panelPackages.isEmpty()) return@withContext emptyList()

        val selectedPack = panelPrefs.selectedIconPack

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
