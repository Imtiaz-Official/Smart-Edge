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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                Log.e(TAG, "Binder not ready yet")
                false
            }
        } else {
            true
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
            // 1. Get the component name first
            val resolveCmd = "cmd package resolve-activity --brief $packageName | tail -n 1"
            val componentName = executeCommand(resolveCmd).trim()
            
            if (componentName.isEmpty() || !componentName.contains("/")) {
                Log.e(TAG, "Could not resolve component for $packageName")
                return false
            }

            Log.d(TAG, "Resolved component: $componentName")

            // 2. Launch with aggressive flags
            // -S: Force stop before launch
            // --windowingMode 5: Freeform
            val launchCmd = "am start -S --windowingMode 5 -n $componentName --activity-multiple-task"
            Log.d(TAG, "Executing: $launchCmd")
            
            val result = executeCommand(launchCmd)
            Log.d(TAG, "Launch result: $result")
            
            !result.contains("Error")
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku launch error", e)
            false
        }
    }

    private fun executeCommand(command: String): String {
        return try {
            // Use reflection more carefully to find the method
            val methods = Shizuku::class.java.methods
            val newProcessMethod = methods.find { it.name == "newProcess" && it.parameterCount == 3 }
            
            if (newProcessMethod == null) {
                Log.e(TAG, "newProcess method not found")
                return "Error: No Method"
            }
            
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
                Log.e(TAG, "Command Error: $error")
                // On some systems, success messages are in stderr, so we check for actual "Error" keyword
                if (error.contains("Error", ignoreCase = true) || error.contains("Exception", ignoreCase = true)) {
                    return "Error: $error"
                }
            }
            output.ifEmpty { error }
        } catch (e: Exception) {
            Log.e(TAG, "Execute error", e)
            "Error: ${e.message}"
        }
    }
}
