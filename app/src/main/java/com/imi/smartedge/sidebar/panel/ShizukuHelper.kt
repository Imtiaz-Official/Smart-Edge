package com.imi.smartedge.sidebar.panel

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuHelper {
    private const val TAG = "ShizukuHelper"

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        if (!isShizukuAvailable()) return false
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku service not ready")
            return false
        }
    }

    fun requestShizukuPermission(requestCode: Int) {
        if (isShizukuAvailable()) {
            Shizuku.requestPermission(requestCode)
        }
    }

    fun launchAppFloating(packageName: String): Boolean {
        if (!hasShizukuPermission()) {
            Log.e(TAG, "No Shizuku permission")
            return false
        }

        return try {
            val resolveCmd = "cmd package resolve-activity --brief $packageName | tail -n 1"
            val componentName = executeCommand(resolveCmd).trim()
            
            if (componentName.isEmpty() || !componentName.contains("/")) {
                Log.e(TAG, "Could not resolve component for $packageName")
                return false
            }

            val launchCmd = "am start -S --windowingMode 5 -n $componentName --activity-multiple-task"
            val result = executeCommand(launchCmd)
            !result.contains("Error")
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku launch error", e)
            false
        }
    }

    fun reboot(type: String = ""): Boolean {
        if (!hasShizukuPermission()) return false
        try {
            // Multiple methods to reboot
            executeCommand("svc power reboot $type")
            executeCommand(if (type.isEmpty()) "reboot" else "reboot $type")
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun triggerPowerMenu(): Boolean {
        if (!hasShizukuPermission()) return false
        
        return try {
            // Use GLOBAL_ACTION_POWER_DIALOG and GLOBAL_ACTIONS keyevent as robust fallbacks
            executeCommand("cmd accessibility call-system-action 6")
            executeCommand("input keyevent 524")
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Force stops an application.
     */
    fun forceStopApp(packageName: String): Boolean {
        if (!hasShizukuPermission()) return false
        val result = executeCommand("am force-stop $packageName")
        return !result.contains("Error")
    }

    /**
     * Performs system navigation actions.
     * 1: Back, 2: Home, 3: Recents
     */
    fun performNavigation(action: Int): Boolean {
        if (!hasShizukuPermission()) return false
        val keycode = when (action) {
            1 -> 4   // Back
            2 -> 3   // Home
            3 -> 187 // Recents
            else -> return false
        }
        executeCommand("input keyevent $keycode")
        return true
    }

    /**
     * Freezes (disables) or unfreezes (enables) an app.
     */
    fun setAppEnabled(packageName: String, enabled: Boolean): Boolean {
        if (!hasShizukuPermission()) return false
        // 'pm disable-user' is safer than just 'pm disable'
        val cmd = if (enabled) "pm enable $packageName" else "pm disable-user --user 0 $packageName"
        val result = executeCommand(cmd)
        return !result.contains("Error")
    }

    private fun executeCommand(command: String): String {
        return try {
            val methods = Shizuku::class.java.methods
            val newProcessMethod = methods.find { it.name == "newProcess" && it.parameterCount == 3 }
            
            if (newProcessMethod == null) return "Error: No Method"
            
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(
                null, 
                arrayOf("sh", "-c", command), 
                null, 
                null
            ) as java.lang.Process
            
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val error = BufferedReader(InputStreamReader(process.errorStream)).readText()
            process.waitFor()
            
            if (error.isNotEmpty()) {
                if (error.contains("Error", ignoreCase = true) || error.contains("Exception", ignoreCase = true)) {
                    return "Error: $error"
                }
            }
            output.ifEmpty { error }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
