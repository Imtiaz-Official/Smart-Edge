package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.DataOutputStream

object AutomationManager {
    private const val TAG = "AutomationManager"

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su -c id")
            val reader = process.inputStream.bufferedReader()
            val output = reader.readLine()
            process.waitFor() == 0 && output != null && output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    fun requestRootPermission(onResult: (Boolean) -> Unit) {
        // Run a simple command to trigger the SU dialog
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val thread = Thread {
                try {
                    val exitCode = process.waitFor()
                    onResult(exitCode == 0)
                } catch (e: Exception) {
                    onResult(false)
                }
            }
            thread.start()
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun isAutomationPossible(): Boolean {
        return isShizukuAvailable() || isRootAvailable()
    }

    fun checkRootAndRequestPermission(context: Context, onResult: (Boolean) -> Unit) {
        if (isRootAvailable()) {
            onResult(true)
            return
        }

        // Check if 'su' binary exists
        val suExists = try {
            Runtime.getRuntime().exec("which su").waitFor() == 0
        } catch (e: Exception) {
            false
        }

        if (suExists) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("Root Detected")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage("Smart Edge has detected root access. Using root for gestures is more efficient and saves battery compared to the Accessibility Service.\n\nWould you like to grant root permission now?")
                .setPositiveButton("Grant Permission") { _, _ ->
                    requestRootPermission(onResult)
                }
                .setNegativeButton("Not Now") { _, _ ->
                    onResult(false)
                }
                .show()
        } else {
            onResult(false)
        }
    }

    fun execute(command: String): Boolean {
        if (isShizukuAvailable()) {
            return try {
                val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                process.waitFor() == 0
            } catch (e: Exception) {
                Log.e(TAG, "Shizuku execution failed: $command", e)
                false
            }
        } else if (isRootAvailable()) {
            return try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                process.waitFor() == 0
            } catch (e: Exception) {
                Log.e(TAG, "Root execution failed: $command", e)
                false
            }
        }
        return false
    }

    fun performBack() = execute("input keyevent 4")
    fun performHome() = execute("input keyevent 3")
    fun performRecents() = execute("input keyevent 187")
    fun performNotifications() = execute("cmd statusbar expand-notifications")
    fun performQuickSettings() = execute("cmd statusbar expand-settings")
    fun performSplitScreen() = execute("cmd statusbar toggle-split-screen")
    
    fun performPowerMenu() = execute("input keyevent --longpress 26") // Long press power for menu

    fun performLockScreen() {
        // Locking is tricky via shell. input keyevent 26 (power) is the best fallback
        execute("input keyevent 26")
    }

    fun performPreviousApp() {
        // Previous app is usually two recents taps
        performRecents()
        Thread.sleep(200)
        performRecents()
    }

    fun takeScreenshot() {
        // Shell screenshot command varies, but 'screencap' is common.
        // However, triggering the system screenshot UI is better.
        execute("input keyevent 120") // KEYCODE_SYSRQ/Screenshot
    }

    fun launchApp(context: Context, packageName: String, windowingMode: Int): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
            val component = intent.component?.flattenToShortString() ?: return false
            
            // Get real screen metrics to ensure accurate centering
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            val size = android.graphics.Point()
            wm.defaultDisplay.getRealSize(size)
            val w = size.x
            val h = size.y
            
            // Use a 75% window size for better snap compatibility
            val factor = 0.75
            val winW = (w * factor).toInt()
            val winH = (h * factor).toInt()
            val left = (w - winW) / 2
            val top = (h - winH) / 2
            val right = left + winW
            val bottom = top + winH
            
            // am start -n com.pkg/.Activity --windowingMode 106 --bounds 100,100,800,1200
            // Added --activity-clear-task to ensure a fresh window placement
            val cmd = "am start -n $component --windowingMode $windowingMode --bounds $left,$top,$right,$bottom"
            execute(cmd)
        } catch (e: Exception) {
            Log.e(TAG, "Shell launch failed", e)
            false
        }
    }
}
