package com.originpanel.sidepanel

import android.content.Context
import android.content.pm.PackageManager

/**
 * Loads all user-installed, launchable apps from the PackageManager.
 * Also cross-references with PanelPreferences to mark which apps are pinned.
 */
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val panelPrefs = PanelPreferences(context)

    /**
     * Returns all launchable apps installed on the device, sorted alphabetically.
     * Each [AppInfo] has [AppInfo.isInPanel] set according to current panel preferences.
     */
    fun getAllApps(): List<AppInfo> {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }

        val panelPackages = panelPrefs.getPanelApps().toSet()

        return packageManager.queryIntentActivities(intent, 0)
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
     */
    fun getPanelApps(): List<AppInfo> {
        val panelPackages = panelPrefs.getPanelApps()
        val allApps = getAllApps().associateBy { it.packageName }

        return panelPackages
            .mapNotNull { pkg -> allApps[pkg] }
            .onEach { it.isInPanel = true }
    }
}
