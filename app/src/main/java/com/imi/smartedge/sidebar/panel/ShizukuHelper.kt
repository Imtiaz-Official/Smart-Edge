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
        // Try multiple methods to reboot
        executeCommand("svc power reboot $type")
        executeCommand(if (type.isEmpty()) "reboot" else "reboot $type")
        return true
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
